package com.intl.cophos.ecom_inventory_service.application.command.release;

import com.intl.cophos.ecom_inventory_service.domain.InventoryReservation;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;

import java.util.UUID;

public record ReleaseInventoryResult(UUID reservationId, ReservationStatus status) {
    public static ReleaseInventoryResult from(InventoryReservation reservation) {
        return new ReleaseInventoryResult(reservation.reservationId(), reservation.status());
    }
}
