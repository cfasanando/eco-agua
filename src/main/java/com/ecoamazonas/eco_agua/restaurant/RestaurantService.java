package com.ecoamazonas.eco_agua.restaurant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RestaurantService {

    private static final List<String> VALID_TABLE_STATUSES = List.of("FREE", "OCCUPIED", "RESERVED", "DISABLED");
    private static final List<String> VALID_ORDER_STATUSES = List.of("NEW", "IN_KITCHEN", "READY", "SERVED", "PAID", "CANCELLED");
    private static final List<String> VALID_SERVICE_TYPES = List.of("DINE_IN", "TAKEAWAY", "DELIVERY");

    private final JdbcTemplate jdbcTemplate;

    public RestaurantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RestaurantDashboardSummary dashboardSummary() {
        int totalTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true");
        int freeTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true AND status = 'FREE'");
        int occupiedTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true AND status = 'OCCUPIED'");
        int reservedTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true AND status = 'RESERVED'");
        int activeOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status IN ('NEW','IN_KITCHEN','READY','SERVED')");
        int kitchenPendingOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status IN ('NEW','IN_KITCHEN')");
        int readyOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status = 'READY'");
        BigDecimal todaySales = amount("SELECT COALESCE(SUM(subtotal),0) FROM restaurant_order WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED'");
        return new RestaurantDashboardSummary(totalTables, freeTables, occupiedTables, reservedTables, activeOrders, kitchenPendingOrders, readyOrders, todaySales);
    }

    public List<RestaurantMenuItemRow> menuItems() {
        return jdbcTemplate.query("""
                SELECT id, name, description, image_path, price, featured, stock
                FROM product
                WHERE active = true
                ORDER BY featured DESC, name ASC
                """, menuMapper());
    }

    public List<RestaurantMenuItemRow> featuredMenuItems() {
        return jdbcTemplate.query("""
                SELECT id, name, description, image_path, price, featured, stock
                FROM product
                WHERE active = true AND featured = true
                ORDER BY name ASC
                LIMIT 8
                """, menuMapper());
    }

    public List<RestaurantTableRow> tables() {
        return jdbcTemplate.query("""
                SELECT id, code, name, area, seats, status, active, notes
                FROM restaurant_table
                ORDER BY area ASC, name ASC
                """, tableMapper());
    }

    public List<RestaurantTableRow> availableTables() {
        return jdbcTemplate.query("""
                SELECT id, code, name, area, seats, status, active, notes
                FROM restaurant_table
                WHERE active = true AND status IN ('FREE','RESERVED')
                ORDER BY area ASC, name ASC
                """, tableMapper());
    }

    public List<RestaurantOrderRow> activeOrders() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('NEW','IN_KITCHEN','READY','SERVED')
                GROUP BY o.id, o.order_code, o.service_type, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at
                ORDER BY o.created_at DESC
                """, orderMapper());
    }

    public List<RestaurantOrderRow> kitchenOrders() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('NEW','IN_KITCHEN','READY')
                GROUP BY o.id, o.order_code, o.service_type, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at
                ORDER BY FIELD(o.status, 'READY', 'NEW', 'IN_KITCHEN'), o.created_at ASC
                """, orderMapper());
    }

    public Map<Long, List<RestaurantOrderItemRow>> itemsByOrder(List<RestaurantOrderRow> orders) {
        Map<Long, List<RestaurantOrderItemRow>> result = new LinkedHashMap<>();
        for (RestaurantOrderRow order : orders) {
            result.put(order.id(), orderItems(order.id()));
        }
        return result;
    }

    public List<RestaurantOrderItemRow> orderItems(Long orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, product_name, quantity, unit_price, line_total, kitchen_status
                FROM restaurant_order_item
                WHERE order_id = ?
                ORDER BY id ASC
                """, itemMapper(), orderId);
    }

    public void updateTableStatus(Long tableId, String status) {
        String cleanStatus = normalize(status, VALID_TABLE_STATUSES, "FREE");
        jdbcTemplate.update("UPDATE restaurant_table SET status = ?, updated_at = NOW() WHERE id = ?", cleanStatus, tableId);
    }

    @Transactional
    public Long createOrder(String serviceType,
                            Long tableId,
                            String customerName,
                            String customerPhone,
                            String notes,
                            Map<String, String> requestParams) {
        String cleanServiceType = normalize(serviceType, VALID_SERVICE_TYPES, "DINE_IN");
        Map<Long, Integer> quantities = selectedQuantities(requestParams);
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto a la comanda.");
        }

        List<RestaurantMenuItemRow> selectedItems = menuItems().stream()
                .filter(item -> quantities.containsKey(item.id()))
                .toList();
        if (selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Los productos seleccionados ya no están disponibles.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (RestaurantMenuItemRow item : selectedItems) {
            subtotal = subtotal.add(item.price().multiply(BigDecimal.valueOf(quantities.get(item.id()))));
        }

        Long finalTableId = "DINE_IN".equals(cleanServiceType) ? tableId : null;
        String orderCode = nextOrderCode();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        BigDecimal finalSubtotal = subtotal;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO restaurant_order
                    (order_code, service_type, table_id, customer_name, customer_phone, status, subtotal, notes, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'IN_KITCHEN', ?, ?, NOW(), NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderCode);
            ps.setString(2, cleanServiceType);
            if (finalTableId == null) {
                ps.setObject(3, null);
            } else {
                ps.setLong(3, finalTableId);
            }
            ps.setString(4, blankToNull(customerName));
            ps.setString(5, blankToNull(customerPhone));
            ps.setBigDecimal(6, finalSubtotal);
            ps.setString(7, blankToNull(notes));
            return ps;
        }, keyHolder);

        Long orderId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        for (RestaurantMenuItemRow item : selectedItems) {
            int quantity = quantities.get(item.id());
            BigDecimal lineTotal = item.price().multiply(BigDecimal.valueOf(quantity));
            jdbcTemplate.update("""
                    INSERT INTO restaurant_order_item
                    (order_id, product_id, product_name, quantity, unit_price, line_total, kitchen_status)
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
                    """, orderId, item.id(), item.name(), quantity, item.price(), lineTotal);
        }

        if (finalTableId != null) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'OCCUPIED', updated_at = NOW() WHERE id = ?", finalTableId);
        }

        return orderId;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        String cleanStatus = normalize(status, VALID_ORDER_STATUSES, "IN_KITCHEN");
        jdbcTemplate.update("UPDATE restaurant_order SET status = ?, updated_at = NOW() WHERE id = ?", cleanStatus, orderId);
        if ("READY".equals(cleanStatus)) {
            jdbcTemplate.update("UPDATE restaurant_order_item SET kitchen_status = 'READY' WHERE order_id = ?", orderId);
        }
        if ("PAID".equals(cleanStatus) || "CANCELLED".equals(cleanStatus)) {
            Long tableId = jdbcTemplate.query("SELECT table_id FROM restaurant_order WHERE id = ?", rs -> rs.next() ? rs.getLong(1) : null, orderId);
            if (tableId != null && tableId > 0) {
                jdbcTemplate.update("UPDATE restaurant_table SET status = 'FREE', updated_at = NOW() WHERE id = ?", tableId);
            }
        }
    }

    private Map<Long, Integer> selectedQuantities(Map<String, String> requestParams) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        requestParams.forEach((key, value) -> {
            if (key != null && key.startsWith("qty_")) {
                try {
                    Long productId = Long.parseLong(key.substring(4));
                    int quantity = Integer.parseInt(value == null || value.isBlank() ? "0" : value.trim());
                    if (quantity > 0) {
                        quantities.put(productId, Math.min(quantity, 99));
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed quantity inputs from the browser.
                }
            }
        });
        return quantities;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private BigDecimal amount(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nextOrderCode() {
        return "CMD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String normalize(String value, List<String> allowed, String fallback) {
        String clean = value == null ? "" : value.trim().toUpperCase();
        return allowed.contains(clean) ? clean : fallback;
    }

    private String blankToNull(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? null : clean;
    }

    private RowMapper<RestaurantMenuItemRow> menuMapper() {
        return (rs, rowNum) -> new RestaurantMenuItemRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("image_path"),
                rs.getBigDecimal("price"),
                rs.getBoolean("featured"),
                rs.getBigDecimal("stock")
        );
    }

    private RowMapper<RestaurantTableRow> tableMapper() {
        return (rs, rowNum) -> new RestaurantTableRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("area"),
                rs.getInt("seats"),
                rs.getString("status"),
                rs.getBoolean("active"),
                rs.getString("notes")
        );
    }

    private RowMapper<RestaurantOrderRow> orderMapper() {
        return (rs, rowNum) -> new RestaurantOrderRow(
                rs.getLong("id"),
                rs.getString("order_code"),
                rs.getString("service_type"),
                rs.getString("table_name"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("status"),
                rs.getBigDecimal("subtotal"),
                rs.getString("notes"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                rs.getInt("item_count")
        );
    }

    private RowMapper<RestaurantOrderItemRow> itemMapper() {
        return (rs, rowNum) -> new RestaurantOrderItemRow(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("line_total"),
                rs.getString("kitchen_status")
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
