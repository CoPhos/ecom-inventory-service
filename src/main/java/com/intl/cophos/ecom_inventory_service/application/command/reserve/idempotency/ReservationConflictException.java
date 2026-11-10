package com.intl.cophos.ecom_inventory_service.application.command.reserve.idempotency;

public class ReservationConflictException extends RuntimeException {
    public ReservationConflictException() {
        super("The reservation or order ID was already used for a different reservation request");
    }
}
