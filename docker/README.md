# Inventory database

Run from the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d --build --wait
docker compose -f docker/docker-compose.yml logs sqlserver
```

The Compose project is `ecom-inventory`, so its containers, network, and volume
are separate from the Order Service's Compose project. The image runs SQL Server
2025 Developer and initializes the database before it becomes healthy. SQL errors
stop startup rather than reporting a successful initialization.

Connect using:

| Setting | Local value |
| --- | --- |
| Server | `localhost,1434` |
| Database | `InventoryServiceDb` |
| User | `sa` |
| Password | `Toor123!` (local lab default) |
| JDBC URL | `jdbc:sqlserver://localhost:1434;databaseName=InventoryServiceDb;encrypt=true;trustServerCertificate=true` |

Set `INVENTORY_DB_PORT` or `INVENTORY_DB_SA_PASSWORD` in your shell to override
the local defaults. Changing the password variable does not change the SA
password in an existing SQL Server data volume.

## Initial schema

All tables belong to schema `inv`. IDs use `UNIQUEIDENTIFIER`; timestamps use
`DATETIME2(3)` in UTC. Java will supply product, order, and reservation IDs.

| Table | Purpose |
| --- | --- |
| `inv.stock` | One stock pool per product; stores on-hand and reserved quantities. |
| `inv.reservations` | One durable reservation outcome per order, including rejected requests. |
| `inv.reservation_items` | Products and positive quantities belonging to a successful reservation. |

Available quantity is calculated as `on_hand_quantity - reserved_quantity`.
Checks enforce `0 <= reserved_quantity <= on_hand_quantity`, so availability
cannot become negative. Updates will still need correct transaction and locking
behavior; constraints alone do not implement the reservation workflow.

`reservation_id` is supplied by the saga and reused across retries. The unique
`order_id` permits one reservation attempt per order in this first version.
`request_fingerprint` will contain a SHA-256 hex digest of the canonical request
(including the order and consistently sorted product IDs and quantities), so the
application can reject retries with a changed payload. The schema supplies the
keys; the application must claim them safely and compare fingerprints.

Reservation states are `RESERVED`, `REJECTED`, `RELEASED`, and `COMMITTED`.
The application will enforce valid transitions and update `updated_at` explicitly;
the timestamp defaults apply only to inserts. Rejected requests keep their header
and fingerprint but no item rows, allowing rejection when a product has no stock
record. Released and committed reservations retain their item rows for retries.
There is no foreign key to Order Service's database.

The initial script creates missing objects and can run again without erasing
data. It runs on every container start; rebuild the image after editing the SQL.
Existing tables are not altered by this bootstrap script. Subsequent schema
changes should use versioned migrations. Messaging/outbox tables will be added
when we integrate messaging.

Inspect availability with:

```sql
USE InventoryServiceDb;

SELECT product_id, on_hand_quantity, reserved_quantity,
       on_hand_quantity - reserved_quantity AS available_quantity
FROM inv.stock;
```

Stop the container while keeping its data:

```bash
docker compose -f docker/docker-compose.yml down
```
