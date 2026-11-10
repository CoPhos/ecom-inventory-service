package com.intl.cophos.ecom_inventory_service.application.port.out;

import com.intl.cophos.ecom_inventory_service.domain.InventoryStock;

import java.util.Optional;
import java.util.UUID;

public interface InventoryStockRepository {
    Optional<InventoryStock> findAndLock(UUID productId);

    void reserve(UUID productId, int quantity);

    void release(UUID productId, int quantity);

    void commit(UUID productId, int quantity);
}
