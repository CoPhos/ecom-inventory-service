package com.intl.cophos.ecom_inventory_service.application.command.commit;

import java.util.UUID;

public record CommitInventoryCommand(UUID reservationId) {
}
