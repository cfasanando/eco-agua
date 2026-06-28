package com.ecoamazonas.eco_agua.platform.control.purge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26PurgeRepository {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26PurgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertPlan(Matrix26PurgePlan plan) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO matrix26_purge_plan (
                        public_id, archive_record_id, archive_public_id, decommission_job_id,
                        instance_id, instance_code, instance_name, database_name, runtime_profile,
                        runtime_port, final_backup_job_id, final_backup_public_id, final_backup_sha256,
                        retention_until, retention_status, status, run_number, eligible_for_future_purge,
                        blockers_count, would_delete_count, would_keep_count, protected_count,
                        review_count, not_found_count, reason, requested_by, requested_at,
                        evaluated_at, last_error
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, plan.publicId());
            statement.setLong(2, plan.archiveRecordId());
            statement.setString(3, plan.archivePublicId());
            statement.setObject(4, plan.decommissionJobId());
            statement.setLong(5, plan.instanceId());
            statement.setString(6, plan.instanceCode());
            statement.setString(7, plan.instanceName());
            statement.setString(8, plan.databaseName());
            statement.setString(9, plan.runtimeProfile());
            statement.setObject(10, plan.runtimePort());
            statement.setObject(11, plan.finalBackupJobId());
            statement.setString(12, plan.finalBackupPublicId());
            statement.setString(13, plan.finalBackupSha256());
            statement.setObject(14, plan.retentionUntil());
            statement.setString(15, plan.retentionStatus());
            statement.setString(16, plan.status().name());
            statement.setInt(17, plan.runNumber() == null ? 1 : plan.runNumber());
            statement.setBoolean(18, plan.eligibleForFuturePurge());
            statement.setInt(19, value(plan.blockersCount()));
            statement.setInt(20, value(plan.wouldDeleteCount()));
            statement.setInt(21, value(plan.wouldKeepCount()));
            statement.setInt(22, value(plan.protectedCount()));
            statement.setInt(23, value(plan.reviewCount()));
            statement.setInt(24, value(plan.notFoundCount()));
            statement.setString(25, limit(plan.reason(), 1000));
            statement.setString(26, limit(plan.requestedBy(), 120));
            statement.setObject(27, plan.requestedAt());
            statement.setObject(28, plan.evaluatedAt());
            statement.setString(29, limit(plan.lastError(), 8000));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new Matrix26PurgeException("Matrix26 could not persist the purge dry run plan.");
        }
        return key.longValue();
    }

    public Optional<Matrix26PurgePlan> findPlan(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_purge_plan WHERE id = ?", planMapper(), id)
                .stream().findFirst();
    }

    public List<Matrix26PurgePlan> recentPlans() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_purge_plan ORDER BY requested_at DESC, id DESC LIMIT 100",
                planMapper()
        );
    }

    public Matrix26PurgeSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM matrix26_purge_plan) AS total_plans,
                    (SELECT COUNT(*) FROM matrix26_purge_plan WHERE status = 'DRY_RUN_READY') AS dry_run_ready,
                    (SELECT COUNT(*) FROM matrix26_purge_plan WHERE status = 'BLOCKED') AS blocked,
                    (SELECT COALESCE(SUM(would_delete_count), 0) FROM matrix26_purge_plan) AS total_would_delete,
                    (SELECT COALESCE(SUM(protected_count), 0) FROM matrix26_purge_plan) AS total_protected
                """, (rs, rowNum) -> new Matrix26PurgeSummary(
                rs.getLong("total_plans"),
                rs.getLong("dry_run_ready"),
                rs.getLong("blocked"),
                rs.getLong("total_would_delete"),
                rs.getLong("total_protected")
        ));
    }

    public void markRunning(long planId, int runNumber) {
        jdbcTemplate.update("""
                UPDATE matrix26_purge_plan
                SET status = ?, run_number = ?, evaluated_at = NULL, last_error = NULL
                WHERE id = ?
                """, Matrix26PurgeStatus.DRY_RUN_RUNNING.name(), runNumber, planId);
    }

    public void completePlan(long planId, Matrix26PurgeStatus status, boolean eligible, Counts counts, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_purge_plan
                SET status = ?, eligible_for_future_purge = ?, blockers_count = ?,
                    would_delete_count = ?, would_keep_count = ?, protected_count = ?,
                    review_count = ?, not_found_count = ?, evaluated_at = ?, last_error = ?
                WHERE id = ?
                """, status.name(), eligible, counts.blockers(), counts.wouldDelete(), counts.wouldKeep(),
                counts.protectedCount(), counts.review(), counts.notFound(), LocalDateTime.now(), limit(error, 8000), planId);
    }

    public void failPlan(long planId, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_purge_plan
                SET status = ?, evaluated_at = ?, last_error = ?
                WHERE id = ?
                """, Matrix26PurgeStatus.FAILED.name(), LocalDateTime.now(), limit(error, 8000), planId);
    }

    public void addCheck(long planId, int runNumber, String code, String label, String status, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_purge_check (
                    purge_plan_id, run_number, check_code, label, status, detail, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, planId, runNumber, code, limit(label, 220), status, limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26PurgeCheck> checks(long planId) {
        return jdbcTemplate.query("""
                SELECT c.* FROM matrix26_purge_check c
                JOIN matrix26_purge_plan p ON p.id = c.purge_plan_id
                WHERE c.purge_plan_id = ? AND c.run_number = p.run_number
                ORDER BY c.id
                """, checkMapper(), planId);
    }

    public void addItem(
            long planId,
            int runNumber,
            String resourceType,
            String resourceName,
            String resourcePath,
            Matrix26PurgeDisposition disposition,
            Long sizeBytes,
            Integer fileCount,
            String detail
    ) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_purge_item (
                    purge_plan_id, run_number, resource_type, resource_name, resource_path,
                    disposition, size_bytes, file_count, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, planId, runNumber, resourceType, limit(resourceName, 255), limit(resourcePath, 1000),
                disposition.name(), sizeBytes, fileCount, limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26PurgeItem> items(long planId) {
        return jdbcTemplate.query("""
                SELECT i.* FROM matrix26_purge_item i
                JOIN matrix26_purge_plan p ON p.id = i.purge_plan_id
                WHERE i.purge_plan_id = ? AND i.run_number = p.run_number
                ORDER BY FIELD(i.disposition, 'BLOCKED','PROTECTED','REQUIRES_REVIEW','WOULD_DELETE','WOULD_KEEP','NOT_FOUND'), i.id
                """, itemMapper(), planId);
    }

    public void addEvent(long planId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_purge_event (
                    purge_plan_id, event_type, status, actor, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, planId, eventType, status, limit(actor, 120), limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26PurgeEvent> events(long planId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_purge_event
                WHERE purge_plan_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new Matrix26PurgeEvent(
                rs.getLong("id"),
                rs.getLong("purge_plan_id"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("actor"),
                rs.getString("detail"),
                rs.getObject("created_at", LocalDateTime.class)
        ), planId);
    }

    public boolean schemaExists(String databaseName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?
                """, Integer.class, databaseName);
        return count != null && count > 0;
    }

    public int tableCount(String databaseName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?
                """, Integer.class, databaseName);
        return count == null ? 0 : count;
    }

    public long databaseSizeBytes(String databaseName) {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(data_length + index_length), 0)
                FROM information_schema.tables WHERE table_schema = ?
                """, Long.class, databaseName);
        return value == null ? 0L : value;
    }

    public boolean hasEnabledSchedules(long instanceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule WHERE instance_id = ? AND enabled = 1",
                Integer.class,
                instanceId
        );
        return count != null && count > 0;
    }

    public int totalSchedules(long instanceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule WHERE instance_id = ?",
                Integer.class,
                instanceId
        );
        return count == null ? 0 : count;
    }

    public int backupCount(long instanceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_job WHERE instance_id = ?",
                Integer.class,
                instanceId
        );
        return count == null ? 0 : count;
    }

    public boolean hasActiveBackupOrRestore(long instanceId, String instanceCode) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_backup_job
                WHERE instance_id = ? AND status IN ('PENDING','VALIDATING','RUNNING','COMPRESSING','VERIFYING')
                """, instanceId)
                || positive("""
                SELECT COUNT(*) FROM matrix26_restore_job
                WHERE (source_instance_id = ? OR target_instance_code = ?)
                  AND status NOT IN ('COMPLETED','FAILED','CLEANUP_REQUIRED','PARTIALLY_CLEANED','CLEANED','CANCELLED')
                """, instanceId, instanceCode)
                || positive("""
                SELECT COUNT(*) FROM matrix26_inplace_restore_job
                WHERE source_instance_id = ?
                  AND status NOT IN ('COMPLETED','ROLLED_BACK','FAILED','CANCELLED')
                """, instanceId);
    }

    public int associatedCloneCount(long archiveRecordId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM matrix26_archive_restore_link WHERE archive_record_id = ?
                """, Integer.class, archiveRecordId);
        return count == null ? 0 : count;
    }

    private boolean positive(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private RowMapper<Matrix26PurgePlan> planMapper() {
        return (rs, rowNum) -> new Matrix26PurgePlan(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getLong("archive_record_id"),
                rs.getString("archive_public_id"),
                rs.getObject("decommission_job_id", Long.class),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                rs.getString("database_name"),
                rs.getString("runtime_profile"),
                rs.getObject("runtime_port", Integer.class),
                rs.getObject("final_backup_job_id", Long.class),
                rs.getString("final_backup_public_id"),
                rs.getString("final_backup_sha256"),
                rs.getObject("retention_until", LocalDateTime.class),
                rs.getString("retention_status"),
                Matrix26PurgeStatus.valueOf(rs.getString("status")),
                rs.getObject("run_number", Integer.class),
                rs.getBoolean("eligible_for_future_purge"),
                rs.getObject("blockers_count", Integer.class),
                rs.getObject("would_delete_count", Integer.class),
                rs.getObject("would_keep_count", Integer.class),
                rs.getObject("protected_count", Integer.class),
                rs.getObject("review_count", Integer.class),
                rs.getObject("not_found_count", Integer.class),
                rs.getString("reason"),
                rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("evaluated_at", LocalDateTime.class),
                rs.getString("last_error")
        );
    }

    private RowMapper<Matrix26PurgeCheck> checkMapper() {
        return (rs, rowNum) -> new Matrix26PurgeCheck(
                rs.getLong("id"),
                rs.getLong("purge_plan_id"),
                rs.getObject("run_number", Integer.class),
                rs.getString("check_code"),
                rs.getString("label"),
                rs.getString("status"),
                rs.getString("detail"),
                rs.getObject("checked_at", LocalDateTime.class)
        );
    }

    private RowMapper<Matrix26PurgeItem> itemMapper() {
        return (rs, rowNum) -> new Matrix26PurgeItem(
                rs.getLong("id"),
                rs.getLong("purge_plan_id"),
                rs.getObject("run_number", Integer.class),
                rs.getString("resource_type"),
                rs.getString("resource_name"),
                rs.getString("resource_path"),
                Matrix26PurgeDisposition.valueOf(rs.getString("disposition")),
                rs.getObject("size_bytes", Long.class),
                rs.getObject("file_count", Integer.class),
                rs.getString("detail"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }

    public record Counts(int blockers, int wouldDelete, int wouldKeep, int protectedCount, int review, int notFound) {}
}
