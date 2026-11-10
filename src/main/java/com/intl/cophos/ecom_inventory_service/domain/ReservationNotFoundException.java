package com.intl.cophos.ecom_inventory_service.domain;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(UUID reservationId) {
        super("Inventory reservation not found: " + reservationId);
    }
}
