package com.intl.cophos.ecom_inventory_service.infrastructure.persistence.utils;

public final class QueryConstants {
    private QueryConstants() {
    }

    public static final String LOCK_REQUEST_ID = """
            DECLARE @lockResult INT;
            -- JDBC begins transactions lazily. A table SELECT starts the implicit transaction
            -- before sp_getapplock, which cannot start one itself. No rows are read or locked.
            SELECT @lockResult = 0 FROM inv.reservations WHERE 1 = 0;
            EXEC @lockResult = sys.sp_getapplock
                @Resource = :resource,
                @LockMode = 'Exclusive',
                @LockOwner = 'Transaction',
                @LockTimeout = 10000;
            SELECT @lockResult;
            """;

    public static final String FIND_STOCK_AND_LOCK = """
            SELECT product_id, on_hand_quantity, reserved_quantity
            FROM inv.stock WITH (UPDLOCK, ROWLOCK)
            WHERE product_id = :productId
            """;

    public static final String RESERVE_STOCK = """
            UPDATE inv.stock
            SET reserved_quantity = reserved_quantity + :quantity,
                updated_at = SYSUTCDATETIME()
            WHERE product_id = :productId
              AND :quantity > 0
              AND on_hand_quantity - reserved_quantity >= :quantity
            """;

    public static final String RELEASE_STOCK = """
            UPDATE inv.stock
            SET reserved_quantity = reserved_quantity - :quantity,
                updated_at = SYSUTCDATETIME()
            WHERE product_id = :productId
              AND :quantity > 0
              AND reserved_quantity >= :quantity
            """;

    public static final String COMMIT_STOCK = """
            UPDATE inv.stock
            SET on_hand_quantity = on_hand_quantity - :quantity,
                reserved_quantity = reserved_quantity - :quantity,
                updated_at = SYSUTCDATETIME()
            WHERE product_id = :productId
              AND :quantity > 0
              AND reserved_quantity >= :quantity
              AND on_hand_quantity >= :quantity
            """;

    public static final String FIND_RESERVATION = """
            SELECT reservation_id, order_id, request_fingerprint, status, failure_reason
            FROM inv.reservations
            WHERE reservation_id = :reservationId
            """;

    public static final String FIND_RESERVATION_BY_ORDER = """
            SELECT reservation_id, order_id, request_fingerprint, status, failure_reason
            FROM inv.reservations
            WHERE order_id = :orderId
            """;

    public static final String SAVE_RESERVATION = """
            INSERT INTO inv.reservations (
                reservation_id, order_id, request_fingerprint, status, failure_reason
            )
            VALUES (:reservationId, :orderId, :fingerprint, :status, :failureReason)
            """;

    public static final String SAVE_RESERVATION_ITEM = """
            INSERT INTO inv.reservation_items (reservation_id, product_id, quantity)
            VALUES (:reservationId, :productId, :quantity)
            """;

    public static final String FIND_RESERVATION_ITEMS = """
            SELECT product_id, quantity
            FROM inv.reservation_items
            WHERE reservation_id = :reservationId
            """;

    public static final String MARK_RESERVATION_RELEASED = """
            UPDATE inv.reservations
            SET status = 'RELEASED',
                updated_at = SYSUTCDATETIME()
            WHERE reservation_id = :reservationId
              AND status = 'RESERVED'
            """;

    public static final String MARK_RESERVATION_COMMITTED = """
            UPDATE inv.reservations
            SET status = 'COMMITTED',
                updated_at = SYSUTCDATETIME()
            WHERE reservation_id = :reservationId
              AND status = 'RESERVED'
            """;
}
