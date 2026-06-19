package com.ecoamazonas.eco_agua.restaurant;

import org.springframework.dao.EmptyResultDataAccessException;
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
    private static final List<String> VALID_PAYMENT_METHODS = List.of("CASH", "CARD", "YAPE", "PLIN", "TRANSFER", "OTHER");
    private static final List<String> CLOSED_ORDER_STATUSES = List.of("PAID", "CANCELLED");

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
        BigDecimal todaySales = amount("SELECT COALESCE(SUM(subtotal),0) FROM restaurant_order WHERE DATE(created_at) = CURDATE() AND status = 'PAID'");
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

    public List<RestaurantTableBoardRow> tableBoard() {
        return jdbcTemplate.query("""
                SELECT rt.id, rt.code, rt.name, rt.area, rt.seats, rt.status, rt.active, rt.notes,
                       o.id AS order_id, o.order_code, o.status AS order_status, o.subtotal AS order_subtotal,
                       o.customer_name, o.customer_phone, o.created_at AS order_created_at,
                       CASE WHEN o.created_at IS NULL THEN NULL ELSE TIMESTAMPDIFF(MINUTE, o.created_at, NOW()) END AS order_minutes
                FROM restaurant_table rt
                LEFT JOIN restaurant_order o ON o.id = (
                    SELECT ro.id
                    FROM restaurant_order ro
                    WHERE ro.table_id = rt.id
                      AND ro.status IN ('NEW','IN_KITCHEN','READY','SERVED')
                    ORDER BY ro.created_at DESC, ro.id DESC
                    LIMIT 1
                )
                WHERE rt.active = true
                ORDER BY rt.area ASC, rt.name ASC
                """, tableBoardMapper());
    }

    public List<RestaurantTableRow> availableTables() {
        return jdbcTemplate.query("""
                SELECT id, code, name, area, seats, status, active, notes
                FROM restaurant_table
                WHERE active = true
                  AND status IN ('FREE','RESERVED')
                  AND id NOT IN (
                      SELECT table_id
                      FROM restaurant_order
                      WHERE table_id IS NOT NULL
                        AND status IN ('NEW','IN_KITCHEN','READY','SERVED')
                  )
                ORDER BY area ASC, name ASC
                """, tableMapper());
    }

    public RestaurantOrderRow order(Long orderId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                           o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at,
                           COALESCE(COUNT(i.id), 0) AS item_count
                    FROM restaurant_order o
                    LEFT JOIN restaurant_table t ON t.id = o.table_id
                    LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                    WHERE o.id = ?
                    GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at
                    """, orderMapper(), orderId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantOrderRow> activeOrders() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('NEW','IN_KITCHEN','READY','SERVED')
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at
                ORDER BY o.created_at DESC
                """, orderMapper());
    }

    public List<RestaurantOrderRow> kitchenOrders() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('NEW','IN_KITCHEN','READY')
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at
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
        if ("DINE_IN".equals(cleanServiceType) && tableId == null) {
            throw new IllegalArgumentException("Selecciona una mesa para atención en salón.");
        }

        Map<Long, Integer> quantities = selectedQuantities(requestParams);
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto a la comanda.");
        }

        List<RestaurantMenuItemRow> selectedItems = selectedMenuItems(quantities);
        if (selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Los productos seleccionados ya no están disponibles.");
        }

        Long finalTableId = "DINE_IN".equals(cleanServiceType) ? tableId : null;
        if (finalTableId != null) {
            assertTableAvailable(finalTableId);
        }

        BigDecimal subtotal = subtotal(selectedItems, quantities);
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
        insertOrderItems(orderId, selectedItems, quantities);

        if (finalTableId != null) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'OCCUPIED', updated_at = NOW() WHERE id = ?", finalTableId);
        }

        return orderId;
    }

    @Transactional
    public void addItemsToOrder(Long orderId, Map<String, String> requestParams) {
        assertOrderEditable(orderId);
        Map<Long, Integer> quantities = selectedQuantities(requestParams);
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un producto para agregar.");
        }

        List<RestaurantMenuItemRow> selectedItems = selectedMenuItems(quantities);
        if (selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Los productos seleccionados ya no están disponibles.");
        }

        insertOrderItems(orderId, selectedItems, quantities);
        recalculateOrderTotal(orderId);
        jdbcTemplate.update("UPDATE restaurant_order SET status = CASE WHEN status = 'NEW' THEN 'IN_KITCHEN' ELSE status END, updated_at = NOW() WHERE id = ?", orderId);
    }

    @Transactional
    public void updateItemQuantity(Long orderId, Long itemId, int quantity) {
        assertOrderEditable(orderId);
        int cleanQuantity = Math.max(1, Math.min(quantity, 99));
        int updated = jdbcTemplate.update("""
                UPDATE restaurant_order_item
                SET quantity = ?, line_total = unit_price * ?, kitchen_status = CASE WHEN kitchen_status = 'READY' THEN 'PENDING' ELSE kitchen_status END
                WHERE id = ? AND order_id = ?
                """, cleanQuantity, cleanQuantity, itemId, orderId);
        if (updated == 0) {
            throw new IllegalArgumentException("No se encontró el producto dentro de la comanda.");
        }
        recalculateOrderTotal(orderId);
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId) {
        assertOrderEditable(orderId);
        int deleted = jdbcTemplate.update("DELETE FROM restaurant_order_item WHERE id = ? AND order_id = ?", itemId, orderId);
        if (deleted == 0) {
            throw new IllegalArgumentException("No se encontró el producto dentro de la comanda.");
        }
        if (count("SELECT COUNT(*) FROM restaurant_order_item WHERE order_id = " + orderId) == 0) {
            throw new IllegalArgumentException("La comanda debe mantener al menos un producto.");
        }
        recalculateOrderTotal(orderId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        String cleanStatus = normalize(status, VALID_ORDER_STATUSES, "IN_KITCHEN");
        if ("PAID".equals(cleanStatus)) {
            payOrder(orderId, "CASH");
            return;
        }
        if ("CANCELLED".equals(cleanStatus)) {
            cancelOrder(orderId);
            return;
        }

        RestaurantOrderRow order = assertOrderExists(orderId);
        if (order.isClosed()) {
            throw new IllegalArgumentException("No se puede cambiar una comanda cerrada.");
        }

        jdbcTemplate.update("UPDATE restaurant_order SET status = ?, updated_at = NOW() WHERE id = ?", cleanStatus, orderId);
        if ("READY".equals(cleanStatus)) {
            jdbcTemplate.update("UPDATE restaurant_order_item SET kitchen_status = 'READY' WHERE order_id = ?", orderId);
        }
        if ("IN_KITCHEN".equals(cleanStatus)) {
            jdbcTemplate.update("UPDATE restaurant_order_item SET kitchen_status = 'PENDING' WHERE order_id = ? AND kitchen_status <> 'READY'", orderId);
        }
    }

    @Transactional
    public void payOrder(Long orderId, String paymentMethod) {
        RestaurantOrderRow order = assertOrderExists(orderId);
        if (order.isClosed()) {
            throw new IllegalArgumentException("La comanda ya está cerrada.");
        }
        recalculateOrderTotal(orderId);
        String cleanPaymentMethod = normalize(paymentMethod, VALID_PAYMENT_METHODS, "CASH");
        jdbcTemplate.update("""
                UPDATE restaurant_order
                SET status = 'PAID', payment_method = ?, paid_at = NOW(), updated_at = NOW()
                WHERE id = ?
                """, cleanPaymentMethod, orderId);
        releaseTableForOrder(orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        RestaurantOrderRow order = assertOrderExists(orderId);
        if ("PAID".equals(order.safeStatus())) {
            throw new IllegalArgumentException("No se puede anular una comanda pagada desde esta pantalla.");
        }
        jdbcTemplate.update("UPDATE restaurant_order SET status = 'CANCELLED', updated_at = NOW() WHERE id = ?", orderId);
        releaseTableForOrder(orderId);
    }

    private void insertOrderItems(Long orderId, List<RestaurantMenuItemRow> selectedItems, Map<Long, Integer> quantities) {
        for (RestaurantMenuItemRow item : selectedItems) {
            int quantity = quantities.get(item.id());
            BigDecimal lineTotal = item.price().multiply(BigDecimal.valueOf(quantity));
            jdbcTemplate.update("""
                    INSERT INTO restaurant_order_item
                    (order_id, product_id, product_name, quantity, unit_price, line_total, kitchen_status)
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
                    """, orderId, item.id(), item.name(), quantity, item.price(), lineTotal);
        }
    }

    private List<RestaurantMenuItemRow> selectedMenuItems(Map<Long, Integer> quantities) {
        return menuItems().stream()
                .filter(item -> quantities.containsKey(item.id()))
                .toList();
    }

    private BigDecimal subtotal(List<RestaurantMenuItemRow> selectedItems, Map<Long, Integer> quantities) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (RestaurantMenuItemRow item : selectedItems) {
            subtotal = subtotal.add(item.price().multiply(BigDecimal.valueOf(quantities.get(item.id()))));
        }
        return subtotal;
    }

    private void recalculateOrderTotal(Long orderId) {
        jdbcTemplate.update("""
                UPDATE restaurant_order
                SET subtotal = COALESCE((
                    SELECT SUM(line_total)
                    FROM restaurant_order_item
                    WHERE order_id = ?
                ), 0), updated_at = NOW()
                WHERE id = ?
                """, orderId, orderId);
    }

    private void releaseTableForOrder(Long orderId) {
        Long tableId = jdbcTemplate.query("SELECT table_id FROM restaurant_order WHERE id = ?", rs -> rs.next() ? nullableLong(rs, "table_id") : null, orderId);
        if (tableId != null && tableId > 0) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'FREE', updated_at = NOW() WHERE id = ?", tableId);
        }
    }

    private void assertTableAvailable(Long tableId) {
        String status = jdbcTemplate.query("SELECT status FROM restaurant_table WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, tableId);
        if (status == null) {
            throw new IllegalArgumentException("La mesa seleccionada no existe.");
        }
        if ("DISABLED".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("La mesa seleccionada está fuera de servicio.");
        }
        Integer activeOrders = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE table_id = ? AND status IN ('NEW','IN_KITCHEN','READY','SERVED')
                """, Integer.class, tableId);
        if (activeOrders != null && activeOrders > 0) {
            throw new IllegalArgumentException("La mesa seleccionada ya tiene una comanda activa.");
        }
    }

    private RestaurantOrderRow assertOrderExists(Long orderId) {
        RestaurantOrderRow order = order(orderId);
        if (order == null) {
            throw new IllegalArgumentException("La comanda solicitada no existe.");
        }
        return order;
    }

    private void assertOrderEditable(Long orderId) {
        RestaurantOrderRow order = assertOrderExists(orderId);
        if (CLOSED_ORDER_STATUSES.contains(order.safeStatus())) {
            throw new IllegalArgumentException("No se puede editar una comanda pagada o anulada.");
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

    private RowMapper<RestaurantTableBoardRow> tableBoardMapper() {
        return (rs, rowNum) -> new RestaurantTableBoardRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("area"),
                rs.getInt("seats"),
                rs.getString("status"),
                rs.getBoolean("active"),
                rs.getString("notes"),
                nullableLong(rs, "order_id"),
                rs.getString("order_code"),
                rs.getString("order_status"),
                rs.getBigDecimal("order_subtotal"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                toLocalDateTime(rs.getTimestamp("order_created_at")),
                nullableInteger(rs, "order_minutes")
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
                nullableLong(rs, "table_id"),
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

    private Long nullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
