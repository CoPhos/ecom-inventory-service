package com.intl.cophos.ecom_inventory_service.infrastructure.persistence;

import com.intl.cophos.ecom_inventory_service.application.port.out.InventoryStockRepository;
import com.intl.cophos.ecom_inventory_service.domain.InventoryStock;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.intl.cophos.ecom_inventory_service.infrastructure.persistence.utils.QueryConstants.COMMIT_STOCK;
import static com.intl.cophos.ecom_inventory_service.infrastructure.persistence.utils.QueryConstants.FIND_STOCK_AND_LOCK;
import static com.intl.cophos.ecom_inventory_service.infrastructure.persistence.utils.QueryConstants.RELEASE_STOCK;
import static com.intl.cophos.ecom_inventory_service.infrastructure.persistence.utils.QueryConstants.RESERVE_STOCK;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class JdbcInventoryStockRepository implements InventoryStockRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcInventoryStockRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<InventoryStock> findAndLock(UUID productId) {
        var parameters = new MapSqlParameterSource("productId", productId.toString());
        return jdbcTemplate.query(FIND_STOCK_AND_LOCK, parameters, (row, rowNumber) ->
                new InventoryStock(
                        UUID.fromString(row.getString("product_id")),
                        row.getInt("on_hand_quantity"),
                        row.getInt("reserved_quantity")))
                .stream().findFirst();
    }

    @Override
    public void reserve(UUID productId, int quantity) {
        var parameters = new MapSqlParameterSource()
                .addValue("productId", productId.toString())
                .addValue("quantity", quantity);
        if (jdbcTemplate.update(RESERVE_STOCK, parameters) != 1) {
            // An unexpected failure after locked validation is a technical error, so roll back.
            throw new IllegalStateException("Expected to reserve exactly one locked stock row");
        }
    }

    @Override
    public void release(UUID productId, int quantity) {
        var parameters = new MapSqlParameterSource()
                .addValue("productId", productId.toString())
                .addValue("quantity", quantity);
        if (jdbcTemplate.update(RELEASE_STOCK, parameters) != 1) {
            // Missing or inconsistent stock is a technical failure; roll back the whole release.
            throw new IllegalStateException("Expected to release exactly one stock row with sufficient reserved quantity");
        }
    }

    @Override
    public void commit(UUID productId, int quantity) {
        var parameters = new MapSqlParameterSource()
                .addValue("productId", productId.toString())
                .addValue("quantity", quantity);
        if (jdbcTemplate.update(COMMIT_STOCK, parameters) != 1) {
            // Missing or inconsistent stock is a technical failure; roll back every item.
            throw new IllegalStateException("Expected to commit exactly one stock row with sufficient reserved quantity");
        }
    }
}
