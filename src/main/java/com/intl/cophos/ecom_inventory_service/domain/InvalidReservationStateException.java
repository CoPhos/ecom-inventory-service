package com.intl.cophos.ecom_inventory_service.domain;

import java.util.UUID;

public class InvalidReservationStateException extends RuntimeException {
    public InvalidReservationStateException(UUID reservationId, ReservationStatus status, String operation) {
        super("Cannot " + operation + " inventory reservation " + reservationId + " in state " + status);
    }
}
