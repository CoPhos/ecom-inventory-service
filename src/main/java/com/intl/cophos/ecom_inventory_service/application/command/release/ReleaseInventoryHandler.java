package com.intl.cophos.ecom_inventory_service.application.command.release;

import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryReservationRepository;
import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryStockRepository;
import com.intl.cophos.ecom_inventory_service.domain.ReservationNotFoundException;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class ReleaseInventoryHandler {
    private final InventoryStockRepository stockRepository;
    private final InventoryReservationRepository reservationRepository;

    public ReleaseInventoryHandler(
            InventoryStockRepository stockRepository,
            InventoryReservationRepository reservationRepository
    ) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(timeout = 30)
    public ReleaseInventoryResult handle(ReleaseInventoryCommand command) {
        if (command == null || command.reservationId() == null) {
            throw new IllegalArgumentException("Reservation ID is required");
        }

        // Use the same reservation lock as reserve, without acquiring an order lock afterward.
        reservationRepository.lockReservation(command.reservationId());
        var reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));
        var released = reservation.release();

        if (reservation.status() != ReservationStatus.RESERVED) {
            return ReleaseInventoryResult.from(released);
        }

        // Match reserve's UUID string order; SQL Server's native UUID ordering differs.
        var items = reservationRepository.findItems(command.reservationId()).stream()
                .sorted(Comparator.comparing(item -> item.productId().toString()))
                .toList();
        if (items.isEmpty()) {
            throw new IllegalStateException("A reserved reservation must contain inventory items");
        }

        for (var item : items) {
            stockRepository.release(item.productId(), item.quantity());
        }
        reservationRepository.markReleased(command.reservationId());

        return ReleaseInventoryResult.from(released);
    }
}
