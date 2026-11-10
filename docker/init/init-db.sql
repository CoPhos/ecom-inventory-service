USE master;
GO

IF DB_ID(N'InventoryServiceDb') IS NULL
BEGIN
    CREATE DATABASE InventoryServiceDb;
END;
GO

USE InventoryServiceDb;
GO

SET XACT_ABORT ON;
GO

-- Database creation runs separately; schema and tables are committed together.
BEGIN TRANSACTION;

IF SCHEMA_ID(N'inv') IS NULL
    EXEC(N'CREATE SCHEMA inv AUTHORIZATION dbo;');

IF OBJECT_ID(N'inv.stock', N'U') IS NULL
BEGIN
    CREATE TABLE inv.stock (
        product_id UNIQUEIDENTIFIER NOT NULL,
        on_hand_quantity INT NOT NULL,
        reserved_quantity INT NOT NULL
            CONSTRAINT DF_stock_reserved_quantity DEFAULT (0),
        updated_at DATETIME2(3) NOT NULL
            CONSTRAINT DF_stock_updated_at DEFAULT SYSUTCDATETIME(),

        CONSTRAINT PK_stock PRIMARY KEY CLUSTERED (product_id),
        CONSTRAINT CK_stock_on_hand_nonnegative CHECK (on_hand_quantity >= 0),
        CONSTRAINT CK_stock_reserved_valid CHECK (
            reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity
        )
    );
END;

IF OBJECT_ID(N'inv.reservations', N'U') IS NULL
BEGIN
    CREATE TABLE inv.reservations (
        -- The caller supplies stable IDs so retries refer to the same operation.
        reservation_id UNIQUEIDENTIFIER NOT NULL,
        order_id UNIQUEIDENTIFIER NOT NULL,
        request_fingerprint CHAR(64) NOT NULL,
        status VARCHAR(20) NOT NULL,
        failure_reason VARCHAR(100) NULL,
        created_at DATETIME2(3) NOT NULL
            CONSTRAINT DF_reservations_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL
            CONSTRAINT DF_reservations_updated_at DEFAULT SYSUTCDATETIME(),

        CONSTRAINT PK_reservations PRIMARY KEY CLUSTERED (reservation_id),
        CONSTRAINT UQ_reservations_order_id UNIQUE (order_id),
        CONSTRAINT CK_reservations_status CHECK (
            status IN ('RESERVED', 'REJECTED', 'RELEASED', 'COMMITTED')
        )
    );
END;

IF OBJECT_ID(N'inv.reservation_items', N'U') IS NULL
BEGIN
    CREATE TABLE inv.reservation_items (
        reservation_id UNIQUEIDENTIFIER NOT NULL,
        product_id UNIQUEIDENTIFIER NOT NULL,
        quantity INT NOT NULL,

        CONSTRAINT PK_reservation_items PRIMARY KEY CLUSTERED (reservation_id, product_id),
        CONSTRAINT FK_reservation_items_reservations FOREIGN KEY (reservation_id)
            REFERENCES inv.reservations (reservation_id),
        CONSTRAINT FK_reservation_items_stock FOREIGN KEY (product_id)
            REFERENCES inv.stock (product_id),
        CONSTRAINT CK_reservation_items_quantity_positive CHECK (quantity > 0)
    );

    -- The primary key already supports loading all items for one reservation.
    CREATE INDEX IX_reservation_items_product_id
        ON inv.reservation_items (product_id);
END;

COMMIT TRANSACTION;
GO
