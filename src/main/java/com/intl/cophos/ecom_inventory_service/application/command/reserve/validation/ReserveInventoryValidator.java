package com.intl.cophos.ecom_inventory_service.application.command.reserve.validation;

import com.intl.cophos.ecom_inventory_service.application.command.reserve.ReserveInventoryCommand;
import com.intl.cophos.ecom_inventory_service.domain.ReservationItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Component
public class ReserveInventoryValidator {
    public List<ReservationItem> validateAndNormalize(ReserveInventoryCommand command) {
        if (command == null || command.reservationId() == null || command.orderId() == null) {
            throw new IllegalArgumentException("Reservation and order IDs are required");
        }
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("At least one reservation item is required");
        }

        var products = new HashSet<UUID>();
        for (var item : command.items()) {
            if (item.productId() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Each item needs a product ID and a positive quantity");
            }
            if (!products.add(item.productId())) {
                throw new IllegalArgumentException("Duplicate product in reservation: " + item.productId());
            }
        }

        // Use the same order for fingerprinting and acquiring individual stock locks.
        return command.items().stream()
                .map(item -> new ReservationItem(item.productId(), item.quantity()))
                .sorted(Comparator.comparing(item -> item.productId().toString()))
                .toList();
    }
}
