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
public class Matrix26ArchiveDestructionRepository {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26ArchiveDestructionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertPlan(Matrix26ArchiveDestructionPlan plan) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO matrix26_archive_destruction_plan (
                        public_id, archive_record_id, archive_public_id, instance_id, instance_code,
                        instance_name, final_backup_job_id, final_backup_public_id, final_backup_sha256,
                        backup_directory, package_path, retention_until, retention_status, status,
                        run_number, blockers_count, would_delete_count, would_keep_count, protected_count,
                        review_count, reason, requested_by, requested_at, evaluated_at, last_error
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, plan.publicId());
            statement.setLong(2, plan.archiveRecordId());
            statement.setString(3, plan.archivePublicId());
            statement.setLong(4, plan.instanceId());
            statement.setString(5, plan.instanceCode());
            statement.setString(6, plan.instanceName());
            statement.setObject(7, plan.finalBackupJobId());
            statement.setString(8, plan.finalBackupPublicId());
            statement.setString(9, plan.finalBackupSha256());
            statement.setString(10, plan.backupDirectory());
            statement.setString(11, plan.packagePath());
            statement.setObject(12, plan.retentionUntil());
            statement.setString(13, plan.retentionStatus());
            statement.setString(14, plan.status().name());
            statement.setInt(15, plan.runNumber() == null ? 1 : plan.runNumber());
            statement.setInt(16, value(plan.blockersCount()));
            statement.setInt(17, value(plan.wouldDeleteCount()));
            statement.setInt(18, value(plan.wouldKeepCount()));
            statement.setInt(19, value(plan.protectedCount()));
            statement.setInt(20, value(plan.reviewCount()));
            statement.setString(21, limit(plan.reason(), 1000));
            statement.setString(22, limit(plan.requestedBy(), 120));
            statement.setObject(23, plan.requestedAt());
            statement.setObject(24, plan.evaluatedAt());
            statement.setString(25, limit(plan.lastError(), 8000));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new Matrix26PurgeException("Matrix26 could not persist the archive destruction plan.");
        }
        return key.longValue();
    }

    public Optional<Matrix26ArchiveDestructionPlan> findPlan(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_archive_destruction_plan WHERE id = ?", planMapper(), id)
                .stream().findFirst();
    }

    public List<Matrix26ArchiveDestructionPlan> recentPlans() {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_archive_destruction_plan
                ORDER BY requested_at DESC, id DESC LIMIT 100
                """, planMapper());
    }

    public Matrix26ArchiveDestructionSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM matrix26_archive_destruction_plan) AS total_plans,
                    (SELECT COUNT(*) FROM matrix26_archive_destruction_plan WHERE status = 'READY_FOR_REVIEW') AS ready_for_review,
                    (SELECT COUNT(*) FROM matrix26_archive_destruction_plan WHERE status = 'BLOCKED') AS blocked,
                    (SELECT COALESCE(SUM(protected_count), 0) FROM matrix26_archive_destruction_plan) AS protected_archives,
                    (SELECT COALESCE(SUM(would_delete_count), 0) FROM matrix26_archive_destruction_plan) AS would_delete
                """, (rs, rowNum) -> new Matrix26ArchiveDestructionSummary(
                rs.getLong("total_plans"),
                rs.getLong("ready_for_review"),
                rs.getLong("blocked"),
                rs.getLong("protected_archives"),
                rs.getLong("would_delete")
        ));
    }

    public void markAnalyzing(long planId, int runNumber) {
        jdbcTemplate.update("""
                UPDATE matrix26_archive_destruction_plan
                SET status = ?, run_number = ?, evaluated_at = NULL, last_error = NULL
                WHERE id = ?
                """, Matrix26ArchiveDestructionStatus.ANALYZING.name(), runNumber, planId);
    }

    public void completePlan(long planId, Matrix26ArchiveDestructionStatus status, Counts counts, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_archive_destruction_plan
                SET status = ?, blockers_count = ?, would_delete_count = ?, would_keep_count = ?,
                    protected_count = ?, review_count = ?, evaluated_at = ?, last_error = ?
                WHERE id = ?
                """, status.name(), counts.blockers(), counts.wouldDelete(), counts.wouldKeep(),
                counts.protectedCount(), counts.review(), LocalDateTime.now(), limit(error, 8000), planId);
    }

    public void failPlan(long planId, String error) {
        completePlan(planId, Matrix26ArchiveDestructionStatus.FAILED, new Counts(1, 0, 0, 0, 0), error);
    }

    public void addCheck(long planId, int runNumber, String code, String label, String status, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_archive_destruction_check (
                    destruction_plan_id, run_number, check_code, label, status, detail, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, planId, runNumber, code, limit(label, 220), status, limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26ArchiveDestructionCheck> checks(long planId) {
        return jdbcTemplate.query("""
                SELECT c.* FROM matrix26_archive_destruction_check c
                JOIN matrix26_archive_destruction_plan p ON p.id = c.destruction_plan_id
                WHERE c.destruction_plan_id = ? AND c.run_number = p.run_number
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
                INSERT INTO matrix26_archive_destruction_item (
                    destruction_plan_id, run_number, resource_type, resource_name, resource_path,
                    disposition, size_bytes, file_count, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, planId, runNumber, resourceType, limit(resourceName, 255), limit(resourcePath, 1000),
                disposition.name(), sizeBytes, fileCount, limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26ArchiveDestructionItem> items(long planId) {
        return jdbcTemplate.query("""
                SELECT i.* FROM matrix26_archive_destruction_item i
                JOIN matrix26_archive_destruction_plan p ON p.id = i.destruction_plan_id
                WHERE i.destruction_plan_id = ? AND i.run_number = p.run_number
                ORDER BY FIELD(i.disposition, 'BLOCKED','PROTECTED','REQUIRES_REVIEW','WOULD_DELETE','WOULD_KEEP','NOT_FOUND'), i.id
                """, itemMapper(), planId);
    }

    public void addEvent(long planId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_archive_destruction_event (
                    destruction_plan_id, event_type, status, actor, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, planId, eventType, status, limit(actor, 120), limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26ArchiveDestructionEvent> events(long planId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_archive_destruction_event
                WHERE destruction_plan_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new Matrix26ArchiveDestructionEvent(
                rs.getLong("id"),
                rs.getLong("destruction_plan_id"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("actor"),
                rs.getString("detail"),
                rs.getObject("created_at", LocalDateTime.class)
        ), planId);
    }

    public int associatedCloneCount(long archiveRecordId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM matrix26_archive_restore_link WHERE archive_record_id = ?
                """, Integer.class, archiveRecordId);
        return count == null ? 0 : count;
    }

    public boolean hasActiveArchiveCloneRestore(long archiveRecordId) {
        return positive("""
                SELECT COUNT(*)
                FROM matrix26_archive_restore_link l
                JOIN matrix26_restore_job r ON r.id = l.restore_job_id
                WHERE l.archive_record_id = ?
                  AND r.status NOT IN ('COMPLETED','FAILED','CLEANUP_REQUIRED','PARTIALLY_CLEANED','CLEANED','CANCELLED')
                """, archiveRecordId);
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

    private boolean positive(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private RowMapper<Matrix26ArchiveDestructionPlan> planMapper() {
        return (rs, rowNum) -> new Matrix26ArchiveDestructionPlan(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getLong("archive_record_id"),
                rs.getString("archive_public_id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                rs.getObject("final_backup_job_id", Long.class),
                rs.getString("final_backup_public_id"),
                rs.getString("final_backup_sha256"),
                rs.getString("backup_directory"),
                rs.getString("package_path"),
                rs.getObject("retention_until", LocalDateTime.class),
                rs.getString("retention_status"),
                Matrix26ArchiveDestructionStatus.valueOf(rs.getString("status")),
                rs.getObject("run_number", Integer.class),
                rs.getObject("blockers_count", Integer.class),
                rs.getObject("would_delete_count", Integer.class),
                rs.getObject("would_keep_count", Integer.class),
                rs.getObject("protected_count", Integer.class),
                rs.getObject("review_count", Integer.class),
                rs.getString("reason"),
                rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("evaluated_at", LocalDateTime.class),
                rs.getString("last_error")
        );
    }

    private RowMapper<Matrix26ArchiveDestructionCheck> checkMapper() {
        return (rs, rowNum) -> new Matrix26ArchiveDestructionCheck(
                rs.getLong("id"),
                rs.getLong("destruction_plan_id"),
                rs.getObject("run_number", Integer.class),
                rs.getString("check_code"),
                rs.getString("label"),
                rs.getString("status"),
                rs.getString("detail"),
                rs.getObject("checked_at", LocalDateTime.class)
        );
    }

    private RowMapper<Matrix26ArchiveDestructionItem> itemMapper() {
        return (rs, rowNum) -> new Matrix26ArchiveDestructionItem(
                rs.getLong("id"),
                rs.getLong("destruction_plan_id"),
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

    public record Counts(int blockers, int wouldDelete, int wouldKeep, int protectedCount, int review) {}
}
