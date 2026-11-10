package com.intl.cophos.ecom_inventory_service.application.command.reserve;

import com.intl.cophos.ecom_inventory_service.domain.InventoryReservation;
import com.intl.cophos.ecom_inventory_service.domain.ReservationFailureReason;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;

import java.util.UUID;

public record ReserveInventoryResult(
        UUID reservationId,
        ReservationStatus status,
        ReservationFailureReason failureReason
) {
    public static ReserveInventoryResult from(InventoryReservation reservation) {
        return new ReserveInventoryResult(
                reservation.reservationId(), reservation.status(), reservation.failureReason());
    }
}
