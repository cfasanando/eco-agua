package com.ecoamazonas.eco_agua.restaurant;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
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


    public RestaurantCashSummary cashSummary(LocalDate businessDate) {
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        int paidOrders = count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE status = 'PAID'
                  AND DATE(COALESCE(paid_at, updated_at, created_at)) = ?
                """, targetDate);
        BigDecimal paidTotal = amount("""
                SELECT COALESCE(SUM(subtotal), 0)
                FROM restaurant_order
                WHERE status = 'PAID'
                  AND DATE(COALESCE(paid_at, updated_at, created_at)) = ?
                """, targetDate);
        BigDecimal cashTotal = paymentTotal(targetDate, "CASH");
        BigDecimal cardTotal = paymentTotal(targetDate, "CARD");
        BigDecimal yapeTotal = paymentTotal(targetDate, "YAPE");
        BigDecimal plinTotal = paymentTotal(targetDate, "PLIN");
        BigDecimal transferTotal = paymentTotal(targetDate, "TRANSFER");
        BigDecimal otherTotal = amount("""
                SELECT COALESCE(SUM(subtotal), 0)
                FROM restaurant_order
                WHERE status = 'PAID'
                  AND DATE(COALESCE(paid_at, updated_at, created_at)) = ?
                  AND COALESCE(payment_method, 'OTHER') NOT IN ('CASH','CARD','YAPE','PLIN','TRANSFER')
                """, targetDate);
        int openOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status IN ('NEW','IN_KITCHEN','READY','SERVED')");
        BigDecimal openTotal = amount("SELECT COALESCE(SUM(subtotal), 0) FROM restaurant_order WHERE status IN ('NEW','IN_KITCHEN','READY','SERVED')");
        int cancelledOrders = count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE status = 'CANCELLED'
                  AND DATE(COALESCE(updated_at, created_at)) = ?
                """, targetDate);
        BigDecimal averageTicket = paidOrders == 0
                ? BigDecimal.ZERO
                : paidTotal.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);

        return new RestaurantCashSummary(
                targetDate,
                paidOrders,
                paidTotal,
                cashTotal,
                cardTotal,
                yapeTotal,
                plinTotal,
                transferTotal,
                otherTotal,
                openOrders,
                openTotal,
                cancelledOrders,
                averageTicket
        );
    }

    public List<RestaurantPaymentBreakdownRow> paymentBreakdown(LocalDate businessDate) {
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        return jdbcTemplate.query("""
                SELECT COALESCE(payment_method, 'OTHER') AS payment_method,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(subtotal), 0) AS total_amount
                FROM restaurant_order
                WHERE status = 'PAID'
                  AND DATE(COALESCE(paid_at, updated_at, created_at)) = ?
                GROUP BY COALESCE(payment_method, 'OTHER')
                ORDER BY FIELD(COALESCE(payment_method, 'OTHER'), 'CASH', 'YAPE', 'PLIN', 'CARD', 'TRANSFER', 'OTHER'), payment_method
                """, (rs, rowNum) -> new RestaurantPaymentBreakdownRow(
                rs.getString("payment_method"),
                rs.getInt("order_count"),
                rs.getBigDecimal("total_amount")
        ), targetDate);
    }

    public List<RestaurantCashOrderRow> paidOrdersForDate(LocalDate businessDate) {
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, o.subtotal, o.payment_method,
                       o.created_at, o.paid_at, COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status = 'PAID'
                  AND DATE(COALESCE(o.paid_at, o.updated_at, o.created_at)) = ?
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone,
                         o.status, o.subtotal, o.payment_method, o.created_at, o.paid_at
                ORDER BY COALESCE(o.paid_at, o.updated_at, o.created_at) DESC, o.id DESC
                """, cashOrderMapper(), targetDate);
    }


    public RestaurantCashOrderRow cashOrder(Long orderId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                           o.customer_name, o.customer_phone, o.status, o.subtotal, o.payment_method,
                           o.created_at, o.paid_at, COALESCE(COUNT(i.id), 0) AS item_count
                    FROM restaurant_order o
                    LEFT JOIN restaurant_table t ON t.id = o.table_id
                    LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                    WHERE o.id = ?
                    GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone,
                             o.status, o.subtotal, o.payment_method, o.created_at, o.paid_at
                    """, cashOrderMapper(), orderId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantOrderRow> openOrdersForCash() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('NEW','IN_KITCHEN','READY','SERVED')
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.notes, o.created_at
                ORDER BY o.created_at ASC
                """, orderMapper());
    }

    public List<RestaurantMenuItemRow> menuItems() {
        return jdbcTemplate.query(menuItemsSql(
                "p.active = true AND p.restaurant_visible = true AND p.restaurant_available = true AND COALESCE(p.stock, 0) > 0",
                "ORDER BY category_name ASC, p.restaurant_sort_order ASC, p.featured DESC, p.name ASC",
                ""), menuMapper());
    }

    public List<RestaurantMenuAdminRow> menuItemsAdmin() {
        return menuItemsAdmin("ALL");
    }

    public List<RestaurantMenuAdminRow> menuItemsAdmin(String stockFilter) {
        String cleanFilter = stockFilter == null ? "ALL" : stockFilter.trim().toUpperCase();
        String whereClause = switch (cleanFilter) {
            case "AVAILABLE" -> "WHERE p.active = true AND p.restaurant_visible = true AND p.restaurant_available = true AND COALESCE(p.stock, 0) > 0";
            case "LOW" -> "WHERE p.active = true AND COALESCE(p.stock, 0) > 0 AND COALESCE(p.minimum_stock, 0) > 0 AND COALESCE(p.stock, 0) <= COALESCE(p.minimum_stock, 0)";
            case "OUT" -> "WHERE COALESCE(p.stock, 0) <= 0 OR p.restaurant_available = false";
            case "HIDDEN" -> "WHERE p.active = false OR p.restaurant_visible = false";
            default -> "";
        };
        return jdbcTemplate.query(menuItemsAdminSql(whereClause, "ORDER BY category_name ASC, p.restaurant_sort_order ASC, p.name ASC"), menuAdminMapper());
    }

    public RestaurantStockSummary stockSummary() {
        int totalItems = count("SELECT COUNT(*) FROM product");
        int availableItems = count("""
                SELECT COUNT(*)
                FROM product
                WHERE active = true
                  AND restaurant_visible = true
                  AND restaurant_available = true
                  AND COALESCE(stock, 0) > 0
                """);
        int lowStockItems = count("""
                SELECT COUNT(*)
                FROM product
                WHERE active = true
                  AND COALESCE(stock, 0) > 0
                  AND COALESCE(minimum_stock, 0) > 0
                  AND COALESCE(stock, 0) <= COALESCE(minimum_stock, 0)
                """);
        int outOfStockItems = count("SELECT COUNT(*) FROM product WHERE COALESCE(stock, 0) <= 0 OR restaurant_available = false");
        int hiddenItems = count("SELECT COUNT(*) FROM product WHERE active = false OR restaurant_visible = false");
        return new RestaurantStockSummary(totalItems, availableItems, lowStockItems, outOfStockItems, hiddenItems);
    }

    public RestaurantMenuAdminRow menuItemAdmin(Long id) {
        try {
            return jdbcTemplate.queryForObject(menuItemsAdminSql("WHERE p.id = ?", ""), menuAdminMapper(), id);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantMenuCategoryRow> productCategories() {
        if (!tableExists("category")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, name
                FROM category
                WHERE active = true
                  AND type = 'PRODUCT'
                ORDER BY name ASC
                """, (rs, rowNum) -> new RestaurantMenuCategoryRow(
                rs.getLong("id"),
                rs.getString("name")
        ));
    }

    public List<RestaurantMenuGroupRow> menuGroups() {
        Map<String, List<RestaurantMenuItemRow>> grouped = menuItems().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        RestaurantMenuItemRow::categoryLabel,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> new RestaurantMenuGroupRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<RestaurantMenuItemRow> featuredMenuItems() {
        return jdbcTemplate.query(menuItemsSql(
                "p.active = true AND p.restaurant_visible = true AND p.restaurant_available = true AND COALESCE(p.stock, 0) > 0 AND p.featured = true",
                "ORDER BY category_name ASC, p.restaurant_sort_order ASC, p.name ASC",
                "LIMIT 8"), menuMapper());
    }

    @Transactional
    public Long createMenuItem(String name,
                               String description,
                               String imagePath,
                               BigDecimal price,
                               BigDecimal stock,
                               BigDecimal minimumStock,
                               Long categoryId,
                               String newCategoryName,
                               boolean active,
                               boolean featured,
                               boolean restaurantVisible,
                               boolean restaurantAvailable,
                               int sortOrder) {
        String cleanName = requireText(name, "Ingresa el nombre del plato.");
        BigDecimal cleanPrice = money(price);
        BigDecimal cleanStock = nonNegative(stock);
        BigDecimal cleanMinimumStock = nonNegative(minimumStock);
        boolean cleanRestaurantAvailable = restaurantAvailable && cleanStock.compareTo(BigDecimal.ZERO) > 0;
        Long resolvedCategoryId = resolveCategoryId(categoryId, newCategoryName);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO product
                    (name, description, image_path, category_id, price, active, featured, stock, minimum_stock,
                     restaurant_visible, restaurant_available, restaurant_sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, cleanName);
            ps.setString(2, blankToNull(description));
            ps.setString(3, blankToNull(imagePath));
            if (resolvedCategoryId == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, resolvedCategoryId);
            }
            ps.setBigDecimal(5, cleanPrice);
            ps.setBoolean(6, active);
            ps.setBoolean(7, featured);
            ps.setBigDecimal(8, cleanStock);
            ps.setBigDecimal(9, cleanMinimumStock);
            ps.setBoolean(10, restaurantVisible);
            ps.setBoolean(11, cleanRestaurantAvailable);
            ps.setInt(12, Math.max(0, sortOrder));
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @Transactional
    public void updateMenuItem(Long id,
                               String name,
                               String description,
                               String imagePath,
                               BigDecimal price,
                               BigDecimal stock,
                               BigDecimal minimumStock,
                               Long categoryId,
                               String newCategoryName,
                               boolean active,
                               boolean featured,
                               boolean restaurantVisible,
                               boolean restaurantAvailable,
                               int sortOrder) {
        String cleanName = requireText(name, "Ingresa el nombre del plato.");
        BigDecimal cleanPrice = money(price);
        BigDecimal cleanStock = nonNegative(stock);
        BigDecimal cleanMinimumStock = nonNegative(minimumStock);
        boolean cleanRestaurantAvailable = restaurantAvailable && cleanStock.compareTo(BigDecimal.ZERO) > 0;
        Long resolvedCategoryId = resolveCategoryId(categoryId, newCategoryName);
        int updated = jdbcTemplate.update("""
                UPDATE product
                SET name = ?, description = ?, image_path = ?, category_id = ?, price = ?, active = ?, featured = ?,
                    stock = ?, minimum_stock = ?, restaurant_visible = ?, restaurant_available = ?, restaurant_sort_order = ?
                WHERE id = ?
                """,
                cleanName, blankToNull(description), blankToNull(imagePath), resolvedCategoryId, cleanPrice, active, featured,
                cleanStock, cleanMinimumStock, restaurantVisible, cleanRestaurantAvailable, Math.max(0, sortOrder), id);
        if (updated == 0) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
    }

    @Transactional
    public void toggleMenuItemAvailability(Long id) {
        RestaurantMenuAdminRow item = menuItemAdmin(id);
        if (item == null) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
        if (item.restaurantAvailable()) {
            jdbcTemplate.update("UPDATE product SET restaurant_available = false WHERE id = ?", id);
            return;
        }
        if (item.safeStock().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repón stock antes de volver a marcar el plato como disponible.");
        }
        jdbcTemplate.update("UPDATE product SET restaurant_available = true WHERE id = ?", id);
    }

    @Transactional
    public void replenishMenuItemStock(Long id, BigDecimal quantity) {
        RestaurantMenuAdminRow item = menuItemAdmin(id);
        if (item == null) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
        BigDecimal cleanQuantity = nonNegative(quantity);
        if (cleanQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ingresa una cantidad mayor a cero para reponer stock.");
        }
        jdbcTemplate.update("""
                UPDATE product
                SET stock = COALESCE(stock, 0) + ?, restaurant_available = true, active = true
                WHERE id = ?
                """, cleanQuantity, id);
    }

    @Transactional
    public void toggleMenuItemVisibility(Long id) {
        int updated = jdbcTemplate.update("""
                UPDATE product
                SET restaurant_visible = CASE WHEN restaurant_visible = true THEN false ELSE true END
                WHERE id = ?
                """, id);
        if (updated == 0) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
    }

    @Transactional
    public void toggleMenuItemFeatured(Long id) {
        int updated = jdbcTemplate.update("""
                UPDATE product
                SET featured = CASE WHEN featured = true THEN false ELSE true END
                WHERE id = ?
                """, id);
        if (updated == 0) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
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

    public RestaurantPublicTableContext publicTableContext(Long tableId) {
        if (tableId == null) {
            return null;
        }

        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, code, name, area, seats, status
                    FROM restaurant_table
                    WHERE id = ? AND active = true
                    """, (rs, rowNum) -> new RestaurantPublicTableContext(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("area"),
                    rs.getInt("seats"),
                    rs.getString("status")
            ), tableId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
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
        jdbcTemplate.update("UPDATE restaurant_table SET status = ? WHERE id = ?", cleanStatus, tableId);
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
        assertSelectedProductsAvailable(selectedItems, quantities);

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
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'OCCUPIED' WHERE id = ?", finalTableId);
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
        assertSelectedProductsAvailable(selectedItems, quantities);

        insertOrderItems(orderId, selectedItems, quantities);
        recalculateOrderTotal(orderId);
        jdbcTemplate.update("UPDATE restaurant_order SET status = CASE WHEN status = 'NEW' THEN 'IN_KITCHEN' ELSE status END WHERE id = ?", orderId);
    }

    @Transactional
    public void updateItemQuantity(Long orderId, Long itemId, int quantity) {
        assertOrderEditable(orderId);
        int cleanQuantity = Math.max(1, Math.min(quantity, 99));
        RestaurantOrderItemStock item = orderItemForStock(orderId, itemId);
        int delta = cleanQuantity - item.quantity();
        if (delta > 0) {
            reserveProductStock(item.productId(), item.productName(), delta);
        } else if (delta < 0) {
            restoreProductStock(item.productId(), Math.abs(delta));
        }

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
        if (count("SELECT COUNT(*) FROM restaurant_order_item WHERE order_id = " + orderId) <= 1) {
            throw new IllegalArgumentException("La comanda debe mantener al menos un producto.");
        }
        RestaurantOrderItemStock item = orderItemForStock(orderId, itemId);
        int deleted = jdbcTemplate.update("DELETE FROM restaurant_order_item WHERE id = ? AND order_id = ?", itemId, orderId);
        if (deleted == 0) {
            throw new IllegalArgumentException("No se encontró el producto dentro de la comanda.");
        }
        restoreProductStock(item.productId(), item.quantity());
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

        jdbcTemplate.update("UPDATE restaurant_order SET status = ? WHERE id = ?", cleanStatus, orderId);
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
                SET status = 'PAID', payment_method = ?, paid_at = NOW()
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
        restoreOrderStock(orderId);
        jdbcTemplate.update("UPDATE restaurant_order SET status = 'CANCELLED' WHERE id = ?", orderId);
        releaseTableForOrder(orderId);
    }

    private void insertOrderItems(Long orderId, List<RestaurantMenuItemRow> selectedItems, Map<Long, Integer> quantities) {
        for (RestaurantMenuItemRow item : selectedItems) {
            int quantity = quantities.get(item.id());
            reserveProductStock(item.id(), item.name(), quantity);
            BigDecimal lineTotal = item.price().multiply(BigDecimal.valueOf(quantity));
            jdbcTemplate.update("""
                    INSERT INTO restaurant_order_item
                    (order_id, product_id, product_name, quantity, unit_price, line_total, kitchen_status)
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
                    """, orderId, item.id(), item.name(), quantity, item.price(), lineTotal);
        }
    }

    private void assertSelectedProductsAvailable(List<RestaurantMenuItemRow> selectedItems, Map<Long, Integer> quantities) {
        if (selectedItems.isEmpty() || selectedItems.size() != quantities.size()) {
            throw new IllegalArgumentException("Uno o más platos ya no están disponibles en carta.");
        }
        for (RestaurantMenuItemRow item : selectedItems) {
            int requestedQuantity = quantities.getOrDefault(item.id(), 0);
            if (requestedQuantity <= 0) {
                continue;
            }
            if (item.safeStock().compareTo(BigDecimal.valueOf(requestedQuantity)) < 0) {
                throw new IllegalArgumentException("Stock insuficiente para " + item.name() + ". Disponible: " + item.safeStock().stripTrailingZeros().toPlainString() + ".");
            }
        }
    }

    private void reserveProductStock(Long productId, String productName, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        int updated = jdbcTemplate.update("""
                UPDATE product
                SET stock = COALESCE(stock, 0) - ?,
                    restaurant_available = CASE WHEN COALESCE(stock, 0) - ? <= 0 THEN false ELSE restaurant_available END
                WHERE id = ?
                  AND active = true
                  AND restaurant_visible = true
                  AND restaurant_available = true
                  AND COALESCE(stock, 0) >= ?
                """, quantity, quantity, productId, quantity);
        if (updated == 0) {
            throw new IllegalArgumentException("Stock insuficiente o plato agotado: " + productName + ".");
        }
    }

    private void restoreProductStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE product
                SET stock = COALESCE(stock, 0) + ?
                WHERE id = ?
                """, quantity, productId);
    }

    private void restoreOrderStock(Long orderId) {
        List<RestaurantOrderItemStock> items = jdbcTemplate.query("""
                SELECT id, order_id, product_id, product_name, quantity
                FROM restaurant_order_item
                WHERE order_id = ?
                """, orderItemStockMapper(), orderId);
        for (RestaurantOrderItemStock item : items) {
            restoreProductStock(item.productId(), item.quantity());
        }
    }

    private RestaurantOrderItemStock orderItemForStock(Long orderId, Long itemId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, order_id, product_id, product_name, quantity
                    FROM restaurant_order_item
                    WHERE id = ? AND order_id = ?
                    """, orderItemStockMapper(), itemId, orderId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("No se encontró el producto dentro de la comanda.");
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
                ), 0)
                WHERE id = ?
                """, orderId, orderId);
    }

    private void releaseTableForOrder(Long orderId) {
        Long tableId = jdbcTemplate.query("SELECT table_id FROM restaurant_order WHERE id = ?", rs -> rs.next() ? nullableLong(rs, "table_id") : null, orderId);
        if (tableId != null && tableId > 0) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'FREE' WHERE id = ?", tableId);
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

    private String menuItemsSql(String whereClause, String orderClause, String limitClause) {
        boolean hasCategoryTable = tableExists("category");
        String categoryColumns = hasCategoryTable
                ? "p.category_id, COALESCE(c.name, 'Carta general') AS category_name"
                : "NULL AS category_id, 'Carta general' AS category_name";
        String categoryJoin = hasCategoryTable ? "LEFT JOIN category c ON c.id = p.category_id" : "";
        return """
                SELECT p.id, p.name, p.description, p.image_path, p.price, p.featured, p.stock,
                       %s, p.restaurant_visible, p.restaurant_available, p.restaurant_sort_order
                FROM product p
                %s
                WHERE %s
                %s
                %s
                """.formatted(categoryColumns, categoryJoin, whereClause, orderClause, limitClause);
    }

    private String menuItemsAdminSql(String whereClause, String orderClause) {
        boolean hasCategoryTable = tableExists("category");
        String categoryColumns = hasCategoryTable
                ? "p.category_id, COALESCE(c.name, 'Carta general') AS category_name"
                : "NULL AS category_id, 'Carta general' AS category_name";
        String categoryJoin = hasCategoryTable ? "LEFT JOIN category c ON c.id = p.category_id" : "";
        return """
                SELECT p.id, p.name, p.description, p.image_path, p.price, p.active, p.featured, p.stock, p.minimum_stock,
                       %s, p.restaurant_visible, p.restaurant_available, p.restaurant_sort_order
                FROM product p
                %s
                %s
                %s
                """.formatted(categoryColumns, categoryJoin, whereClause == null ? "" : whereClause, orderClause == null ? "" : orderClause);
    }

    private Long resolveCategoryId(Long categoryId, String newCategoryName) {
        String cleanNewCategory = newCategoryName == null ? "" : newCategoryName.trim();
        if (!cleanNewCategory.isBlank()) {
            return ensureProductCategory(cleanNewCategory);
        }
        return categoryId;
    }

    private Long ensureProductCategory(String name) {
        if (!tableExists("category")) {
            return null;
        }
        Long existing = jdbcTemplate.query("""
                SELECT id
                FROM category
                WHERE LOWER(name) = LOWER(?)
                  AND type = 'PRODUCT'
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, name);
        if (existing != null) {
            return existing;
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO category
                    (name, description, type, active, cost_behavior, include_in_break_even, include_in_operational_reading, personnel_mode, created_at)
                    VALUES (?, 'Categoría de carta restaurante', 'PRODUCT', true, 'NON_OPERATING', false, false, 'NONE', NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private String requireText(String value, String message) {
        String clean = value == null ? "" : value.trim();
        if (clean.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return clean;
    }

    private BigDecimal money(BigDecimal value) {
        BigDecimal clean = value == null ? BigDecimal.ZERO : value;
        if (clean.compareTo(BigDecimal.ZERO) < 0) {
            clean = BigDecimal.ZERO;
        }
        return clean.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal clean = value == null ? BigDecimal.ZERO : value;
        return clean.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : clean;
    }

    private boolean tableExists(String tableName) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return value != null && value > 0;
    }


    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private BigDecimal amount(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal paymentTotal(LocalDate businessDate, String paymentMethod) {
        return amount("""
                SELECT COALESCE(SUM(subtotal), 0)
                FROM restaurant_order
                WHERE status = 'PAID'
                  AND DATE(COALESCE(paid_at, updated_at, created_at)) = ?
                  AND COALESCE(payment_method, 'OTHER') = ?
                """, businessDate, paymentMethod);
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
                rs.getBigDecimal("stock"),
                nullableLong(rs, "category_id"),
                rs.getString("category_name"),
                rs.getBoolean("restaurant_visible"),
                rs.getBoolean("restaurant_available"),
                rs.getInt("restaurant_sort_order")
        );
    }

    private RowMapper<RestaurantMenuAdminRow> menuAdminMapper() {
        return (rs, rowNum) -> new RestaurantMenuAdminRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("image_path"),
                rs.getBigDecimal("price"),
                rs.getBoolean("active"),
                rs.getBoolean("featured"),
                rs.getBigDecimal("stock"),
                rs.getBigDecimal("minimum_stock"),
                nullableLong(rs, "category_id"),
                rs.getString("category_name"),
                rs.getBoolean("restaurant_visible"),
                rs.getBoolean("restaurant_available"),
                rs.getInt("restaurant_sort_order")
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


    private RowMapper<RestaurantCashOrderRow> cashOrderMapper() {
        return (rs, rowNum) -> new RestaurantCashOrderRow(
                rs.getLong("id"),
                rs.getString("order_code"),
                rs.getString("service_type"),
                nullableLong(rs, "table_id"),
                rs.getString("table_name"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("status"),
                rs.getBigDecimal("subtotal"),
                rs.getString("payment_method"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("paid_at")),
                rs.getInt("item_count")
        );
    }

    private RowMapper<RestaurantOrderItemStock> orderItemStockMapper() {
        return (rs, rowNum) -> new RestaurantOrderItemStock(
                rs.getLong("id"),
                rs.getLong("order_id"),
                nullableLong(rs, "product_id"),
                rs.getString("product_name"),
                rs.getInt("quantity")
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
