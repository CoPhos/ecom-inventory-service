package com.intl.cophos.ecom_inventory_service.domain;

import java.util.Objects;
import java.util.UUID;

public record InventoryReservation(
        UUID reservationId,
        UUID orderId,
        String requestFingerprint,
        ReservationStatus status,
        ReservationFailureReason failureReason
) {
    public InventoryReservation {
        Objects.requireNonNull(reservationId, "reservationId is required");
        Objects.requireNonNull(orderId, "orderId is required");
        Objects.requireNonNull(requestFingerprint, "requestFingerprint is required");
        Objects.requireNonNull(status, "status is required");
        if ((status == ReservationStatus.REJECTED) != (failureReason != null)) {
            throw new IllegalArgumentException("Only rejected reservations must have a failure reason");
        }
    }

    public InventoryReservation release() {
        return switch (status) {
            case RESERVED -> new InventoryReservation(
                    reservationId, orderId, requestFingerprint, ReservationStatus.RELEASED, null);
            case RELEASED, REJECTED -> this;
            case COMMITTED -> throw new InvalidReservationStateException(reservationId, status, "release");
        };
    }

    public InventoryReservation commit() {
        return switch (status) {
            case RESERVED -> new InventoryReservation(
                    reservationId, orderId, requestFingerprint, ReservationStatus.COMMITTED, null);
            case COMMITTED -> this;
            case RELEASED, REJECTED -> throw new InvalidReservationStateException(reservationId, status, "commit");
        };
    }
}
