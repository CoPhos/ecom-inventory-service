package com.intl.cophos.ecom_inventory_service.domain;

import java.util.Objects;
import java.util.UUID;

public record ReservationItem(UUID productId, int quantity) {
    public ReservationItem {
        Objects.requireNonNull(productId, "productId is required");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
    }
}
