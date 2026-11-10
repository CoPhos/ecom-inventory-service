package com.intl.cophos.ecom_inventory_service.application.command.reserve;

import java.util.UUID;

public record ReserveInventoryItemCommand(UUID productId, int quantity) {
}
