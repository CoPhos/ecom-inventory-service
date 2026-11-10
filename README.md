# Inventory Service

SQL Server inventory participant for the e-commerce microservices lab. The first
application operation is local inventory reservation; the Order saga will invoke
this through a messaging adapter later.

## Reserve flow

`ReserveInventoryHandler.handle(command)` is the application entry point:

1. Validate reservation/order IDs and a nonempty list of distinct products with
   positive quantities. Copy the request and sort products consistently.
2. Hash the order ID and normalized products/quantities with SHA-256.
3. Acquire transaction-owned SQL Server locks for the order and reservation IDs,
   always in that order. These locks protect IDs before reservation rows exist.
4. Return an existing reservation's state when the fingerprint matches. Reusing
   an ID with a changed payload, or creating another reservation for the same
   order, throws `ReservationConflictException`.
5. Lock the requested stock rows individually in sorted order with `UPDLOCK`.
   Check all quantities before updating any stock.
6. Save either a `RESERVED` header, stock updates, and items, or a `REJECTED`
   header with `PRODUCT_NOT_FOUND` / `INSUFFICIENT_STOCK` and no stock changes.
7. Commit the transaction. Exceptions roll back every write.

Rejected outcomes are durable: adding stock later does not change a retry's
outcome. A new business attempt requires a new order. A retry after a reservation
has been released or committed returns its current state without reserving again.

The request locks use SQL Server's
[`sp_getapplock`](https://learn.microsoft.com/en-us/sql/relational-databases/system-stored-procedures/sp-getapplock-transact-sql).
They belong to the database transaction and work across application instances;
SQL Server releases them on commit or rollback. JDBC starts transactions lazily,
so the lock SQL first issues a table SELECT with no matching rows to begin the
implicit transaction. Repositories require an existing Spring transaction.
Lock timeouts/deadlocks are technical failures: roll back and retry with the same
IDs. They must not be persisted as out-of-stock rejections.

## Local database

```bash
docker compose -f docker/docker-compose.yml up -d --build --wait
mvn spring-boot:run
```

The app uses port `8082` and the database defaults to
`localhost:1434/InventoryServiceDb`. Configure `INVENTORY_DB_PORT`,
`INVENTORY_DB_SA_PASSWORD`, `INVENTORY_DB_USER`, or the full `INVENTORY_DB_URL`
through environment variables. See [Docker setup](docker/README.md) for the
schema, connection details, and local credentials.

## Integration tests

```bash
mvn test
```

Tests require Docker and start a separate SQL Server 2025 instance using
[Testcontainers](https://java.testcontainers.org/modules/databases/mssqlserver/).
They use the same `docker/init/init-db.sql` bootstrap as development and discard
their database afterward. They do not use the Compose database or its volume.

Coverage includes successful reservations, insufficient/missing stock,
all-or-nothing multi-product requests, stable retries and conflicts, concurrent
overselling attempts, concurrent duplicate requests, consistent product lock
ordering, and rollback/retry after a simulated persistence failure.

HTTP endpoints, release/commit handlers, and Kafka/Eventuate integration are
subsequent steps. This milestone is invoked through the application handler and
its integration tests.
