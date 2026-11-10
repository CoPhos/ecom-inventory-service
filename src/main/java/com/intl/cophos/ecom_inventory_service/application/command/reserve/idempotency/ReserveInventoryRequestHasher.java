package com.intl.cophos.ecom_inventory_service.application.command.reserve.idempotency;

import com.intl.cophos.ecom_inventory_service.domain.ReservationItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

public final class ReserveInventoryRequestHasher {
    private ReserveInventoryRequestHasher() {
    }

    public static String hash(UUID orderId, List<ReservationItem> items) {
        StringJoiner payload = new StringJoiner("|");
        payload.add("reserve-inventory:v1");
        payload.add(orderId.toString());
        items.stream()
                .sorted(Comparator.comparing(item -> item.productId().toString()))
                .forEach(item -> payload.add(item.productId() + ":" + item.quantity()));

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
