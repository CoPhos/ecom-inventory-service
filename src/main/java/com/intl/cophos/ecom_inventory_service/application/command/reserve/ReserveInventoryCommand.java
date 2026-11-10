package com.intl.cophos.ecom_inventory_service.application.command.reserve;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryCommand(
        UUID reservationId,
        UUID orderId,
        List<ReserveInventoryItemCommand> items
) {
    public ReserveInventoryCommand {
        if (items == null) {
            throw new IllegalArgumentException("Reservation items are required");
        }
        items = List.copyOf(items);
    }
}
