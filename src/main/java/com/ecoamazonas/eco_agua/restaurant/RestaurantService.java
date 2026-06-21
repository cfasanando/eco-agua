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
    private static final List<String> VALID_ORDER_STATUSES = List.of("NEW", "IN_KITCHEN", "READY", "SERVED", "PAID", "CANCELLED");
    private static final List<String> VALID_SERVICE_TYPES = List.of("DINE_IN", "TAKEAWAY", "DELIVERY");
    private static final List<String> VALID_PAYMENT_METHODS = List.of("CASH", "CARD", "YAPE", "PLIN", "TRANSFER", "OTHER");
    private static final List<String> VALID_TABLE_REQUEST_TYPES = List.of("ATTENTION", "BILL", "PAID_NOTICE", "WAITER", "NOTE");
    private static final List<String> VALID_TABLE_REQUEST_STATUSES = List.of("PENDING", "RESOLVED");
    private static final List<String> VALID_QR_ORDER_STATUSES = List.of("PENDING", "APPROVED", "REJECTED");
    private static final List<String> VALID_RESERVATION_STATUSES = List.of("PENDING", "CONFIRMED", "ATTENDED", "CANCELLED", "NO_SHOW");
    private static final List<String> ACTIVE_RESERVATION_STATUSES = List.of("PENDING", "CONFIRMED");
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
                      AND ro.status IN ('NEW','IN_KITCHEN','READY','SERVED')
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
        jdbcTemplate.update("UPDATE restaurant_order SET status = CASE WHEN status IN ('NEW','READY','SERVED') THEN 'IN_KITCHEN' ELSE status END WHERE id = ?", orderId);
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
                WHERE table_id = ? AND status IN ('NEW','IN_KITCHEN','READY','SERVED')
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
                WHERE table_id = ? AND status IN ('NEW','IN_KITCHEN','READY','SERVED')
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
        RestaurantOrderRow order = assertOrderExists(orderId);
        if (CLOSED_ORDER_STATUSES.contains(order.safeStatus())) {
            throw new IllegalArgumentException("No se puede editar una comanda pagada o anulada.");
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
                  AND status IN ('NEW','IN_KITCHEN','READY','SERVED')
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
