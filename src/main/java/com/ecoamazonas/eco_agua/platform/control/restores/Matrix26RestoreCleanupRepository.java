package com.ecoamazonas.eco_agua.platform.control.restores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreCleanupRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26RestoreCleanupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertPlan(
            String publicId,
            long restoreJobId,
            Matrix26RestoreCleanupStatus status,
            String fingerprint,
            String signature,
            String actor,
            String summary
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO matrix26_restore_cleanup_plan (
                        public_id, restore_job_id, status, snapshot_fingerprint, plan_signature,
                        requested_by, requested_at, summary
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, publicId);
            statement.setLong(2, restoreJobId);
            statement.setString(3, status.name());
            statement.setString(4, fingerprint);
            statement.setString(5, signature);
            statement.setString(6, actor);
            statement.setObject(7, LocalDateTime.now());
            statement.setString(8, limit(summary, 4000));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("The cleanup plan could not be created.");
        return key.longValue();
    }

    public void insertItem(long planId, Matrix26RestoreCleanupPlanItem item) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_restore_cleanup_item (
                    cleanup_plan_id, sequence_number, resource_type, location,
                    existed_at_preview, ownership, planned_action, confirmation_group,
                    status, detail
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, planId, item.sequenceNumber(), item.resourceType(), item.location(),
                item.existedAtPreview(), item.ownership(), item.plannedAction(), item.confirmationGroup(),
                item.status().name(), limit(item.detail(), 4000));
    }

    public Optional<Matrix26RestoreCleanupPlan> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_restore_cleanup_plan WHERE id = ?", planMapper(), id)
                .stream().findFirst();
    }

    public Optional<Matrix26RestoreCleanupPlan> findLatestForJob(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_restore_cleanup_plan
                WHERE restore_job_id = ?
                ORDER BY requested_at DESC, id DESC
                LIMIT 1
                """, planMapper(), jobId).stream().findFirst();
    }

    public List<Matrix26RestoreCleanupPlanItem> findItems(long planId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_restore_cleanup_item
                WHERE cleanup_plan_id = ?
                ORDER BY sequence_number, id
                """, itemMapper(), planId);
    }

    public void cancelOpenPlans(long jobId, String reason) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_plan
                SET status = 'CANCELLED', completed_at = ?, last_error = ?
                WHERE restore_job_id = ?
                  AND status IN ('PREVIEW_READY','BLOCKED','APPROVED')
                """, LocalDateTime.now(), limit(reason, 4000), jobId);
    }

    public void approve(long planId, String actor) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_plan
                SET status = 'APPROVED', approved_by = ?, approved_at = ?, last_error = NULL
                WHERE id = ? AND status = 'PREVIEW_READY'
                """, actor, LocalDateTime.now(), planId);
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_item
                SET status = CASE WHEN planned_action IN ('KEEP','SKIP') THEN 'SKIPPED' ELSE 'APPROVED' END
                WHERE cleanup_plan_id = ? AND status = 'PENDING'
                """, planId);
    }

    public void start(long planId) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_plan
                SET status = 'RUNNING', started_at = COALESCE(started_at, ?), completed_at = NULL, last_error = NULL
                WHERE id = ?
                """, LocalDateTime.now(), planId);
    }

    public void updatePlanStatus(long planId, Matrix26RestoreCleanupStatus status, String summary, String error) {
        LocalDateTime completed = status == Matrix26RestoreCleanupStatus.CLEANED
                || status == Matrix26RestoreCleanupStatus.PARTIALLY_CLEANED
                || status == Matrix26RestoreCleanupStatus.FAILED ? LocalDateTime.now() : null;
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_plan
                SET status = ?, summary = ?, last_error = ?, completed_at = ?
                WHERE id = ?
                """, status.name(), limit(summary, 4000), limit(error, 4000), completed, planId);
    }

    public void startItem(long itemId) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_item
                SET status = 'RUNNING', started_at = COALESCE(started_at, ?), completed_at = NULL, last_error = NULL
                WHERE id = ?
                """, LocalDateTime.now(), itemId);
    }

    public void completeItem(long itemId, Matrix26RestoreCleanupItemStatus status, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_item
                SET status = ?, detail = ?, completed_at = ?, last_error = NULL
                WHERE id = ?
                """, status.name(), limit(detail, 4000), LocalDateTime.now(), itemId);
    }

    public void failItem(long itemId, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_item
                SET status = 'FAILED', last_error = ?, completed_at = ?
                WHERE id = ?
                """, limit(error, 4000), LocalDateTime.now(), itemId);
    }

    public void insertEvent(long planId, Long itemId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_restore_cleanup_event (
                    cleanup_plan_id, cleanup_item_id, event_type, status,
                    actor_username, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, planId, itemId, eventType, status, actor, limit(detail, 4000), LocalDateTime.now());
    }

    public boolean hasRunningPlan() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM matrix26_restore_cleanup_plan
                WHERE status = 'RUNNING'
                """, Integer.class);
        return count != null && count > 0;
    }

    private RowMapper<Matrix26RestoreCleanupPlan> planMapper() {
        return (rs, rowNum) -> new Matrix26RestoreCleanupPlan(
                rs.getLong("id"), rs.getString("public_id"), rs.getLong("restore_job_id"),
                Matrix26RestoreCleanupStatus.valueOf(rs.getString("status")),
                rs.getString("snapshot_fingerprint"), rs.getString("plan_signature"),
                rs.getString("requested_by"), rs.getObject("requested_at", LocalDateTime.class),
                rs.getString("approved_by"), rs.getObject("approved_at", LocalDateTime.class),
                rs.getObject("started_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class),
                rs.getString("summary"), rs.getString("last_error")
        );
    }

    private RowMapper<Matrix26RestoreCleanupPlanItem> itemMapper() {
        return (rs, rowNum) -> new Matrix26RestoreCleanupPlanItem(
                rs.getLong("id"), rs.getLong("cleanup_plan_id"), rs.getInt("sequence_number"),
                rs.getString("resource_type"), rs.getString("location"), rs.getBoolean("existed_at_preview"),
                rs.getString("ownership"), rs.getString("planned_action"), rs.getString("confirmation_group"),
                Matrix26RestoreCleanupItemStatus.valueOf(rs.getString("status")), rs.getString("detail"),
                rs.getObject("started_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class),
                rs.getString("last_error")
        );
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
