package com.intl.cophos.ecom_inventory_service.domain;

import java.util.Objects;
import java.util.UUID;

public record InventoryStock(UUID productId, int onHandQuantity, int reservedQuantity) {
    public InventoryStock {
        Objects.requireNonNull(productId, "productId is required");
        if (onHandQuantity < 0 || reservedQuantity < 0 || reservedQuantity > onHandQuantity) {
            throw new IllegalArgumentException("Stock quantities must satisfy 0 <= reserved <= on hand");
        }
    }

    public int availableQuantity() {
        return onHandQuantity - reservedQuantity;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && availableQuantity() >= quantity;
    }
}
