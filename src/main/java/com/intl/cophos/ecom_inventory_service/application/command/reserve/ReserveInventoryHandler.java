package com.intl.cophos.ecom_inventory_service.application.command.reserve;

import com.intl.cophos.ecom_inventory_service.application.command.reserve.idempotency.ReservationConflictException;
import com.intl.cophos.ecom_inventory_service.application.command.reserve.idempotency.ReserveInventoryRequestHasher;
import com.intl.cophos.ecom_inventory_service.application.command.reserve.validation.ReserveInventoryValidator;
import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryReservationRepository;
import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryStockRepository;
import com.intl.cophos.ecom_inventory_service.domain.InventoryReservation;
import com.intl.cophos.ecom_inventory_service.domain.ReservationFailureReason;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveInventoryHandler {
    private final ReserveInventoryValidator validator;
    private final InventoryStockRepository stockRepository;
    private final InventoryReservationRepository reservationRepository;

    public ReserveInventoryHandler(
            ReserveInventoryValidator validator,
            InventoryStockRepository stockRepository,
            InventoryReservationRepository reservationRepository
    ) {
        this.validator = validator;
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(timeout = 30)
    public ReserveInventoryResult handle(ReserveInventoryCommand command) {
        var items = validator.validateAndNormalize(command);
        String fingerprint = ReserveInventoryRequestHasher.hash(command.orderId(), items);

        reservationRepository.lockRequest(command.reservationId(), command.orderId());
        var existing = reservationRepository.findById(command.reservationId());
        if (existing.isPresent()) {
            var reservation = existing.get();
            if (!reservation.orderId().equals(command.orderId())
                    || !reservation.requestFingerprint().equals(fingerprint)) {
                throw new ReservationConflictException();
            }
            // Return the saved state, including terminal states; never reserve again.
            return ReserveInventoryResult.from(reservation);
        }
        if (reservationRepository.findByOrderId(command.orderId()).isPresent()) {
            throw new ReservationConflictException();
        }

        ReservationFailureReason failureReason = null;
        for (var item : items) {
            var stock = stockRepository.findAndLock(item.productId());
            if (stock.isEmpty()) {
                failureReason = ReservationFailureReason.PRODUCT_NOT_FOUND;
                break;
            }
            if (!stock.get().canReserve(item.quantity())) {
                failureReason = ReservationFailureReason.INSUFFICIENT_STOCK;
                break;
            }
        }

        var reservation = new InventoryReservation(
                command.reservationId(), command.orderId(), fingerprint,
                failureReason == null ? ReservationStatus.RESERVED : ReservationStatus.REJECTED,
                failureReason);
        reservationRepository.save(reservation);

        if (failureReason == null) {
            // All stock checks passed while holding update locks. No partial business rejection.
            for (var item : items) {
                stockRepository.reserve(item.productId(), item.quantity());
            }
            reservationRepository.saveItems(command.reservationId(), items);
        }

        return ReserveInventoryResult.from(reservation);
    }
}
