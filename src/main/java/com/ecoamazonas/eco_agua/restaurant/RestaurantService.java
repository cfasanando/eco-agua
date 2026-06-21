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
import java.util.UUID;

@Service
public class RestaurantService {

    private static final List<String> VALID_TABLE_STATUSES = List.of("FREE", "OCCUPIED", "RESERVED", "DISABLED");
    private static final List<String> VALID_ORDER_STATUSES = List.of("NEW", "CONFIRMED", "IN_KITCHEN", "READY", "OUT_FOR_DELIVERY", "DELIVERED", "SERVED", "PAID", "CANCELLED");
    private static final List<String> VALID_SERVICE_TYPES = List.of("DINE_IN", "TAKEAWAY", "DELIVERY");
    private static final List<String> VALID_PAYMENT_METHODS = List.of("CASH", "CARD", "YAPE", "PLIN", "TRANSFER", "OTHER");
    private static final List<String> VALID_TABLE_REQUEST_TYPES = List.of("ATTENTION", "BILL", "PAID_NOTICE", "WAITER", "NOTE");
    private static final List<String> VALID_TABLE_REQUEST_STATUSES = List.of("PENDING", "RESOLVED");
    private static final List<String> VALID_QR_ORDER_STATUSES = List.of("PENDING", "APPROVED", "REJECTED");
    private static final List<String> VALID_RESERVATION_STATUSES = List.of("PENDING", "CONFIRMED", "ATTENDED", "CANCELLED", "NO_SHOW");
    private static final List<String> ACTIVE_RESERVATION_STATUSES = List.of("PENDING", "CONFIRMED");
    private static final List<String> CLOSED_ORDER_STATUSES = List.of("PAID", "CANCELLED");
    private static final List<String> EXTERNAL_ORDER_SERVICE_TYPES = List.of("TAKEAWAY", "DELIVERY");
    private static final List<String> EXTERNAL_ORDER_ACTIVE_STATUSES = List.of("NEW", "CONFIRMED", "IN_KITCHEN", "READY", "OUT_FOR_DELIVERY", "DELIVERED");
    private static final List<String> VALID_INGREDIENT_UNITS = List.of("UNIT", "KG", "G", "L", "ML", "PORTION");
    private static final List<String> VALID_STOCK_CONTROL_MODES = List.of("PRODUCT", "RECIPE", "NONE");

    private record RecipeStockRequirement(
            Long ingredientId,
            String ingredientName,
            String unitCode,
            BigDecimal quantityPerUnit,
            BigDecimal currentStock,
            boolean active
    ) {
    }

    private final JdbcTemplate jdbcTemplate;

    public RestaurantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RestaurantDashboardSummary dashboardSummary() {
        int totalTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true");
        int freeTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true AND status = 'FREE'");
        int occupiedTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true AND status = 'OCCUPIED'");
        int reservedTables = count("SELECT COUNT(*) FROM restaurant_table WHERE active = true AND status = 'RESERVED'");
        int activeOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED') AND NOT (service_type IN ('TAKEAWAY','DELIVERY') AND status = 'DELIVERED' AND paid_at IS NOT NULL)");
        int kitchenPendingOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status = 'IN_KITCHEN' OR (status = 'NEW' AND service_type = 'DINE_IN')");
        int readyOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE status = 'READY'");
        BigDecimal todaySales = amount("SELECT COALESCE(SUM(subtotal + COALESCE(delivery_fee, 0)),0) FROM restaurant_order WHERE paid_at IS NOT NULL AND DATE(paid_at) = CURDATE()");
        return new RestaurantDashboardSummary(totalTables, freeTables, occupiedTables, reservedTables, activeOrders, kitchenPendingOrders, readyOrders, todaySales);
    }


    public RestaurantCashSummary cashSummary(LocalDate businessDate) {
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        int paidOrders = count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE paid_at IS NOT NULL
                  AND DATE(paid_at) = ?
                """, targetDate);
        BigDecimal paidTotal = amount("""
                SELECT COALESCE(SUM(subtotal + COALESCE(delivery_fee, 0)), 0)
                FROM restaurant_order
                WHERE paid_at IS NOT NULL
                  AND DATE(paid_at) = ?
                """, targetDate);
        BigDecimal cashTotal = paymentTotal(targetDate, "CASH");
        BigDecimal cardTotal = paymentTotal(targetDate, "CARD");
        BigDecimal yapeTotal = paymentTotal(targetDate, "YAPE");
        BigDecimal plinTotal = paymentTotal(targetDate, "PLIN");
        BigDecimal transferTotal = paymentTotal(targetDate, "TRANSFER");
        BigDecimal otherTotal = amount("""
                SELECT COALESCE(SUM(subtotal + COALESCE(delivery_fee, 0)), 0)
                FROM restaurant_order
                WHERE paid_at IS NOT NULL
                  AND DATE(paid_at) = ?
                  AND COALESCE(payment_method, 'OTHER') NOT IN ('CASH','CARD','YAPE','PLIN','TRANSFER')
                """, targetDate);
        int openOrders = count("SELECT COUNT(*) FROM restaurant_order WHERE paid_at IS NULL AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')");
        BigDecimal openTotal = amount("SELECT COALESCE(SUM(subtotal + COALESCE(delivery_fee, 0)), 0) FROM restaurant_order WHERE paid_at IS NULL AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')");
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

    public List<RestaurantTableRequestRow> pendingTableRequests() {
        return tableRequests("PENDING", 30);
    }

    public List<RestaurantTableRequestRow> recentTableRequests() {
        return tableRequests("ALL", 80);
    }

    public List<RestaurantTableRequestRow> tableRequests(String statusFilter) {
        return tableRequests(statusFilter, 120);
    }

    private List<RestaurantTableRequestRow> tableRequests(String statusFilter, int limit) {
        if (!tableExists("restaurant_table_request")) {
            return List.of();
        }
        String cleanStatus = statusFilter == null ? "PENDING" : statusFilter.trim().toUpperCase();
        String whereClause = switch (cleanStatus) {
            case "RESOLVED" -> "WHERE r.status = 'RESOLVED'";
            case "ALL" -> "";
            default -> "WHERE r.status = 'PENDING'";
        };
        int cleanLimit = Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.query("""
                SELECT r.id, r.table_id, t.name AS table_name, t.area AS table_area,
                       r.request_type, r.customer_note, r.status, r.created_at, r.resolved_at
                FROM restaurant_table_request r
                JOIN restaurant_table t ON t.id = r.table_id
                %s
                ORDER BY CASE WHEN r.status = 'PENDING' THEN 0 ELSE 1 END, r.created_at DESC, r.id DESC
                LIMIT %d
                """.formatted(whereClause, cleanLimit), tableRequestMapper());
    }

    public int pendingTableRequestCount() {
        if (!tableExists("restaurant_table_request")) {
            return 0;
        }
        return count("SELECT COUNT(*) FROM restaurant_table_request WHERE status = 'PENDING'");
    }

    public int pendingBillRequestCount() {
        if (!tableExists("restaurant_table_request")) {
            return 0;
        }
        return count("SELECT COUNT(*) FROM restaurant_table_request WHERE status = 'PENDING' AND request_type IN ('BILL','PAID_NOTICE')");
    }

    @Transactional
    public void createTableRequest(Long tableId, String requestType, String customerNote) {
        if (!tableExists("restaurant_table_request")) {
            throw new IllegalArgumentException("La bandeja de solicitudes aún no está instalada.");
        }
        RestaurantPublicTableContext tableContext = publicTableContext(tableId);
        if (tableContext == null) {
            throw new IllegalArgumentException("No se pudo identificar la mesa del QR.");
        }
        String cleanType = normalize(requestType, VALID_TABLE_REQUEST_TYPES, "ATTENTION");
        String cleanNote = limitText(customerNote, 500);
        jdbcTemplate.update("""
                INSERT INTO restaurant_table_request
                (table_id, request_type, customer_note, status, created_at, updated_at)
                VALUES (?, ?, ?, 'PENDING', NOW(), NOW())
                """, tableId, cleanType, blankToNull(cleanNote));
    }

    @Transactional
    public void resolveTableRequest(Long requestId) {
        if (!tableExists("restaurant_table_request")) {
            throw new IllegalArgumentException("La bandeja de solicitudes aún no está instalada.");
        }
        int updated = jdbcTemplate.update("""
                UPDATE restaurant_table_request
                SET status = 'RESOLVED', resolved_at = NOW(), updated_at = NOW()
                WHERE id = ?
                """, requestId);
        if (updated == 0) {
            throw new IllegalArgumentException("La solicitud seleccionada no existe.");
        }
    }

    public List<RestaurantQrOrderRow> pendingQrOrders() {
        return qrOrders("PENDING", 20);
    }

    public List<RestaurantQrOrderRow> qrOrders(String statusFilter) {
        return qrOrders(statusFilter, 120);
    }

    private List<RestaurantQrOrderRow> qrOrders(String statusFilter, int limit) {
        if (!tableExists("restaurant_qr_order")) {
            return List.of();
        }
        String cleanStatus = statusFilter == null ? "PENDING" : statusFilter.trim().toUpperCase();
        String whereClause = switch (cleanStatus) {
            case "APPROVED" -> "WHERE q.status = 'APPROVED'";
            case "REJECTED" -> "WHERE q.status = 'REJECTED'";
            case "ALL" -> "";
            default -> "WHERE q.status = 'PENDING'";
        };
        int cleanLimit = Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.query("""
                SELECT q.id, q.table_id, t.name AS table_name, t.area AS table_area,
                       q.customer_note, q.status, q.subtotal, q.approved_order_id,
                       q.created_at, q.processed_at, COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_qr_order q
                JOIN restaurant_table t ON t.id = q.table_id
                LEFT JOIN restaurant_qr_order_item i ON i.qr_order_id = q.id
                %s
                GROUP BY q.id, q.table_id, t.name, t.area, q.customer_note, q.status, q.subtotal,
                         q.approved_order_id, q.created_at, q.processed_at
                ORDER BY CASE WHEN q.status = 'PENDING' THEN 0 ELSE 1 END, q.created_at DESC, q.id DESC
                LIMIT %d
                """.formatted(whereClause, cleanLimit), qrOrderMapper());
    }

    public RestaurantQrOrderRow qrOrder(Long qrOrderId) {
        if (!tableExists("restaurant_qr_order")) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT q.id, q.table_id, t.name AS table_name, t.area AS table_area,
                           q.customer_note, q.status, q.subtotal, q.approved_order_id,
                           q.created_at, q.processed_at, COALESCE(COUNT(i.id), 0) AS item_count
                    FROM restaurant_qr_order q
                    JOIN restaurant_table t ON t.id = q.table_id
                    LEFT JOIN restaurant_qr_order_item i ON i.qr_order_id = q.id
                    WHERE q.id = ?
                    GROUP BY q.id, q.table_id, t.name, t.area, q.customer_note, q.status, q.subtotal,
                             q.approved_order_id, q.created_at, q.processed_at
                    """, qrOrderMapper(), qrOrderId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantQrOrderItemRow> qrOrderItems(Long qrOrderId) {
        if (!tableExists("restaurant_qr_order_item")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, qr_order_id, product_id, product_name, quantity, unit_price, line_total
                FROM restaurant_qr_order_item
                WHERE qr_order_id = ?
                ORDER BY id ASC
                """, qrOrderItemMapper(), qrOrderId);
    }

    public Map<Long, List<RestaurantQrOrderItemRow>> itemsByQrOrder(List<RestaurantQrOrderRow> orders) {
        Map<Long, List<RestaurantQrOrderItemRow>> result = new LinkedHashMap<>();
        for (RestaurantQrOrderRow order : orders) {
            result.put(order.id(), qrOrderItems(order.id()));
        }
        return result;
    }

    public int pendingQrOrderCount() {
        if (!tableExists("restaurant_qr_order")) {
            return 0;
        }
        return count("SELECT COUNT(*) FROM restaurant_qr_order WHERE status = 'PENDING'");
    }

    @Transactional
    public Long createQrOrder(Long tableId, String customerNote, Map<String, String> requestParams) {
        if (!tableExists("restaurant_qr_order") || !tableExists("restaurant_qr_order_item")) {
            throw new IllegalArgumentException("La bandeja de pedidos QR aún no está instalada.");
        }
        RestaurantPublicTableContext tableContext = publicTableContext(tableId);
        if (tableContext == null) {
            throw new IllegalArgumentException("No se pudo identificar la mesa del QR.");
        }
        if ("DISABLED".equalsIgnoreCase(tableContext.status())) {
            throw new IllegalArgumentException("Esta mesa está fuera de servicio.");
        }

        Map<Long, Integer> quantities = selectedQuantities(requestParams);
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un producto para enviar el pedido.");
        }

        List<RestaurantMenuItemRow> selectedItems = selectedMenuItems(quantities);
        assertSelectedProductsAvailable(selectedItems, quantities);
        BigDecimal subtotal = subtotal(selectedItems, quantities);
        String cleanNote = limitText(customerNote, 500);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO restaurant_qr_order
                    (table_id, customer_note, status, subtotal, created_at, updated_at)
                    VALUES (?, ?, 'PENDING', ?, NOW(), NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, tableId);
            ps.setString(2, blankToNull(cleanNote));
            ps.setBigDecimal(3, subtotal);
            return ps;
        }, keyHolder);

        Long qrOrderId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        for (RestaurantMenuItemRow item : selectedItems) {
            int quantity = quantities.get(item.id());
            BigDecimal lineTotal = item.safePrice().multiply(BigDecimal.valueOf(quantity));
            jdbcTemplate.update("""
                    INSERT INTO restaurant_qr_order_item
                    (qr_order_id, product_id, product_name, quantity, unit_price, line_total)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, qrOrderId, item.id(), item.name(), quantity, item.safePrice(), lineTotal);
        }
        return qrOrderId;
    }

    @Transactional
    public Long approveQrOrder(Long qrOrderId) {
        lockPendingQrOrder(qrOrderId);
        RestaurantQrOrderRow qrOrder = qrOrder(qrOrderId);
        if (qrOrder == null) {
            throw new IllegalArgumentException("El pedido QR seleccionado no existe.");
        }
        if (!qrOrder.isPending()) {
            throw new IllegalArgumentException("Este pedido QR ya fue procesado.");
        }
        List<RestaurantQrOrderItemRow> items = qrOrderItems(qrOrderId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("El pedido QR no tiene productos.");
        }

        Map<String, String> quantityParams = qrOrderQuantityParams(items);
        Long orderId = activeOrderIdForTable(qrOrder.tableId());
        if (orderId == null) {
            orderId = createOrder("DINE_IN", qrOrder.tableId(), null, null, qrOrderNote(qrOrder), quantityParams);
        } else {
            addItemsToOrder(orderId, quantityParams);
            appendQrOrderNote(orderId, qrOrder.customerNote());
        }

        jdbcTemplate.update("""
                UPDATE restaurant_qr_order
                SET status = 'APPROVED', approved_order_id = ?, processed_at = NOW(), updated_at = NOW()
                WHERE id = ? AND status = 'PENDING'
                """, orderId, qrOrderId);
        return orderId;
    }

    private void lockPendingQrOrder(Long qrOrderId) {
        try {
            String status = jdbcTemplate.queryForObject("""
                    SELECT status
                    FROM restaurant_qr_order
                    WHERE id = ?
                    FOR UPDATE
                    """, String.class, qrOrderId);
            if (!"PENDING".equalsIgnoreCase(status)) {
                throw new IllegalArgumentException("Este pedido QR ya fue procesado.");
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("El pedido QR seleccionado no existe.");
        }
    }

    @Transactional
    public void rejectQrOrder(Long qrOrderId) {
        if (!tableExists("restaurant_qr_order")) {
            throw new IllegalArgumentException("La bandeja de pedidos QR aún no está instalada.");
        }
        int updated = jdbcTemplate.update("""
                UPDATE restaurant_qr_order
                SET status = 'REJECTED', processed_at = NOW(), updated_at = NOW()
                WHERE id = ? AND status = 'PENDING'
                """, qrOrderId);
        if (updated == 0) {
            throw new IllegalArgumentException("El pedido QR seleccionado no existe o ya fue procesado.");
        }
    }

    public List<RestaurantReservationRow> reservations(LocalDate businessDate, String statusFilter) {
        if (!tableExists("restaurant_reservation")) {
            return List.of();
        }
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        String cleanStatus = normalizeReservationFilter(statusFilter);
        String statusClause = switch (cleanStatus) {
            case "PENDING", "CONFIRMED", "ATTENDED", "CANCELLED", "NO_SHOW" -> "AND r.status = '" + cleanStatus + "'";
            case "ACTIVE" -> "AND r.status IN ('PENDING','CONFIRMED')";
            default -> "";
        };
        return jdbcTemplate.query("""
                SELECT r.id, r.reservation_code, r.table_id, t.name AS table_name, t.area AS table_area,
                       r.customer_name, r.customer_phone, r.reservation_at, r.duration_minutes,
                       r.party_size, r.status, r.notes, r.order_id, r.created_at, r.updated_at
                FROM restaurant_reservation r
                JOIN restaurant_table t ON t.id = r.table_id
                WHERE DATE(r.reservation_at) = ?
                %s
                ORDER BY r.reservation_at ASC, r.id ASC
                """.formatted(statusClause), reservationMapper(), targetDate);
    }

    public Map<String, Integer> reservationStatusCounts(LocalDate businessDate) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("ALL", 0);
        counts.put("PENDING", 0);
        counts.put("CONFIRMED", 0);
        counts.put("ATTENDED", 0);
        counts.put("CANCELLED", 0);
        counts.put("NO_SHOW", 0);
        if (!tableExists("restaurant_reservation")) {
            return counts;
        }
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        jdbcTemplate.query("""
                SELECT status, COUNT(*) AS total
                FROM restaurant_reservation
                WHERE DATE(reservation_at) = ?
                GROUP BY status
                """, rs -> {
            String status = rs.getString("status");
            int total = rs.getInt("total");
            if (status != null) {
                counts.put(status.toUpperCase(), total);
            }
            counts.put("ALL", counts.get("ALL") + total);
        }, targetDate);
        return counts;
    }

    public List<RestaurantReservationRow> upcomingReservations() {
        return upcomingReservations(12, 2);
    }

    public Map<Long, RestaurantReservationRow> nextReservationsByTable() {
        Map<Long, RestaurantReservationRow> result = new LinkedHashMap<>();
        for (RestaurantReservationRow reservation : upcomingReservations(200, 7)) {
            result.putIfAbsent(reservation.tableId(), reservation);
        }
        return result;
    }

    private List<RestaurantReservationRow> upcomingReservations(int limit, int daysAhead) {
        if (!tableExists("restaurant_reservation")) {
            return List.of();
        }
        int cleanLimit = Math.max(1, Math.min(limit, 250));
        int cleanDaysAhead = Math.max(1, Math.min(daysAhead, 30));
        return jdbcTemplate.query("""
                SELECT r.id, r.reservation_code, r.table_id, t.name AS table_name, t.area AS table_area,
                       r.customer_name, r.customer_phone, r.reservation_at, r.duration_minutes,
                       r.party_size, r.status, r.notes, r.order_id, r.created_at, r.updated_at
                FROM restaurant_reservation r
                JOIN restaurant_table t ON t.id = r.table_id
                WHERE r.status IN ('PENDING','CONFIRMED')
                  AND DATE_ADD(r.reservation_at, INTERVAL r.duration_minutes MINUTE) >= NOW()
                  AND r.reservation_at <= DATE_ADD(NOW(), INTERVAL %d DAY)
                ORDER BY r.reservation_at ASC, r.id ASC
                LIMIT %d
                """.formatted(cleanDaysAhead, cleanLimit), reservationMapper());
    }

    public RestaurantReservationRow reservation(Long reservationId) {
        if (!tableExists("restaurant_reservation") || reservationId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT r.id, r.reservation_code, r.table_id, t.name AS table_name, t.area AS table_area,
                           r.customer_name, r.customer_phone, r.reservation_at, r.duration_minutes,
                           r.party_size, r.status, r.notes, r.order_id, r.created_at, r.updated_at
                    FROM restaurant_reservation r
                    JOIN restaurant_table t ON t.id = r.table_id
                    WHERE r.id = ?
                    """, reservationMapper(), reservationId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantTableRow> reservationTables() {
        return jdbcTemplate.query("""
                SELECT id, code, name, area, seats, status, active, notes
                FROM restaurant_table
                WHERE active = true AND status <> 'DISABLED'
                ORDER BY area ASC, name ASC
                """, tableMapper());
    }

    @Transactional
    public Long createReservation(Long tableId,
                                  String customerName,
                                  String customerPhone,
                                  LocalDateTime reservationAt,
                                  int durationMinutes,
                                  int partySize,
                                  String status,
                                  String notes) {
        String cleanName = requireText(customerName, "Ingresa el nombre del cliente.");
        String cleanPhone = limitText(customerPhone, 40);
        String cleanStatus = normalize(status, VALID_RESERVATION_STATUSES, "PENDING");
        if (!ACTIVE_RESERVATION_STATUSES.contains(cleanStatus)) {
            cleanStatus = "PENDING";
        }
        String cleanNotes = limitText(notes, 2000);
        validateReservation(tableId, reservationAt, durationMinutes, partySize, cleanStatus, null);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String reservationCode = nextReservationCode();
        String finalStatus = cleanStatus;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO restaurant_reservation
                    (reservation_code, table_id, customer_name, customer_phone, reservation_at,
                     duration_minutes, party_size, status, notes, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, reservationCode);
            ps.setLong(2, tableId);
            ps.setString(3, cleanName);
            ps.setString(4, blankToNull(cleanPhone));
            ps.setTimestamp(5, Timestamp.valueOf(reservationAt));
            ps.setInt(6, durationMinutes);
            ps.setInt(7, partySize);
            ps.setString(8, finalStatus);
            ps.setString(9, blankToNull(cleanNotes));
            return ps;
        }, keyHolder);
        refreshTableReservationStatus(tableId);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @Transactional
    public void updateReservation(Long reservationId,
                                  Long tableId,
                                  String customerName,
                                  String customerPhone,
                                  LocalDateTime reservationAt,
                                  int durationMinutes,
                                  int partySize,
                                  String status,
                                  String notes) {
        RestaurantReservationRow current = assertReservationExists(reservationId);
        if (!current.canEdit()) {
            throw new IllegalArgumentException("La reserva ya fue cerrada o convertida en comanda.");
        }
        String cleanName = requireText(customerName, "Ingresa el nombre del cliente.");
        String cleanPhone = limitText(customerPhone, 40);
        String cleanStatus = normalize(status, ACTIVE_RESERVATION_STATUSES, current.safeStatus());
        String cleanNotes = limitText(notes, 2000);
        validateReservation(tableId, reservationAt, durationMinutes, partySize, cleanStatus, reservationId);

        jdbcTemplate.update("""
                UPDATE restaurant_reservation
                SET table_id = ?, customer_name = ?, customer_phone = ?, reservation_at = ?,
                    duration_minutes = ?, party_size = ?, status = ?, notes = ?, updated_at = NOW()
                WHERE id = ?
                """, tableId, cleanName, blankToNull(cleanPhone), Timestamp.valueOf(reservationAt),
                durationMinutes, partySize, cleanStatus, blankToNull(cleanNotes), reservationId);
        refreshTableReservationStatus(current.tableId());
        refreshTableReservationStatus(tableId);
    }

    @Transactional
    public void updateReservationStatus(Long reservationId, String status) {
        RestaurantReservationRow current = assertReservationExists(reservationId);
        String cleanStatus = normalize(status, VALID_RESERVATION_STATUSES, current.safeStatus());
        if (current.isClosed() && !current.safeStatus().equals(cleanStatus)) {
            throw new IllegalArgumentException("Una reserva cerrada no puede volver a abrirse.");
        }
        if (current.safeStatus().equals(cleanStatus)) {
            refreshTableReservationStatus(current.tableId());
            return;
        }
        if ("CONFIRMED".equals(cleanStatus)) {
            validateReservation(current.tableId(), current.reservationAt(), current.durationMinutes(),
                    current.partySize(), cleanStatus, current.id());
        }
        if (current.hasOrder() && !"ATTENDED".equals(cleanStatus)) {
            throw new IllegalArgumentException("La reserva ya está asociada a una comanda y debe permanecer atendida.");
        }
        jdbcTemplate.update("""
                UPDATE restaurant_reservation
                SET status = ?, updated_at = NOW()
                WHERE id = ?
                """, cleanStatus, reservationId);
        refreshTableReservationStatus(current.tableId());
    }

    public RestaurantReservationRow reservationForOrder(Long reservationId) {
        RestaurantReservationRow reservation = assertReservationExists(reservationId);
        if (!reservation.canOpenOrder()) {
            throw new IllegalArgumentException("La reserva ya fue cerrada o convertida en comanda.");
        }
        assertTableAvailable(reservation.tableId());
        return reservation;
    }

    @Transactional
    public Long createOrderFromReservation(Long reservationId,
                                           String notes,
                                           Map<String, String> requestParams) {
        RestaurantReservationRow reservation = reservationForOrder(reservationId);
        String reservationNote = limitText(reservation.notes(), 1000);
        String cleanNotes = limitText(notes, 1000);
        String combinedNotes = cleanNotes.isBlank() ? reservationNote : cleanNotes;
        Long orderId = createOrder(
                "DINE_IN",
                reservation.tableId(),
                reservation.customerName(),
                reservation.customerPhone(),
                combinedNotes,
                requestParams
        );
        jdbcTemplate.update("""
                UPDATE restaurant_reservation
                SET status = 'ATTENDED', order_id = ?, updated_at = NOW()
                WHERE id = ?
                """, orderId, reservationId);
        return orderId;
    }

    public List<RestaurantPaymentBreakdownRow> paymentBreakdown(LocalDate businessDate) {
        LocalDate targetDate = businessDate == null ? LocalDate.now() : businessDate;
        return jdbcTemplate.query("""
                SELECT COALESCE(payment_method, 'OTHER') AS payment_method,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(subtotal + COALESCE(delivery_fee, 0)), 0) AS total_amount
                FROM restaurant_order
                WHERE paid_at IS NOT NULL
                  AND DATE(paid_at) = ?
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
                       o.customer_name, o.customer_phone, o.status, (o.subtotal + COALESCE(o.delivery_fee, 0)) AS subtotal, o.payment_method,
                       o.created_at, o.paid_at, COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.paid_at IS NOT NULL
                  AND DATE(o.paid_at) = ?
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone,
                         o.status, o.subtotal, o.delivery_fee, o.payment_method, o.created_at, o.paid_at
                ORDER BY COALESCE(o.paid_at, o.updated_at, o.created_at) DESC, o.id DESC
                """, cashOrderMapper(), targetDate);
    }


    public RestaurantCashOrderRow cashOrder(Long orderId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                           o.customer_name, o.customer_phone, o.status, (o.subtotal + COALESCE(o.delivery_fee, 0)) AS subtotal, o.payment_method,
                           o.created_at, o.paid_at, COALESCE(COUNT(i.id), 0) AS item_count
                    FROM restaurant_order o
                    LEFT JOIN restaurant_table t ON t.id = o.table_id
                    LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                    WHERE o.id = ?
                    GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone,
                             o.status, o.subtotal, o.delivery_fee, o.payment_method, o.created_at, o.paid_at
                    """, cashOrderMapper(), orderId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantOrderRow> openOrdersForCash() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, (o.subtotal + COALESCE(o.delivery_fee, 0)) AS subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.paid_at IS NULL
                  AND o.status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.delivery_fee, o.notes, o.created_at
                ORDER BY o.created_at ASC
                """, orderMapper());
    }

    public List<RestaurantExternalOrderRow> externalOrders(String statusFilter) {
        String filter = normalizeExternalOrderFilter(statusFilter);
        String statusCondition = switch (filter) {
            case "ACTIVE" -> """
                     AND o.status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED')
                     AND NOT (o.status = 'DELIVERED' AND o.paid_at IS NOT NULL)
                    """;
            case "PAID" -> " AND o.paid_at IS NOT NULL";
            case "ALL" -> "";
            default -> " AND o.status = '" + filter + "'";
        };

        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.customer_name, o.customer_phone,
                       o.delivery_address, o.delivery_reference, o.scheduled_at, o.delivery_fee,
                       o.status, o.subtotal, o.payment_method, o.paid_at, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.service_type IN ('TAKEAWAY','DELIVERY')
                %s
                GROUP BY o.id, o.order_code, o.service_type, o.customer_name, o.customer_phone,
                         o.delivery_address, o.delivery_reference, o.scheduled_at, o.delivery_fee,
                         o.status, o.subtotal, o.payment_method, o.paid_at, o.notes, o.created_at
                ORDER BY FIELD(o.status, 'NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','PAID','CANCELLED'),
                         CASE WHEN o.scheduled_at IS NULL THEN 1 ELSE 0 END,
                         o.scheduled_at ASC,
                         o.created_at DESC
                """.formatted(statusCondition), externalOrderMapper());
    }

    public List<RestaurantExternalOrderRow> externalOrdersForDashboard() {
        return externalOrders("ACTIVE").stream().limit(6).toList();
    }

    public int activeExternalOrderCount() {
        return count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE service_type IN ('TAKEAWAY','DELIVERY')
                  AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED')
                  AND NOT (status = 'DELIVERED' AND paid_at IS NOT NULL)
                """);
    }

    public Map<String, Integer> externalOrderStatusCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("ALL", count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE service_type IN ('TAKEAWAY','DELIVERY')
                """));
        counts.put("ACTIVE", activeExternalOrderCount());
        for (String status : List.of("NEW", "CONFIRMED", "IN_KITCHEN", "READY", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")) {
            counts.put(status, count("""
                    SELECT COUNT(*)
                    FROM restaurant_order
                    WHERE service_type IN ('TAKEAWAY','DELIVERY')
                      AND status = ?
                    """, status));
        }
        counts.put("PAID", count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE service_type IN ('TAKEAWAY','DELIVERY')
                  AND paid_at IS NOT NULL
                """));
        return counts;
    }

    public RestaurantExternalOrderRow externalOrder(Long orderId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT o.id, o.order_code, o.service_type, o.customer_name, o.customer_phone,
                           o.delivery_address, o.delivery_reference, o.scheduled_at, o.delivery_fee,
                           o.status, o.subtotal, o.payment_method, o.paid_at, o.notes, o.created_at,
                           COALESCE(COUNT(i.id), 0) AS item_count
                    FROM restaurant_order o
                    LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                    WHERE o.id = ?
                      AND o.service_type IN ('TAKEAWAY','DELIVERY')
                    GROUP BY o.id, o.order_code, o.service_type, o.customer_name, o.customer_phone,
                             o.delivery_address, o.delivery_reference, o.scheduled_at, o.delivery_fee,
                             o.status, o.subtotal, o.payment_method, o.paid_at, o.notes, o.created_at
                    """, externalOrderMapper(), orderId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Transactional
    public Long createExternalOrder(String serviceType,
                                    String customerName,
                                    String customerPhone,
                                    String deliveryAddress,
                                    String deliveryReference,
                                    LocalDateTime scheduledAt,
                                    BigDecimal deliveryFee,
                                    String notes,
                                    Map<String, String> requestParams) {
        String cleanServiceType = normalize(serviceType, EXTERNAL_ORDER_SERVICE_TYPES, "TAKEAWAY");
        String cleanCustomerName = requireText(customerName, "Ingresa el nombre del cliente.");
        String cleanCustomerPhone = requireText(customerPhone, "Ingresa el teléfono del cliente.");

        String cleanDeliveryAddress = blankToNull(deliveryAddress);
        String cleanDeliveryReference = blankToNull(deliveryReference);
        if ("DELIVERY".equals(cleanServiceType) && cleanDeliveryAddress == null) {
            throw new IllegalArgumentException("Ingresa la dirección de entrega.");
        }

        if (scheduledAt != null && scheduledAt.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("La hora estimada no puede estar en el pasado.");
        }

        BigDecimal cleanDeliveryFee = "DELIVERY".equals(cleanServiceType)
                ? nonNegative(deliveryFee)
                : BigDecimal.ZERO;

        Map<Long, Integer> quantities = selectedQuantities(requestParams);
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto al pedido.");
        }

        List<RestaurantMenuItemRow> selectedItems = selectedMenuItems(quantities);
        assertSelectedProductsAvailable(selectedItems, quantities);
        BigDecimal subtotal = subtotal(selectedItems, quantities);
        String orderCode = nextOrderCode();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO restaurant_order
                    (order_code, service_type, table_id, customer_name, customer_phone,
                     delivery_address, delivery_reference, scheduled_at, status,
                     subtotal, delivery_fee, notes, created_at, updated_at)
                    VALUES (?, ?, NULL, ?, ?, ?, ?, ?, 'NEW', ?, ?, ?, NOW(), NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderCode);
            ps.setString(2, cleanServiceType);
            ps.setString(3, cleanCustomerName);
            ps.setString(4, cleanCustomerPhone);
            ps.setString(5, cleanDeliveryAddress);
            ps.setString(6, cleanDeliveryReference);
            ps.setTimestamp(7, scheduledAt == null ? null : Timestamp.valueOf(scheduledAt));
            ps.setBigDecimal(8, subtotal);
            ps.setBigDecimal(9, cleanDeliveryFee);
            ps.setString(10, blankToNull(notes));
            return ps;
        }, keyHolder);

        Long orderId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        insertOrderItems(orderId, selectedItems, quantities);
        return orderId;
    }

    @Transactional
    public void updateExternalOrderStatus(Long orderId, String status) {
        RestaurantExternalOrderRow order = externalOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("El pedido externo seleccionado no existe.");
        }
        if (order.isClosed()) {
            throw new IllegalArgumentException("El pedido externo ya está cerrado.");
        }

        String cleanStatus = normalize(status, VALID_ORDER_STATUSES, order.safeStatus());
        if ("CANCELLED".equals(cleanStatus)) {
            cancelOrder(orderId);
            return;
        }
        if (cleanStatus.equals(order.safeStatus())) {
            return;
        }

        boolean validTransition = switch (order.safeStatus()) {
            case "NEW" -> "CONFIRMED".equals(cleanStatus);
            case "CONFIRMED" -> "IN_KITCHEN".equals(cleanStatus);
            case "IN_KITCHEN" -> "READY".equals(cleanStatus);
            case "READY" -> order.isDelivery()
                    ? "OUT_FOR_DELIVERY".equals(cleanStatus)
                    : "DELIVERED".equals(cleanStatus);
            case "OUT_FOR_DELIVERY" -> order.isDelivery() && "DELIVERED".equals(cleanStatus);
            default -> false;
        };

        if (!validTransition) {
            throw new IllegalArgumentException("La transición de estado no es válida para este pedido.");
        }

        jdbcTemplate.update("UPDATE restaurant_order SET status = ?, updated_at = NOW() WHERE id = ?", cleanStatus, orderId);
        if ("IN_KITCHEN".equals(cleanStatus)) {
            jdbcTemplate.update("""
                    UPDATE restaurant_order_item
                    SET kitchen_status = 'PENDING'
                    WHERE order_id = ?
                    """, orderId);
        } else if ("READY".equals(cleanStatus)) {
            jdbcTemplate.update("""
                    UPDATE restaurant_order_item
                    SET kitchen_status = 'READY'
                    WHERE order_id = ?
                    """, orderId);
        }
    }

    public List<RestaurantIngredientRow> ingredients(String stockFilter) {
        if (!tableExists("restaurant_ingredient")) {
            return List.of();
        }
        String cleanFilter = stockFilter == null ? "ALL" : stockFilter.trim().toUpperCase();
        String whereClause = switch (cleanFilter) {
            case "ACTIVE" -> "WHERE active = true";
            case "LOW" -> "WHERE active = true AND stock > 0 AND minimum_stock > 0 AND stock <= minimum_stock";
            case "OUT" -> "WHERE active = true AND stock <= 0";
            case "INACTIVE" -> "WHERE active = false";
            default -> "";
        };
        return jdbcTemplate.query("""
                SELECT id, name, unit_code, unit_cost, stock, minimum_stock, active, notes
                FROM restaurant_ingredient
                %s
                ORDER BY active DESC, name ASC
                """.formatted(whereClause), ingredientMapper());
    }

    public RestaurantIngredientSummary ingredientSummary() {
        if (!tableExists("restaurant_ingredient")) {
            return new RestaurantIngredientSummary(0, 0, 0, 0, 0);
        }
        int total = count("SELECT COUNT(*) FROM restaurant_ingredient");
        int active = count("SELECT COUNT(*) FROM restaurant_ingredient WHERE active = true");
        int low = count("SELECT COUNT(*) FROM restaurant_ingredient WHERE active = true AND stock > 0 AND minimum_stock > 0 AND stock <= minimum_stock");
        int out = count("SELECT COUNT(*) FROM restaurant_ingredient WHERE active = true AND stock <= 0");
        int inactive = count("SELECT COUNT(*) FROM restaurant_ingredient WHERE active = false");
        return new RestaurantIngredientSummary(total, active, low, out, inactive);
    }

    public RestaurantIngredientRow ingredient(Long ingredientId) {
        if (ingredientId == null || !tableExists("restaurant_ingredient")) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, name, unit_code, unit_cost, stock, minimum_stock, active, notes
                    FROM restaurant_ingredient
                    WHERE id = ?
                    """, ingredientMapper(), ingredientId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Transactional
    public Long createIngredient(String name,
                                 String unitCode,
                                 BigDecimal unitCost,
                                 BigDecimal stock,
                                 BigDecimal minimumStock,
                                 boolean active,
                                 String notes) {
        String cleanName = requireText(name, "Ingresa el nombre del ingrediente.");
        ensureIngredientNameAvailable(cleanName, null);
        String cleanUnit = normalize(unitCode, VALID_INGREDIENT_UNITS, "UNIT");
        BigDecimal cleanUnitCost = decimal4(unitCost);
        BigDecimal cleanStock = decimal4(stock);
        BigDecimal cleanMinimumStock = decimal4(minimumStock);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO restaurant_ingredient
                    (name, unit_code, unit_cost, stock, minimum_stock, active, notes, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, cleanName);
            ps.setString(2, cleanUnit);
            ps.setBigDecimal(3, cleanUnitCost);
            ps.setBigDecimal(4, cleanStock);
            ps.setBigDecimal(5, cleanMinimumStock);
            ps.setBoolean(6, active);
            ps.setString(7, blankToNull(notes));
            return ps;
        }, keyHolder);
        Long ingredientId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        if (cleanStock.compareTo(BigDecimal.ZERO) != 0) {
            recordIngredientMovement(ingredientId, "OPENING", cleanStock, null, null,
                    "Stock inicial del ingrediente");
        }
        return ingredientId;
    }

    @Transactional
    public void updateIngredient(Long ingredientId,
                                 String name,
                                 String unitCode,
                                 BigDecimal unitCost,
                                 BigDecimal stock,
                                 BigDecimal minimumStock,
                                 boolean active,
                                 String notes) {
        if (ingredient(ingredientId) == null) {
            throw new IllegalArgumentException("El ingrediente seleccionado no existe.");
        }
        String cleanName = requireText(name, "Ingresa el nombre del ingrediente.");
        ensureIngredientNameAvailable(cleanName, ingredientId);
        String cleanUnit = normalize(unitCode, VALID_INGREDIENT_UNITS, "UNIT");
        BigDecimal previousStock = lockIngredientStock(ingredientId);
        BigDecimal cleanStock = decimal4(stock);
        int updated = jdbcTemplate.update("""
                UPDATE restaurant_ingredient
                SET name = ?, unit_code = ?, unit_cost = ?, stock = ?, minimum_stock = ?, active = ?, notes = ?, updated_at = NOW()
                WHERE id = ?
                """, cleanName, cleanUnit, decimal4(unitCost), cleanStock, decimal4(minimumStock), active,
                blankToNull(notes), ingredientId);
        if (updated == 0) {
            throw new IllegalArgumentException("El ingrediente seleccionado no existe.");
        }
        BigDecimal difference = cleanStock.subtract(previousStock).setScale(4, RoundingMode.HALF_UP);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            recordIngredientMovement(ingredientId, "ADJUSTMENT", difference, null, null,
                    "Ajuste desde la edición del ingrediente");
        }
    }

    @Transactional
    public void toggleIngredientActive(Long ingredientId) {
        int updated = jdbcTemplate.update("""
                UPDATE restaurant_ingredient
                SET active = CASE WHEN active = true THEN false ELSE true END, updated_at = NOW()
                WHERE id = ?
                """, ingredientId);
        if (updated == 0) {
            throw new IllegalArgumentException("El ingrediente seleccionado no existe.");
        }
    }

    @Transactional
    public void replenishIngredientStock(Long ingredientId, BigDecimal quantity, String notes) {
        RestaurantIngredientRow ingredient = ingredient(ingredientId);
        if (ingredient == null) {
            throw new IllegalArgumentException("El ingrediente seleccionado no existe.");
        }
        BigDecimal cleanQuantity = positiveDecimal4(quantity, "Ingresa una cantidad mayor a cero para reponer stock.");
        lockIngredientStock(ingredientId);
        jdbcTemplate.update("""
                UPDATE restaurant_ingredient
                SET stock = stock + ?, active = true, updated_at = NOW()
                WHERE id = ?
                """, cleanQuantity, ingredientId);
        recordIngredientMovement(ingredientId, "REPLENISHMENT", cleanQuantity, null, null,
                blankToNull(notes) == null ? "Reposición manual de stock" : notes);
    }

    public List<RestaurantIngredientMovementRow> ingredientMovements(Long ingredientId) {
        if (ingredientId == null || !tableExists("restaurant_ingredient_movement")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT m.id, m.ingredient_id, i.name AS ingredient_name, i.unit_code,
                       m.movement_type, m.quantity_change, m.balance_after,
                       m.order_id, m.order_item_id, m.notes, m.created_at
                FROM restaurant_ingredient_movement m
                JOIN restaurant_ingredient i ON i.id = m.ingredient_id
                WHERE m.ingredient_id = ?
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT 300
                """, ingredientMovementMapper(), ingredientId);
    }

    public List<RestaurantRecipeItemRow> recipeItems(Long productId) {
        if (productId == null || !tableExists("restaurant_recipe_item") || !tableExists("restaurant_ingredient")) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT r.id, r.product_id, r.ingredient_id, i.name AS ingredient_name, i.unit_code,
                       i.unit_cost, r.quantity, i.active AS ingredient_active
                FROM restaurant_recipe_item r
                JOIN restaurant_ingredient i ON i.id = r.ingredient_id
                WHERE r.product_id = ?
                ORDER BY i.name ASC, r.id ASC
                """, recipeItemMapper(), productId);
    }

    @Transactional
    public void saveRecipeItem(Long productId, Long ingredientId, BigDecimal quantity) {
        requireRecipeProduct(productId);
        RestaurantIngredientRow ingredient = ingredient(ingredientId);
        if (ingredient == null) {
            throw new IllegalArgumentException("El ingrediente seleccionado no existe.");
        }
        if (!ingredient.active()) {
            throw new IllegalArgumentException("Activa el ingrediente antes de agregarlo a una receta.");
        }
        BigDecimal cleanQuantity = positiveDecimal4(quantity, "Ingresa una cantidad mayor a cero para la receta.");
        jdbcTemplate.update("""
                INSERT INTO restaurant_recipe_item
                (product_id, ingredient_id, quantity, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), updated_at = NOW()
                """, productId, ingredientId, cleanQuantity);
    }

    @Transactional
    public void updateRecipeItemQuantity(Long productId, Long recipeItemId, BigDecimal quantity) {
        requireRecipeProduct(productId);
        BigDecimal cleanQuantity = positiveDecimal4(quantity, "Ingresa una cantidad mayor a cero para la receta.");
        int updated = jdbcTemplate.update("""
                UPDATE restaurant_recipe_item
                SET quantity = ?, updated_at = NOW()
                WHERE id = ? AND product_id = ?
                """, cleanQuantity, recipeItemId, productId);
        if (updated == 0) {
            throw new IllegalArgumentException("El ingrediente de la receta no existe.");
        }
    }

    @Transactional
    public void removeRecipeItem(Long productId, Long recipeItemId) {
        requireRecipeProduct(productId);
        int deleted = jdbcTemplate.update("DELETE FROM restaurant_recipe_item WHERE id = ? AND product_id = ?", recipeItemId, productId);
        if (deleted == 0) {
            throw new IllegalArgumentException("El ingrediente de la receta no existe.");
        }
    }

    public List<RestaurantMenuItemRow> menuItems() {
        return jdbcTemplate.query(menuItemsSql(
                        "p.active = true AND p.restaurant_visible = true AND p.restaurant_available = true",
                        "ORDER BY category_name ASC, p.restaurant_sort_order ASC, p.featured DESC, p.name ASC",
                        ""), menuMapper()).stream()
                .filter(RestaurantMenuItemRow::isAvailableForSale)
                .toList();
    }

    public List<RestaurantMenuAdminRow> menuItemsAdmin() {
        return menuItemsAdmin("ALL");
    }

    public List<RestaurantMenuAdminRow> menuItemsAdmin(String stockFilter) {
        String cleanFilter = stockFilter == null ? "ALL" : stockFilter.trim().toUpperCase();
        List<RestaurantMenuAdminRow> items = jdbcTemplate.query(
                menuItemsAdminSql("", "ORDER BY category_name ASC, p.restaurant_sort_order ASC, p.name ASC"),
                menuAdminMapper()
        );
        return items.stream().filter(item -> switch (cleanFilter) {
            case "AVAILABLE" -> item.canBeSold();
            case "LOW" -> item.active() && item.isLowStock();
            case "OUT" -> item.isOutOfStock() || !item.restaurantAvailable();
            case "HIDDEN" -> !item.active() || !item.restaurantVisible();
            default -> true;
        }).toList();
    }

    public RestaurantStockSummary stockSummary() {
        List<RestaurantMenuAdminRow> items = menuItemsAdmin("ALL");
        int totalItems = items.size();
        int availableItems = (int) items.stream().filter(RestaurantMenuAdminRow::canBeSold).count();
        int lowStockItems = (int) items.stream().filter(item -> item.active() && item.isLowStock()).count();
        int outOfStockItems = (int) items.stream().filter(item -> item.isOutOfStock() || !item.restaurantAvailable()).count();
        int hiddenItems = (int) items.stream().filter(item -> !item.active() || !item.restaurantVisible()).count();
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
                        "p.active = true AND p.restaurant_visible = true AND p.restaurant_available = true AND p.featured = true",
                        "ORDER BY category_name ASC, p.restaurant_sort_order ASC, p.name ASC",
                        ""), menuMapper()).stream()
                .filter(RestaurantMenuItemRow::isAvailableForSale)
                .limit(8)
                .toList();
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
                               String stockControlMode,
                               int sortOrder) {
        String cleanName = requireText(name, "Ingresa el nombre del plato.");
        BigDecimal cleanPrice = money(price);
        BigDecimal cleanStock = nonNegative(stock);
        BigDecimal cleanMinimumStock = nonNegative(minimumStock);
        String cleanStockControlMode = normalize(stockControlMode, VALID_STOCK_CONTROL_MODES, "PRODUCT");
        boolean cleanRestaurantAvailable = restaurantAvailable
                && (!"PRODUCT".equals(cleanStockControlMode) || cleanStock.compareTo(BigDecimal.ZERO) > 0);
        Long resolvedCategoryId = resolveCategoryId(categoryId, newCategoryName);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO product
                    (name, description, image_path, category_id, price, active, featured, stock, minimum_stock,
                     restaurant_visible, restaurant_available, restaurant_sort_order, restaurant_stock_control)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setString(13, cleanStockControlMode);
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
                               String stockControlMode,
                               int sortOrder) {
        String cleanName = requireText(name, "Ingresa el nombre del plato.");
        BigDecimal cleanPrice = money(price);
        BigDecimal cleanStock = nonNegative(stock);
        BigDecimal cleanMinimumStock = nonNegative(minimumStock);
        String cleanStockControlMode = normalize(stockControlMode, VALID_STOCK_CONTROL_MODES, "PRODUCT");
        boolean cleanRestaurantAvailable = restaurantAvailable
                && (!"PRODUCT".equals(cleanStockControlMode) || cleanStock.compareTo(BigDecimal.ZERO) > 0);
        Long resolvedCategoryId = resolveCategoryId(categoryId, newCategoryName);
        int updated = jdbcTemplate.update("""
                UPDATE product
                SET name = ?, description = ?, image_path = ?, category_id = ?, price = ?, active = ?, featured = ?,
                    stock = ?, minimum_stock = ?, restaurant_visible = ?, restaurant_available = ?, restaurant_sort_order = ?,
                    restaurant_stock_control = ?
                WHERE id = ?
                """,
                cleanName, blankToNull(description), blankToNull(imagePath), resolvedCategoryId, cleanPrice, active, featured,
                cleanStock, cleanMinimumStock, restaurantVisible, cleanRestaurantAvailable, Math.max(0, sortOrder),
                cleanStockControlMode, id);
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
        if (!item.hasEffectiveStock()) {
            String message = item.usesRecipeStock()
                    ? "Completa la receta y repón sus ingredientes antes de marcar el plato como disponible."
                    : "Repón stock antes de volver a marcar el plato como disponible.";
            throw new IllegalArgumentException(message);
        }
        jdbcTemplate.update("UPDATE product SET restaurant_available = true WHERE id = ?", id);
    }

    @Transactional
    public void replenishMenuItemStock(Long id, BigDecimal quantity) {
        RestaurantMenuAdminRow item = menuItemAdmin(id);
        if (item == null) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
        if (!item.usesProductStock()) {
            throw new IllegalArgumentException(item.usesRecipeStock()
                    ? "Este plato controla disponibilidad por ingredientes. Repón el ingrediente correspondiente."
                    : "Este plato no utiliza control de stock.");
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
        refreshUpcomingTableStatuses();
        return jdbcTemplate.query("""
                SELECT id, code, name, area, seats, status, active, notes
                FROM restaurant_table
                ORDER BY area ASC, name ASC
                """, tableMapper());
    }

    public List<RestaurantTableBoardRow> tableBoard() {
        refreshUpcomingTableStatuses();
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
                      AND ro.status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
                    ORDER BY ro.created_at DESC, ro.id DESC
                    LIMIT 1
                )
                WHERE rt.active = true
                ORDER BY rt.area ASC, rt.name ASC
                """, tableBoardMapper());
    }

    public List<RestaurantTableRow> availableTables() {
        refreshUpcomingTableStatuses();
        return jdbcTemplate.query("""
                SELECT id, code, name, area, seats, status, active, notes
                FROM restaurant_table
                WHERE active = true
                  AND status IN ('FREE','RESERVED')
                  AND id NOT IN (
                      SELECT table_id
                      FROM restaurant_order
                      WHERE table_id IS NOT NULL
                        AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
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
                           o.customer_name, o.customer_phone, o.status, (o.subtotal + COALESCE(o.delivery_fee, 0)) AS subtotal, o.notes, o.created_at,
                           COALESCE(COUNT(i.id), 0) AS item_count
                    FROM restaurant_order o
                    LEFT JOIN restaurant_table t ON t.id = o.table_id
                    LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                    WHERE o.id = ?
                    GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.delivery_fee, o.notes, o.created_at
                    """, orderMapper(), orderId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<RestaurantOrderRow> activeOrders() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, (o.subtotal + COALESCE(o.delivery_fee, 0)) AS subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
                  AND NOT (o.service_type IN ('TAKEAWAY','DELIVERY') AND o.status = 'DELIVERED' AND o.paid_at IS NOT NULL)
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.delivery_fee, o.notes, o.created_at
                ORDER BY o.created_at DESC
                """, orderMapper());
    }

    public List<RestaurantOrderRow> kitchenOrders() {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_code, o.service_type, o.table_id, t.name AS table_name,
                       o.customer_name, o.customer_phone, o.status, (o.subtotal + COALESCE(o.delivery_fee, 0)) AS subtotal, o.notes, o.created_at,
                       COALESCE(COUNT(i.id), 0) AS item_count
                FROM restaurant_order o
                LEFT JOIN restaurant_table t ON t.id = o.table_id
                LEFT JOIN restaurant_order_item i ON i.order_id = o.id
                WHERE o.status IN ('IN_KITCHEN','READY') OR (o.status = 'NEW' AND o.service_type = 'DINE_IN')
                GROUP BY o.id, o.order_code, o.service_type, o.table_id, t.name, o.customer_name, o.customer_phone, o.status, o.subtotal, o.delivery_fee, o.notes, o.created_at
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
        jdbcTemplate.update("UPDATE restaurant_order SET status = CASE WHEN status IN ('NEW','READY','SERVED') THEN 'IN_KITCHEN' ELSE status END WHERE id = ?", orderId);
    }

    @Transactional
    public void updateItemQuantity(Long orderId, Long itemId, int quantity) {
        assertOrderEditable(orderId);
        int cleanQuantity = Math.max(1, Math.min(quantity, 99));
        RestaurantOrderItemStock item = orderItemForStock(orderId, itemId);
        int delta = cleanQuantity - item.quantity();
        if (delta > 0) {
            switch (item.safeStockControlMode()) {
                case "NONE" -> { }
                case "RECIPE" -> adjustRecipeStock(orderId, itemId, item.productName(), delta);
                default -> reserveProductStock(item.productId(), item.productName(), delta);
            }
        } else if (delta < 0) {
            restoreStockForOrderItem(item, Math.abs(delta));
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
        restoreStockForOrderItem(item, item.quantity());
        int deleted = jdbcTemplate.update("DELETE FROM restaurant_order_item WHERE id = ? AND order_id = ?", itemId, orderId);
        if (deleted == 0) {
            throw new IllegalArgumentException("No se encontró el producto dentro de la comanda.");
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
        RestaurantExternalOrderRow externalOrder = externalOrder(orderId);
        if (externalOrder != null) {
            if (externalOrder.isPaid()) {
                throw new IllegalArgumentException("El pedido externo ya está pagado.");
            }
            if (!externalOrder.canPay()) {
                throw new IllegalArgumentException("El pedido debe estar listo, en reparto o entregado antes de cobrar.");
            }
            recalculateOrderTotal(orderId);
            String cleanPaymentMethod = normalize(paymentMethod, VALID_PAYMENT_METHODS, "CASH");
            jdbcTemplate.update("""
                    UPDATE restaurant_order
                    SET payment_method = ?, paid_at = NOW(), updated_at = NOW()
                    WHERE id = ?
                    """, cleanPaymentMethod, orderId);
            return;
        }

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
        lockOrderStatus(orderId);
        RestaurantOrderRow order = assertOrderExists(orderId);
        RestaurantExternalOrderRow externalOrder = externalOrder(orderId);
        if ("CANCELLED".equals(order.safeStatus())) {
            throw new IllegalArgumentException("La comanda ya está anulada.");
        }
        if ("PAID".equals(order.safeStatus()) || (externalOrder != null && externalOrder.isPaid())) {
            throw new IllegalArgumentException("No se puede anular un pedido pagado desde esta pantalla.");
        }
        if (externalOrder != null && !externalOrder.canCancel()) {
            throw new IllegalArgumentException("El pedido ya salió a reparto o fue entregado y no puede anularse desde esta pantalla.");
        }
        restoreOrderStock(orderId);
        jdbcTemplate.update("UPDATE restaurant_order SET status = 'CANCELLED' WHERE id = ?", orderId);
        releaseTableForOrder(orderId);
    }

    private void insertOrderItems(Long orderId, List<RestaurantMenuItemRow> selectedItems, Map<Long, Integer> quantities) {
        for (RestaurantMenuItemRow item : selectedItems) {
            int quantity = quantities.get(item.id());
            BigDecimal lineTotal = item.safePrice().multiply(BigDecimal.valueOf(quantity));
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO restaurant_order_item
                        (order_id, product_id, product_name, quantity, unit_price, line_total, kitchen_status, stock_control_mode)
                        VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, orderId);
                if (item.id() == null) {
                    ps.setObject(2, null);
                } else {
                    ps.setLong(2, item.id());
                }
                ps.setString(3, item.name());
                ps.setInt(4, quantity);
                ps.setBigDecimal(5, item.safePrice());
                ps.setBigDecimal(6, lineTotal);
                ps.setString(7, item.safeStockControlMode());
                return ps;
            }, keyHolder);
            Long orderItemId = Objects.requireNonNull(keyHolder.getKey()).longValue();
            reserveStockForOrderItem(orderId, orderItemId, item, quantity);
        }
    }

    private void assertSelectedProductsAvailable(List<RestaurantMenuItemRow> selectedItems, Map<Long, Integer> quantities) {
        if (selectedItems.isEmpty() || selectedItems.size() != quantities.size()) {
            throw new IllegalArgumentException("Uno o más platos ya no están disponibles en carta.");
        }
        for (RestaurantMenuItemRow item : selectedItems) {
            int requestedQuantity = quantities.getOrDefault(item.id(), 0);
            if (requestedQuantity <= 0 || item.hasNoStockControl()) {
                continue;
            }
            BigDecimal available = item.effectiveAvailableQuantity();
            if (available.compareTo(BigDecimal.valueOf(requestedQuantity)) < 0) {
                String detail = item.usesRecipeStock()
                        ? " Porciones posibles: " + RestaurantDecimalFormat.quantity(available)
                          + (item.limitingIngredient() == null ? "." : ". Ingrediente limitante: " + item.limitingIngredient() + ".")
                        : " Disponible: " + RestaurantDecimalFormat.quantity(available) + ".";
                throw new IllegalArgumentException("Stock insuficiente para " + item.name() + "." + detail);
            }
        }
    }

    private void reserveStockForOrderItem(Long orderId,
                                          Long orderItemId,
                                          RestaurantMenuItemRow item,
                                          int quantity) {
        switch (item.safeStockControlMode()) {
            case "NONE" -> {
                // This item intentionally does not affect inventory.
            }
            case "RECIPE" -> reserveRecipeStock(orderId, orderItemId, item.id(), item.name(), quantity);
            default -> reserveProductStock(item.id(), item.name(), quantity);
        }
    }

    private void reserveProductStock(Long productId, String productName, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        int updated = jdbcTemplate.update("""
                UPDATE product
                SET stock = COALESCE(stock, 0) - ?
                WHERE id = ?
                  AND active = true
                  AND restaurant_visible = true
                  AND restaurant_available = true
                  AND COALESCE(stock, 0) >= ?
                """, quantity, productId, quantity);
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

    private void reserveRecipeStock(Long orderId,
                                    Long orderItemId,
                                    Long productId,
                                    String productName,
                                    int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        List<RecipeStockRequirement> requirements = recipeStockRequirements(productId, true);
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("El plato " + productName + " no tiene una receta configurada.");
        }
        for (RecipeStockRequirement requirement : requirements) {
            if (!requirement.active() || requirement.quantityPerUnit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La receta de " + productName + " contiene ingredientes inactivos o cantidades inválidas.");
            }
            BigDecimal required = decimal4(requirement.quantityPerUnit().multiply(BigDecimal.valueOf(quantity)));
            if (requirement.currentStock().compareTo(required) < 0) {
                throw new IllegalArgumentException("Ingrediente insuficiente para " + productName + ": "
                        + requirement.ingredientName() + ". Disponible: "
                        + RestaurantDecimalFormat.quantity(requirement.currentStock()) + " "
                        + ingredientUnitAbbreviation(requirement.unitCode()) + ".");
            }
        }

        for (RecipeStockRequirement requirement : requirements) {
            BigDecimal required = decimal4(requirement.quantityPerUnit().multiply(BigDecimal.valueOf(quantity)));
            jdbcTemplate.update("""
                    UPDATE restaurant_ingredient
                    SET stock = stock - ?, updated_at = NOW()
                    WHERE id = ? AND stock >= ?
                    """, required, requirement.ingredientId(), required);
            jdbcTemplate.update("""
                    INSERT INTO restaurant_order_item_ingredient
                    (order_item_id, ingredient_id, ingredient_name, unit_code, quantity_per_unit, quantity_reserved, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                    ON DUPLICATE KEY UPDATE
                        ingredient_name = VALUES(ingredient_name),
                        unit_code = VALUES(unit_code),
                        quantity_per_unit = VALUES(quantity_per_unit),
                        quantity_reserved = quantity_reserved + VALUES(quantity_reserved),
                        updated_at = NOW()
                    """, orderItemId, requirement.ingredientId(), requirement.ingredientName(), requirement.unitCode(),
                    requirement.quantityPerUnit(), required);
            recordIngredientMovement(requirement.ingredientId(), "CONSUMPTION", required.negate(), orderId, orderItemId,
                    "Consumo por " + productName + " x" + quantity);
        }
    }

    private void adjustRecipeStock(Long orderId,
                                   Long orderItemId,
                                   String productName,
                                   int quantityDelta) {
        if (quantityDelta == 0) {
            return;
        }
        List<RestaurantOrderItemIngredientRow> allocations = jdbcTemplate.query("""
                SELECT a.id, a.order_item_id, a.ingredient_id, a.ingredient_name, a.unit_code,
                       a.quantity_per_unit, a.quantity_reserved
                FROM restaurant_order_item_ingredient a
                JOIN restaurant_ingredient i ON i.id = a.ingredient_id
                WHERE a.order_item_id = ?
                ORDER BY a.id
                FOR UPDATE
                """, (rs, rowNum) -> new RestaurantOrderItemIngredientRow(
                rs.getLong("id"),
                rs.getLong("order_item_id"),
                rs.getLong("ingredient_id"),
                rs.getString("ingredient_name"),
                rs.getString("unit_code"),
                rs.getBigDecimal("quantity_per_unit"),
                rs.getBigDecimal("quantity_reserved")
        ), orderItemId);
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No se encontró la reserva de ingredientes del plato " + productName + ".");
        }

        if (quantityDelta > 0) {
            for (RestaurantOrderItemIngredientRow allocation : allocations) {
                BigDecimal required = decimal4(allocation.safeQuantityPerUnit().multiply(BigDecimal.valueOf(quantityDelta)));
                BigDecimal currentStock = lockIngredientStock(allocation.ingredientId());
                if (currentStock.compareTo(required) < 0) {
                    throw new IllegalArgumentException("Ingrediente insuficiente para aumentar " + productName + ": "
                            + allocation.ingredientName() + ". Disponible: "
                            + RestaurantDecimalFormat.quantity(currentStock) + " "
                            + ingredientUnitAbbreviation(allocation.unitCode()) + ".");
                }
            }
            for (RestaurantOrderItemIngredientRow allocation : allocations) {
                BigDecimal required = decimal4(allocation.safeQuantityPerUnit().multiply(BigDecimal.valueOf(quantityDelta)));
                jdbcTemplate.update("UPDATE restaurant_ingredient SET stock = stock - ?, updated_at = NOW() WHERE id = ?",
                        required, allocation.ingredientId());
                jdbcTemplate.update("""
                        UPDATE restaurant_order_item_ingredient
                        SET quantity_reserved = quantity_reserved + ?, updated_at = NOW()
                        WHERE id = ?
                        """, required, allocation.id());
                recordIngredientMovement(allocation.ingredientId(), "CONSUMPTION", required.negate(), orderId, orderItemId,
                        "Aumento de " + productName + " x" + quantityDelta);
            }
            return;
        }

        int unitsToReturn = Math.abs(quantityDelta);
        for (RestaurantOrderItemIngredientRow allocation : allocations) {
            BigDecimal expected = decimal4(allocation.safeQuantityPerUnit().multiply(BigDecimal.valueOf(unitsToReturn)));
            BigDecimal returned = expected.min(allocation.safeQuantityReserved());
            if (returned.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            jdbcTemplate.update("UPDATE restaurant_ingredient SET stock = stock + ?, updated_at = NOW() WHERE id = ?",
                    returned, allocation.ingredientId());
            jdbcTemplate.update("""
                    UPDATE restaurant_order_item_ingredient
                    SET quantity_reserved = GREATEST(quantity_reserved - ?, 0), updated_at = NOW()
                    WHERE id = ?
                    """, returned, allocation.id());
            recordIngredientMovement(allocation.ingredientId(), "RETURN", returned, orderId, orderItemId,
                    "Devolución por reducción de " + productName + " x" + unitsToReturn);
        }
    }

    private void restoreStockForOrderItem(RestaurantOrderItemStock item, int quantity) {
        if (quantity <= 0) {
            return;
        }
        switch (item.safeStockControlMode()) {
            case "NONE" -> {
                // This item intentionally does not affect inventory.
            }
            case "RECIPE" -> adjustRecipeStock(item.orderId(), item.id(), item.productName(), -quantity);
            default -> restoreProductStock(item.productId(), quantity);
        }
    }

    private void restoreOrderStock(Long orderId) {
        List<RestaurantOrderItemStock> items = jdbcTemplate.query("""
                SELECT id, order_id, product_id, product_name, quantity, stock_control_mode
                FROM restaurant_order_item
                WHERE order_id = ?
                FOR UPDATE
                """, orderItemStockMapper(), orderId);
        for (RestaurantOrderItemStock item : items) {
            restoreStockForOrderItem(item, item.quantity());
        }
    }

    private RestaurantOrderItemStock orderItemForStock(Long orderId, Long itemId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, order_id, product_id, product_name, quantity, stock_control_mode
                    FROM restaurant_order_item
                    WHERE id = ? AND order_id = ?
                    FOR UPDATE
                    """, orderItemStockMapper(), itemId, orderId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("No se encontró el producto dentro de la comanda.");
        }
    }

    private List<RecipeStockRequirement> recipeStockRequirements(Long productId, boolean lockRows) {
        String lockClause = lockRows ? " FOR UPDATE" : "";
        return jdbcTemplate.query("""
                SELECT i.id AS ingredient_id, i.name AS ingredient_name, i.unit_code,
                       r.quantity AS quantity_per_unit, i.stock AS current_stock, i.active
                FROM restaurant_recipe_item r
                JOIN restaurant_ingredient i ON i.id = r.ingredient_id
                WHERE r.product_id = ?
                ORDER BY i.id
                """ + lockClause, (rs, rowNum) -> new RecipeStockRequirement(
                rs.getLong("ingredient_id"),
                rs.getString("ingredient_name"),
                rs.getString("unit_code"),
                rs.getBigDecimal("quantity_per_unit"),
                rs.getBigDecimal("current_stock"),
                rs.getBoolean("active")
        ), productId);
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
            refreshTableReservationStatus(tableId);
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
                WHERE table_id = ? AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
                """, Integer.class, tableId);
        if (activeOrders != null && activeOrders > 0) {
            throw new IllegalArgumentException("La mesa seleccionada ya tiene una comanda activa.");
        }
    }

    private void validateReservation(Long tableId,
                                     LocalDateTime reservationAt,
                                     int durationMinutes,
                                     int partySize,
                                     String status,
                                     Long excludeReservationId) {
        if (tableId == null) {
            throw new IllegalArgumentException("Selecciona una mesa para la reserva.");
        }
        RestaurantTableRow table = reservationTable(tableId);
        if (table == null || !table.active()) {
            throw new IllegalArgumentException("La mesa seleccionada no existe o está inactiva.");
        }
        if ("DISABLED".equalsIgnoreCase(table.status())) {
            throw new IllegalArgumentException("La mesa seleccionada está fuera de servicio.");
        }
        if (reservationAt == null) {
            throw new IllegalArgumentException("Selecciona la fecha y hora de la reserva.");
        }
        if (ACTIVE_RESERVATION_STATUSES.contains(status)
                && reservationAt.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("La fecha y hora de la reserva no puede estar en el pasado.");
        }
        if (durationMinutes < 30 || durationMinutes > 480) {
            throw new IllegalArgumentException("La duración debe estar entre 30 y 480 minutos.");
        }
        if (partySize < 1 || partySize > 100) {
            throw new IllegalArgumentException("La cantidad de personas debe estar entre 1 y 100.");
        }
        if (table.seats() > 0 && partySize > table.seats()) {
            throw new IllegalArgumentException("La mesa seleccionada tiene capacidad para " + table.seats() + " personas.");
        }
        if (ACTIVE_RESERVATION_STATUSES.contains(status)) {
            assertNoReservationOverlap(tableId, reservationAt, durationMinutes, excludeReservationId);
        }
    }

    private void assertNoReservationOverlap(Long tableId,
                                            LocalDateTime reservationAt,
                                            int durationMinutes,
                                            Long excludeReservationId) {
        LocalDateTime reservationEnd = reservationAt.plusMinutes(durationMinutes);
        String exclusion = excludeReservationId == null ? "" : "AND id <> ?";
        String sql = """
                SELECT COUNT(*)
                FROM restaurant_reservation
                WHERE table_id = ?
                  AND status IN ('PENDING','CONFIRMED')
                  %s
                  AND reservation_at < ?
                  AND DATE_ADD(reservation_at, INTERVAL duration_minutes MINUTE) > ?
                """.formatted(exclusion);
        int conflicts = excludeReservationId == null
                ? count(sql, tableId, Timestamp.valueOf(reservationEnd), Timestamp.valueOf(reservationAt))
                : count(sql, tableId, excludeReservationId, Timestamp.valueOf(reservationEnd), Timestamp.valueOf(reservationAt));
        if (conflicts > 0) {
            throw new IllegalArgumentException("La mesa ya tiene una reserva que cruza ese horario.");
        }
    }

    private RestaurantTableRow reservationTable(Long tableId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, code, name, area, seats, status, active, notes
                    FROM restaurant_table
                    WHERE id = ?
                    """, tableMapper(), tableId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private RestaurantReservationRow assertReservationExists(Long reservationId) {
        RestaurantReservationRow reservation = reservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("La reserva seleccionada no existe.");
        }
        return reservation;
    }

    private void refreshUpcomingTableStatuses() {
        if (!tableExists("restaurant_reservation")) {
            return;
        }
        List<Long> tableIds = jdbcTemplate.query(
                "SELECT id FROM restaurant_table WHERE active = true AND status <> 'DISABLED'",
                (rs, rowNum) -> rs.getLong("id")
        );
        for (Long tableId : tableIds) {
            refreshTableReservationStatus(tableId);
        }
    }

    private void refreshTableReservationStatus(Long tableId) {
        if (tableId == null || !tableExists("restaurant_reservation")) {
            return;
        }
        String currentStatus = jdbcTemplate.query(
                "SELECT status FROM restaurant_table WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                tableId
        );
        if (currentStatus == null || "DISABLED".equalsIgnoreCase(currentStatus)) {
            return;
        }
        if (count("""
                SELECT COUNT(*)
                FROM restaurant_order
                WHERE table_id = ? AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
                """, tableId) > 0) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'OCCUPIED' WHERE id = ?", tableId);
            return;
        }
        int nearReservations = count("""
                SELECT COUNT(*)
                FROM restaurant_reservation
                WHERE table_id = ?
                  AND status = 'CONFIRMED'
                  AND reservation_at <= DATE_ADD(NOW(), INTERVAL 120 MINUTE)
                  AND DATE_ADD(reservation_at, INTERVAL duration_minutes MINUTE) > NOW()
                """, tableId);
        if (nearReservations > 0) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'RESERVED' WHERE id = ?", tableId);
        } else if ("RESERVED".equalsIgnoreCase(currentStatus)) {
            jdbcTemplate.update("UPDATE restaurant_table SET status = 'FREE' WHERE id = ?", tableId);
        }
    }

    private String normalizeExternalOrderFilter(String value) {
        String clean = value == null ? "ACTIVE" : value.trim().toUpperCase();
        return switch (clean) {
            case "ALL", "ACTIVE", "NEW", "CONFIRMED", "IN_KITCHEN", "READY",
                    "OUT_FOR_DELIVERY", "DELIVERED", "PAID", "CANCELLED" -> clean;
            default -> "ACTIVE";
        };
    }

    private String normalizeReservationFilter(String value) {
        String clean = value == null ? "ALL" : value.trim().toUpperCase();
        return switch (clean) {
            case "ACTIVE", "PENDING", "CONFIRMED", "ATTENDED", "CANCELLED", "NO_SHOW", "ALL" -> clean;
            default -> "ALL";
        };
    }

    private String nextReservationCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RSV-" + timestamp + "-" + suffix;
    }

    private RestaurantOrderRow assertOrderExists(Long orderId) {
        RestaurantOrderRow order = order(orderId);
        if (order == null) {
            throw new IllegalArgumentException("La comanda solicitada no existe.");
        }
        return order;
    }

    private void assertOrderEditable(Long orderId) {
        String status = lockOrderStatus(orderId);
        if (CLOSED_ORDER_STATUSES.contains(status)) {
            throw new IllegalArgumentException("No se puede editar una comanda pagada o anulada.");
        }
    }

    private String lockOrderStatus(Long orderId) {
        try {
            String status = jdbcTemplate.queryForObject("""
                    SELECT status
                    FROM restaurant_order
                    WHERE id = ?
                    FOR UPDATE
                    """, String.class, orderId);
            return status == null ? "NEW" : status.trim().toUpperCase();
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("La comanda seleccionada no existe.");
        }
    }

    private Map<String, String> qrOrderQuantityParams(List<RestaurantQrOrderItemRow> items) {
        Map<String, String> params = new LinkedHashMap<>();
        for (RestaurantQrOrderItemRow item : items) {
            if (item.productId() != null && item.quantity() > 0) {
                String key = "qty_" + item.productId();
                int current = Integer.parseInt(params.getOrDefault(key, "0"));
                params.put(key, Integer.toString(current + item.quantity()));
            }
        }
        return params;
    }

    private Long activeOrderIdForTable(Long tableId) {
        if (tableId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT id
                FROM restaurant_order
                WHERE table_id = ?
                  AND status IN ('NEW','CONFIRMED','IN_KITCHEN','READY','OUT_FOR_DELIVERY','DELIVERED','SERVED')
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, tableId);
    }

    private String qrOrderNote(RestaurantQrOrderRow qrOrder) {
        String note = qrOrder == null ? null : qrOrder.customerNote();
        return note == null || note.isBlank() ? "Pedido enviado desde QR." : "Pedido enviado desde QR. Nota: " + note.trim();
    }

    private void appendQrOrderNote(Long orderId, String customerNote) {
        String cleanNote = limitText(customerNote, 500);
        if (cleanNote.isBlank()) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE restaurant_order
                SET notes = CASE
                    WHEN notes IS NULL OR notes = '' THEN ?
                    ELSE CONCAT(notes, '\n', ?)
                END
                WHERE id = ?
                """, "Pedido QR: " + cleanNote, "Pedido QR: " + cleanNote, orderId);
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
        boolean hasRecipeTables = tableExists("restaurant_recipe_item") && tableExists("restaurant_ingredient");
        String categoryColumns = hasCategoryTable
                ? "p.category_id, COALESCE(c.name, 'Carta general') AS category_name"
                : "NULL AS category_id, 'Carta general' AS category_name";
        String categoryJoin = hasCategoryTable ? "LEFT JOIN category c ON c.id = p.category_id" : "";
        String recipeColumns = hasRecipeTables
                ? "COALESCE((SELECT COUNT(*) FROM restaurant_recipe_item r WHERE r.product_id = p.id), 0) AS recipe_item_count, "
                  + "COALESCE((SELECT COUNT(*) FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND (i.active = false OR r.quantity <= 0)), 0) AS recipe_issue_count, "
                  + "COALESCE((SELECT FLOOR(MIN(i.stock / NULLIF(r.quantity, 0))) FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND r.quantity > 0), 0) AS available_portions, "
                  + "(SELECT i.name FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND r.quantity > 0 ORDER BY (i.stock / r.quantity) ASC, i.name ASC LIMIT 1) AS limiting_ingredient"
                : "0 AS recipe_item_count, 0 AS recipe_issue_count, 0 AS available_portions, NULL AS limiting_ingredient";
        return """
                SELECT p.id, p.name, p.description, p.image_path, p.price, p.featured, p.stock,
                       %s, p.restaurant_visible, p.restaurant_available, p.restaurant_sort_order,
                       COALESCE(p.restaurant_stock_control, 'PRODUCT') AS restaurant_stock_control,
                       %s
                FROM product p
                %s
                WHERE %s
                %s
                %s
                """.formatted(categoryColumns, recipeColumns, categoryJoin, whereClause, orderClause, limitClause);
    }

    private String menuItemsAdminSql(String whereClause, String orderClause) {
        boolean hasCategoryTable = tableExists("category");
        boolean hasRecipeTables = tableExists("restaurant_recipe_item") && tableExists("restaurant_ingredient");
        String categoryColumns = hasCategoryTable
                ? "p.category_id, COALESCE(c.name, 'Carta general') AS category_name"
                : "NULL AS category_id, 'Carta general' AS category_name";
        String categoryJoin = hasCategoryTable ? "LEFT JOIN category c ON c.id = p.category_id" : "";
        String recipeColumns = hasRecipeTables
                ? "COALESCE((SELECT SUM(r.quantity * i.unit_cost) FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id), 0) AS recipe_cost, "
                  + "COALESCE((SELECT COUNT(*) FROM restaurant_recipe_item r WHERE r.product_id = p.id), 0) AS recipe_item_count, "
                  + "COALESCE((SELECT COUNT(*) FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND (i.active = false OR r.quantity <= 0)), 0) AS recipe_issue_count, "
                  + "COALESCE((SELECT FLOOR(MIN(i.stock / NULLIF(r.quantity, 0))) FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND r.quantity > 0), 0) AS available_portions, "
                  + "(SELECT i.name FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND r.quantity > 0 ORDER BY (i.stock / r.quantity) ASC, i.name ASC LIMIT 1) AS limiting_ingredient, "
                  + "COALESCE((SELECT COUNT(*) FROM restaurant_recipe_item r JOIN restaurant_ingredient i ON i.id = r.ingredient_id WHERE r.product_id = p.id AND i.active = true AND i.stock > 0 AND i.minimum_stock > 0 AND i.stock <= i.minimum_stock), 0) AS recipe_low_ingredient_count"
                : "0.0000 AS recipe_cost, 0 AS recipe_item_count, 0 AS recipe_issue_count, 0 AS available_portions, NULL AS limiting_ingredient, 0 AS recipe_low_ingredient_count";
        return """
                SELECT p.id, p.name, p.description, p.image_path, p.price, p.active, p.featured, p.stock, p.minimum_stock,
                       %s, p.restaurant_visible, p.restaurant_available, p.restaurant_sort_order,
                       COALESCE(p.restaurant_stock_control, 'PRODUCT') AS restaurant_stock_control, %s
                FROM product p
                %s
                %s
                %s
                """.formatted(categoryColumns, recipeColumns, categoryJoin,
                whereClause == null ? "" : whereClause, orderClause == null ? "" : orderClause);
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

    private BigDecimal lockIngredientStock(Long ingredientId) {
        try {
            BigDecimal value = jdbcTemplate.queryForObject("""
                    SELECT stock
                    FROM restaurant_ingredient
                    WHERE id = ?
                    FOR UPDATE
                    """, BigDecimal.class, ingredientId);
            return value == null ? BigDecimal.ZERO : value;
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("El ingrediente seleccionado no existe.");
        }
    }

    private void recordIngredientMovement(Long ingredientId,
                                          String movementType,
                                          BigDecimal quantityChange,
                                          Long orderId,
                                          Long orderItemId,
                                          String notes) {
        if (!tableExists("restaurant_ingredient_movement")) {
            return;
        }
        BigDecimal balanceAfter = jdbcTemplate.queryForObject(
                "SELECT stock FROM restaurant_ingredient WHERE id = ?",
                BigDecimal.class,
                ingredientId
        );
        jdbcTemplate.update("""
                INSERT INTO restaurant_ingredient_movement
                (ingredient_id, movement_type, quantity_change, balance_after, order_id, order_item_id, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """, ingredientId,
                movementType == null ? "ADJUSTMENT" : movementType,
                signedDecimal4(quantityChange),
                balanceAfter == null ? BigDecimal.ZERO : decimal4(balanceAfter),
                orderId,
                orderItemId,
                limitText(notes, 500));
    }

    private String ingredientUnitAbbreviation(String unitCode) {
        return switch (unitCode == null ? "UNIT" : unitCode.toUpperCase()) {
            case "KG" -> "kg";
            case "G" -> "g";
            case "L" -> "L";
            case "ML" -> "ml";
            case "PORTION" -> "porción";
            default -> "unid.";
        };
    }

    private void ensureIngredientNameAvailable(String name, Long ignoredId) {
        int duplicates;
        if (ignoredId == null) {
            duplicates = count("SELECT COUNT(*) FROM restaurant_ingredient WHERE LOWER(name) = LOWER(?)", name);
        } else {
            duplicates = count("SELECT COUNT(*) FROM restaurant_ingredient WHERE LOWER(name) = LOWER(?) AND id <> ?", name, ignoredId);
        }
        if (duplicates > 0) {
            throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre.");
        }
    }

    private void requireRecipeProduct(Long productId) {
        if (productId == null || count("SELECT COUNT(*) FROM product WHERE id = ?", productId) == 0) {
            throw new IllegalArgumentException("El plato seleccionado no existe.");
        }
    }

    private BigDecimal signedDecimal4(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal4(BigDecimal value) {
        BigDecimal clean = value == null ? BigDecimal.ZERO : value;
        if (clean.compareTo(BigDecimal.ZERO) < 0) {
            clean = BigDecimal.ZERO;
        }
        return clean.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveDecimal4(BigDecimal value, String message) {
        BigDecimal clean = decimal4(value);
        if (clean.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return clean;
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
                SELECT COALESCE(SUM(subtotal + COALESCE(delivery_fee, 0)), 0)
                FROM restaurant_order
                WHERE paid_at IS NOT NULL
                  AND DATE(paid_at) = ?
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

    private String limitText(String value, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength);
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
                rs.getInt("restaurant_sort_order"),
                rs.getString("restaurant_stock_control"),
                rs.getInt("recipe_item_count"),
                rs.getInt("recipe_issue_count"),
                rs.getBigDecimal("available_portions"),
                rs.getString("limiting_ingredient")
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
                rs.getInt("restaurant_sort_order"),
                rs.getBigDecimal("recipe_cost"),
                rs.getInt("recipe_item_count"),
                rs.getInt("recipe_issue_count"),
                rs.getString("restaurant_stock_control"),
                rs.getBigDecimal("available_portions"),
                rs.getString("limiting_ingredient"),
                rs.getInt("recipe_low_ingredient_count")
        );
    }

    private RowMapper<RestaurantIngredientRow> ingredientMapper() {
        return (rs, rowNum) -> new RestaurantIngredientRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("unit_code"),
                rs.getBigDecimal("unit_cost"),
                rs.getBigDecimal("stock"),
                rs.getBigDecimal("minimum_stock"),
                rs.getBoolean("active"),
                rs.getString("notes")
        );
    }

    private RowMapper<RestaurantIngredientMovementRow> ingredientMovementMapper() {
        return (rs, rowNum) -> new RestaurantIngredientMovementRow(
                rs.getLong("id"),
                rs.getLong("ingredient_id"),
                rs.getString("ingredient_name"),
                rs.getString("unit_code"),
                rs.getString("movement_type"),
                rs.getBigDecimal("quantity_change"),
                rs.getBigDecimal("balance_after"),
                nullableLong(rs, "order_id"),
                nullableLong(rs, "order_item_id"),
                rs.getString("notes"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private RowMapper<RestaurantRecipeItemRow> recipeItemMapper() {
        return (rs, rowNum) -> new RestaurantRecipeItemRow(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getLong("ingredient_id"),
                rs.getString("ingredient_name"),
                rs.getString("unit_code"),
                rs.getBigDecimal("unit_cost"),
                rs.getBigDecimal("quantity"),
                rs.getBoolean("ingredient_active")
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


    private RowMapper<RestaurantReservationRow> reservationMapper() {
        return (rs, rowNum) -> new RestaurantReservationRow(
                rs.getLong("id"),
                rs.getString("reservation_code"),
                rs.getLong("table_id"),
                rs.getString("table_name"),
                rs.getString("table_area"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                toLocalDateTime(rs.getTimestamp("reservation_at")),
                rs.getInt("duration_minutes"),
                rs.getInt("party_size"),
                rs.getString("status"),
                rs.getString("notes"),
                nullableLong(rs, "order_id"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<RestaurantTableRequestRow> tableRequestMapper() {
        return (rs, rowNum) -> new RestaurantTableRequestRow(
                rs.getLong("id"),
                rs.getLong("table_id"),
                rs.getString("table_name"),
                rs.getString("table_area"),
                rs.getString("request_type"),
                rs.getString("customer_note"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("resolved_at"))
        );
    }

    private RowMapper<RestaurantQrOrderRow> qrOrderMapper() {
        return (rs, rowNum) -> new RestaurantQrOrderRow(
                rs.getLong("id"),
                rs.getLong("table_id"),
                rs.getString("table_name"),
                rs.getString("table_area"),
                rs.getString("customer_note"),
                rs.getString("status"),
                rs.getBigDecimal("subtotal"),
                nullableLong(rs, "approved_order_id"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("processed_at")),
                rs.getInt("item_count")
        );
    }

    private RowMapper<RestaurantQrOrderItemRow> qrOrderItemMapper() {
        return (rs, rowNum) -> new RestaurantQrOrderItemRow(
                rs.getLong("id"),
                rs.getLong("qr_order_id"),
                nullableLong(rs, "product_id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("line_total")
        );
    }

    private RowMapper<RestaurantExternalOrderRow> externalOrderMapper() {
        return (rs, rowNum) -> new RestaurantExternalOrderRow(
                rs.getLong("id"),
                rs.getString("order_code"),
                rs.getString("service_type"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("delivery_address"),
                rs.getString("delivery_reference"),
                toLocalDateTime(rs.getTimestamp("scheduled_at")),
                rs.getBigDecimal("delivery_fee"),
                rs.getString("status"),
                rs.getBigDecimal("subtotal"),
                rs.getString("payment_method"),
                toLocalDateTime(rs.getTimestamp("paid_at")),
                rs.getString("notes"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                rs.getInt("item_count")
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
                rs.getInt("quantity"),
                rs.getString("stock_control_mode")
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
