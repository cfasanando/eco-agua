package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationAlertRepository {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26OperationAlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Matrix26OperationAlert> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_operation_alert WHERE id = ?", alertMapper(), id)
                .stream()
                .findFirst();
    }

    public Optional<Matrix26OperationAlert> findBySourceKey(String sourceKey) {
        return jdbcTemplate.query("SELECT * FROM matrix26_operation_alert WHERE source_key = ?", alertMapper(), sourceKey)
                .stream()
                .findFirst();
    }

    public List<Matrix26OperationAlert> findAlerts(
            boolean includeClosed,
            Matrix26OperationAlertStatus status,
            Matrix26OperationAlertSeverity severity,
            Matrix26OperationAlertSource source
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM matrix26_operation_alert WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if (!includeClosed) {
            sql.append(" AND status IN ('OPEN','ACKNOWLEDGED')");
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        if (severity != null) {
            sql.append(" AND severity = ?");
            args.add(severity.name());
        }
        if (source != null) {
            sql.append(" AND source = ?");
            args.add(source.name());
        }
        sql.append(" ORDER BY FIELD(status, 'OPEN','ACKNOWLEDGED','RESOLVED','IGNORED'), FIELD(severity, 'CRITICAL','HIGH','MEDIUM','LOW','INFO'), last_detected_at DESC, id DESC LIMIT 300");
        return jdbcTemplate.query(sql.toString(), alertMapper(), args.toArray());
    }

    public Matrix26OperationAlertSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(*) AS total,
                    SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) AS open_count,
                    SUM(CASE WHEN status = 'ACKNOWLEDGED' THEN 1 ELSE 0 END) AS acknowledged_count,
                    SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved_count,
                    SUM(CASE WHEN status = 'IGNORED' THEN 1 ELSE 0 END) AS ignored_count,
                    SUM(CASE WHEN severity = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_count,
                    SUM(CASE WHEN severity = 'HIGH' THEN 1 ELSE 0 END) AS high_count,
                    SUM(CASE WHEN severity = 'MEDIUM' THEN 1 ELSE 0 END) AS medium_count,
                    SUM(CASE WHEN severity = 'LOW' THEN 1 ELSE 0 END) AS low_count,
                    SUM(CASE WHEN severity = 'INFO' THEN 1 ELSE 0 END) AS info_count
                FROM matrix26_operation_alert
                """, (rs, rowNum) -> new Matrix26OperationAlertSummary(
                rs.getLong("total"),
                rs.getLong("open_count"),
                rs.getLong("acknowledged_count"),
                rs.getLong("resolved_count"),
                rs.getLong("ignored_count"),
                rs.getLong("critical_count"),
                rs.getLong("high_count"),
                rs.getLong("medium_count"),
                rs.getLong("low_count"),
                rs.getLong("info_count")
        ));
    }

    public long insertDetectedAlert(
            String sourceKey,
            Matrix26OperationAlertSource source,
            Matrix26OperationAlertSeverity severity,
            String instanceCode,
            String title,
            String message,
            String href,
            String actionLabel
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO matrix26_operation_alert (
                        source_key, source, severity, status, instance_code, title, message, href, action_label,
                        first_detected_at, last_detected_at, detect_count
                    ) VALUES (?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, limit(sourceKey, 180));
            statement.setString(2, source.name());
            statement.setString(3, severity.name());
            statement.setString(4, limit(instanceCode, 80));
            statement.setString(5, limit(title, 240));
            statement.setString(6, limit(message, 8000));
            statement.setString(7, limit(href, 1000));
            statement.setString(8, limit(actionLabel, 120));
            statement.setObject(9, now);
            statement.setObject(10, now);
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return id == null ? 0L : id.longValue();
    }

    public void markDetected(
            long alertId,
            Matrix26OperationAlertSeverity severity,
            String title,
            String message,
            String href,
            String actionLabel
    ) {
        jdbcTemplate.update("""
                UPDATE matrix26_operation_alert
                SET severity = ?, title = ?, message = ?, href = ?, action_label = ?,
                    resolved_at = CASE WHEN status IN ('RESOLVED','IGNORED') THEN NULL ELSE resolved_at END,
                    resolved_by = CASE WHEN status IN ('RESOLVED','IGNORED') THEN NULL ELSE resolved_by END,
                    ignored_at = CASE WHEN status IN ('RESOLVED','IGNORED') THEN NULL ELSE ignored_at END,
                    ignored_by = CASE WHEN status IN ('RESOLVED','IGNORED') THEN NULL ELSE ignored_by END,
                    status = CASE WHEN status IN ('RESOLVED','IGNORED') THEN 'OPEN' ELSE status END,
                    last_detected_at = ?, detect_count = detect_count + 1
                WHERE id = ?
                """, severity.name(), limit(title, 240), limit(message, 8000), limit(href, 1000), limit(actionLabel, 120), LocalDateTime.now(), alertId);
    }

    public void acknowledge(long alertId, String actor, String note) {
        jdbcTemplate.update("""
                UPDATE matrix26_operation_alert
                SET status = 'ACKNOWLEDGED', acknowledged_at = ?, acknowledged_by = ?, resolution_notes = ?
                WHERE id = ? AND status = 'OPEN'
                """, LocalDateTime.now(), limit(actor, 120), limit(note, 4000), alertId);
    }

    public void resolve(long alertId, String actor, String note) {
        jdbcTemplate.update("""
                UPDATE matrix26_operation_alert
                SET status = 'RESOLVED', resolved_at = ?, resolved_by = ?, resolution_notes = ?
                WHERE id = ? AND status IN ('OPEN','ACKNOWLEDGED')
                """, LocalDateTime.now(), limit(actor, 120), limit(note, 4000), alertId);
    }

    public void ignore(long alertId, String actor, String note) {
        jdbcTemplate.update("""
                UPDATE matrix26_operation_alert
                SET status = 'IGNORED', ignored_at = ?, ignored_by = ?, resolution_notes = ?
                WHERE id = ? AND status IN ('OPEN','ACKNOWLEDGED')
                """, LocalDateTime.now(), limit(actor, 120), limit(note, 4000), alertId);
    }

    public void reopen(long alertId, String actor, String note) {
        jdbcTemplate.update("""
                UPDATE matrix26_operation_alert
                SET status = 'OPEN', resolved_at = NULL, resolved_by = NULL, ignored_at = NULL, ignored_by = NULL,
                    resolution_notes = ?, last_detected_at = ?
                WHERE id = ?
                """, limit(note, 4000), LocalDateTime.now(), alertId);
    }

    public void addEvent(long alertId, String eventType, Matrix26OperationAlertStatus status, String actor, String note) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_operation_alert_event (
                    alert_id, event_type, status, actor, note, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, alertId, limit(eventType, 80), status.name(), limit(actor, 120), limit(note, 4000), LocalDateTime.now());
    }

    public List<Matrix26OperationAlertEvent> events(long alertId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_operation_alert_event
                WHERE alert_id = ?
                ORDER BY created_at DESC, id DESC
                """, eventMapper(), alertId);
    }

    private RowMapper<Matrix26OperationAlert> alertMapper() {
        return (rs, rowNum) -> new Matrix26OperationAlert(
                rs.getLong("id"),
                rs.getString("source_key"),
                Matrix26OperationAlertSource.valueOf(rs.getString("source")),
                Matrix26OperationAlertSeverity.valueOf(rs.getString("severity")),
                Matrix26OperationAlertStatus.valueOf(rs.getString("status")),
                rs.getString("instance_code"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("href"),
                rs.getString("action_label"),
                rs.getObject("first_detected_at", LocalDateTime.class),
                rs.getObject("last_detected_at", LocalDateTime.class),
                rs.getObject("detect_count", Integer.class),
                rs.getObject("acknowledged_at", LocalDateTime.class),
                rs.getString("acknowledged_by"),
                rs.getObject("resolved_at", LocalDateTime.class),
                rs.getString("resolved_by"),
                rs.getObject("ignored_at", LocalDateTime.class),
                rs.getString("ignored_by"),
                rs.getString("resolution_notes")
        );
    }

    private RowMapper<Matrix26OperationAlertEvent> eventMapper() {
        return (rs, rowNum) -> new Matrix26OperationAlertEvent(
                rs.getLong("id"),
                rs.getLong("alert_id"),
                rs.getString("event_type"),
                Matrix26OperationAlertStatus.valueOf(rs.getString("status")),
                rs.getString("actor"),
                rs.getString("note"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }
}
