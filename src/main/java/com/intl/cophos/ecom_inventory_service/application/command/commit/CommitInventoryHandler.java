package com.intl.cophos.ecom_inventory_service.application.command.commit;

import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryReservationRepository;
import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryStockRepository;
import com.intl.cophos.ecom_inventory_service.domain.ReservationNotFoundException;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class CommitInventoryHandler {
    private final InventoryStockRepository stockRepository;
    private final InventoryReservationRepository reservationRepository;

    public CommitInventoryHandler(
            InventoryStockRepository stockRepository,
            InventoryReservationRepository reservationRepository
    ) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(timeout = 30)
    public CommitInventoryResult handle(CommitInventoryCommand command) {
        if (command == null || command.reservationId() == null) {
            throw new IllegalArgumentException("Reservation ID is required");
        }

        // Serialize reserve, release, and commit for the same reservation.
        reservationRepository.lockReservation(command.reservationId());
        var reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));
        var committed = reservation.commit();

        if (reservation.status() == ReservationStatus.COMMITTED) {
            return CommitInventoryResult.from(committed);
        }

        // Match reserve and release's product lock order.
        var items = reservationRepository.findItems(command.reservationId()).stream()
                .sorted(Comparator.comparing(item -> item.productId().toString()))
                .toList();
        if (items.isEmpty()) {
            throw new IllegalStateException("A reserved reservation must contain inventory items");
        }

        for (var item : items) {
            stockRepository.commit(item.productId(), item.quantity());
        }
        reservationRepository.markCommitted(command.reservationId());

        return CommitInventoryResult.from(committed);
    }
}
