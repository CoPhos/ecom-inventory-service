package com.intl.cophos.ecom_inventory_service.application.command.commit;

import com.intl.cophos.ecom_inventory_service.domain.InventoryReservation;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;

import java.util.UUID;

public record CommitInventoryResult(UUID reservationId, ReservationStatus status) {
    public static CommitInventoryResult from(InventoryReservation reservation) {
        return new CommitInventoryResult(reservation.reservationId(), reservation.status());
    }
}
