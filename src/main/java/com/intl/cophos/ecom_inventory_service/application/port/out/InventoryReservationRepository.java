package com.intl.cophos.ecom_inventory_service.application.port.out;

import com.intl.cophos.ecom_inventory_service.domain.InventoryReservation;
import com.intl.cophos.ecom_inventory_service.domain.ReservationItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository {
    // Protect both identifiers until the caller's transaction commits or rolls back.
    void lockRequest(UUID reservationId, UUID orderId);

    // Lifecycle operations share reserve's reservation lock, without taking an order lock.
    void lockReservation(UUID reservationId);

    Optional<InventoryReservation> findById(UUID reservationId);

    Optional<InventoryReservation> findByOrderId(UUID orderId);

    void save(InventoryReservation reservation);

    void saveItems(UUID reservationId, List<ReservationItem> items);

    List<ReservationItem> findItems(UUID reservationId);

    void markReleased(UUID reservationId);

    void markCommitted(UUID reservationId);
}
