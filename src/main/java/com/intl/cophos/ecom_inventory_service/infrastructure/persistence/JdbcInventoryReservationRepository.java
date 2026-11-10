package com.intl.cophos.ecom_inventory_service.infrastructure.persistence;

import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryReservationRepository;
import com.intl.cophos.ecom_inventory_service.domain.InventoryReservation;
import com.intl.cophos.ecom_inventory_service.domain.ReservationFailureReason;
import com.intl.cophos.ecom_inventory_service.domain.ReservationItem;
import com.intl.cophos.ecom_inventory_service.domain.ReservationStatus;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.intl.cophos.ecom_inventory_service.infrastructure.persistence.utils.QueryConstants.*;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class JdbcInventoryReservationRepository implements InventoryReservationRepository {
    private static final RowMapper<InventoryReservation> ROW_MAPPER = (row, rowNumber) -> {
        String reason = row.getString("failure_reason");
        return new InventoryReservation(
                UUID.fromString(row.getString("reservation_id")),
                UUID.fromString(row.getString("order_id")),
                row.getString("request_fingerprint"),
                ReservationStatus.valueOf(row.getString("status")),
                reason == null ? null : ReservationFailureReason.valueOf(reason));
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcInventoryReservationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void lockRequest(UUID reservationId, UUID orderId) {
        // Always acquire order then reservation. Locks also cover IDs not yet in the table,
        // without serializable range locks on unrelated requests in the same index gap.
        lock("inventory:order:" + orderId);
        lockReservation(reservationId);
    }

    @Override
    public void lockReservation(UUID reservationId) {
        lock("inventory:reservation:" + reservationId);
    }

    private void lock(String resource) {
        Integer result = jdbcTemplate.queryForObject(
                LOCK_REQUEST_ID, new MapSqlParameterSource("resource", resource), Integer.class);
        if (result == null || result == -999) {
            throw new IllegalStateException("SQL Server could not execute the inventory request lock: " + result);
        }
        if (result < 0) {
            // Throwing rolls back the surrounding transaction and releases previously held locks.
            throw new CannotAcquireLockException("Unable to lock inventory request; SQL Server code: " + result);
        }
    }

    @Override
    public Optional<InventoryReservation> findById(UUID reservationId) {
        return jdbcTemplate.query(
                FIND_RESERVATION, new MapSqlParameterSource("reservationId", reservationId.toString()), ROW_MAPPER)
                .stream().findFirst();
    }

    @Override
    public Optional<InventoryReservation> findByOrderId(UUID orderId) {
        return jdbcTemplate.query(
                FIND_RESERVATION_BY_ORDER, new MapSqlParameterSource("orderId", orderId.toString()), ROW_MAPPER)
                .stream().findFirst();
    }

    @Override
    public void save(InventoryReservation reservation) {
        var parameters = new MapSqlParameterSource()
                .addValue("reservationId", reservation.reservationId().toString())
                .addValue("orderId", reservation.orderId().toString())
                .addValue("fingerprint", reservation.requestFingerprint())
                .addValue("status", reservation.status().name())
                .addValue("failureReason",
                        reservation.failureReason() == null ? null : reservation.failureReason().name(),
                        Types.VARCHAR);
        if (jdbcTemplate.update(SAVE_RESERVATION, parameters) != 1) {
            throw new IllegalStateException("Expected to save exactly one reservation");
        }
    }

    @Override
    public void saveItems(UUID reservationId, List<ReservationItem> items) {
        SqlParameterSource[] parameters = items.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("reservationId", reservationId.toString())
                        .addValue("productId", item.productId().toString())
                        .addValue("quantity", item.quantity()))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(SAVE_RESERVATION_ITEM, parameters);
    }

    @Override
    public List<ReservationItem> findItems(UUID reservationId) {
        return jdbcTemplate.query(
                FIND_RESERVATION_ITEMS,
                new MapSqlParameterSource("reservationId", reservationId.toString()),
                (row, rowNumber) -> new ReservationItem(
                        UUID.fromString(row.getString("product_id")), row.getInt("quantity")));
    }

    @Override
    public void markReleased(UUID reservationId) {
        var parameters = new MapSqlParameterSource("reservationId", reservationId.toString());
        if (jdbcTemplate.update(MARK_RESERVATION_RELEASED, parameters) != 1) {
            throw new IllegalStateException("Expected to release exactly one reserved reservation");
        }
    }

    @Override
    public void markCommitted(UUID reservationId) {
        var parameters = new MapSqlParameterSource("reservationId", reservationId.toString());
        if (jdbcTemplate.update(MARK_RESERVATION_COMMITTED, parameters) != 1) {
            throw new IllegalStateException("Expected to commit exactly one reserved reservation");
        }
    }
}
