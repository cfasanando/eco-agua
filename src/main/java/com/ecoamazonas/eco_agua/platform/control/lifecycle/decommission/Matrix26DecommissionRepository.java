package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

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
public class Matrix26DecommissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26DecommissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertJob(
            String publicId,
            long instanceId,
            String instanceCode,
            String instanceName,
            String reason,
            String notes,
            String actor,
            int retentionDays,
            String previousStatus
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO matrix26_decommission_job (
                        public_id, instance_id, instance_code, instance_name, status,
                        reason, administrative_notes, requested_by, requested_at,
                        retention_days, previous_instance_status, disabled_schedule_count
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, publicId);
            statement.setLong(2, instanceId);
            statement.setString(3, instanceCode);
            statement.setString(4, instanceName);
            statement.setString(5, Matrix26DecommissionStatus.REQUESTED.name());
            statement.setString(6, limit(reason, 1000));
            statement.setString(7, limit(notes, 8000));
            statement.setString(8, limit(actor, 120));
            statement.setObject(9, LocalDateTime.now());
            statement.setInt(10, retentionDays);
            statement.setString(11, previousStatus);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new Matrix26DecommissionException("Matrix26 could not persist the decommission job.");
        }
        return key.longValue();
    }

    public Optional<Matrix26DecommissionJob> findJob(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_decommission_job WHERE id = ?", jobMapper(), id)
                .stream().findFirst();
    }

    public List<Matrix26DecommissionJob> findRecentJobs() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_decommission_job ORDER BY requested_at DESC, id DESC LIMIT 100",
                jobMapper()
        );
    }

    public List<Matrix26DecommissionJob> findDecommissionedJobs() {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_decommission_job
                WHERE status = 'DECOMMISSIONED'
                ORDER BY completed_at DESC, id DESC
                """, jobMapper());
    }

    public boolean hasActiveJob(long instanceId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_decommission_job
                WHERE instance_id = ?
                  AND status IN ('REQUESTED','PRECHECKING','FINAL_BACKUP_RUNNING',
                                 'FINAL_BACKUP_VERIFYING','READY_TO_DECOMMISSION','DECOMMISSIONING')
                """, instanceId);
    }

    public boolean hasOtherActiveJob(long instanceId, long excludedJobId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_decommission_job
                WHERE instance_id = ? AND id <> ?
                  AND status IN ('REQUESTED','PRECHECKING','FINAL_BACKUP_RUNNING',
                                 'FINAL_BACKUP_VERIFYING','READY_TO_DECOMMISSION','DECOMMISSIONING')
                """, instanceId, excludedJobId);
    }

    public void markStarted(long jobId, Matrix26DecommissionStatus status) {
        jdbcTemplate.update("""
                UPDATE matrix26_decommission_job
                SET status = ?, started_at = COALESCE(started_at, ?), last_error = NULL
                WHERE id = ?
                """, status.name(), LocalDateTime.now(), jobId);
    }

    public void updateStatus(long jobId, Matrix26DecommissionStatus status) {
        jdbcTemplate.update(
                "UPDATE matrix26_decommission_job SET status = ?, last_error = NULL WHERE id = ?",
                status.name(), jobId
        );
    }

    public void attachFinalBackup(long jobId, Matrix26DecommissionBackupView backup) {
        jdbcTemplate.update("""
                UPDATE matrix26_decommission_job
                SET final_backup_job_id = ?, final_backup_public_id = ?,
                    final_backup_completed_at = ?, final_backup_verified_at = ?,
                    final_backup_key_id = ?, final_backup_sha256 = ?
                WHERE id = ?
                """,
                backup.jobId(), backup.publicId(), backup.completedAt(), backup.verifiedAt(),
                backup.keyId(), backup.packageSha256(), jobId
        );
    }

    public void setRetentionUntil(long jobId, LocalDateTime retentionUntil) {
        jdbcTemplate.update(
                "UPDATE matrix26_decommission_job SET retention_until = ? WHERE id = ?",
                retentionUntil, jobId
        );
    }

    public void setDisabledScheduleCount(long jobId, int count) {
        jdbcTemplate.update(
                "UPDATE matrix26_decommission_job SET disabled_schedule_count = ? WHERE id = ?",
                count, jobId
        );
    }

    public void complete(long jobId, String resultingStatus, LocalDateTime retentionUntil) {
        jdbcTemplate.update("""
                UPDATE matrix26_decommission_job
                SET status = ?, resulting_instance_status = ?, retention_until = ?,
                    completed_at = ?, last_error = NULL
                WHERE id = ?
                """, Matrix26DecommissionStatus.DECOMMISSIONED.name(), resultingStatus,
                retentionUntil, LocalDateTime.now(), jobId);
    }

    public void fail(long jobId, Matrix26DecommissionStatus status, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_decommission_job
                SET status = ?, completed_at = ?, last_error = ?
                WHERE id = ?
                """, status.name(), LocalDateTime.now(), limit(error, 8000), jobId);
    }

    public void upsertCheck(long jobId, String code, String label, String status, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_decommission_check (
                    decommission_job_id, check_code, label, status, detail, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    label = VALUES(label), status = VALUES(status), detail = VALUES(detail),
                    checked_at = VALUES(checked_at)
                """, jobId, code, label, status, limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26DecommissionCheck> findChecks(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_decommission_check
                WHERE decommission_job_id = ? ORDER BY id
                """, (rs, rowNum) -> new Matrix26DecommissionCheck(
                rs.getLong("id"),
                rs.getLong("decommission_job_id"),
                rs.getString("check_code"),
                rs.getString("label"),
                rs.getString("status"),
                rs.getString("detail"),
                rs.getObject("checked_at", LocalDateTime.class)
        ), jobId);
    }

    public void addEvent(long jobId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_decommission_event (
                    decommission_job_id, event_type, status, actor, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, jobId, eventType, status, limit(actor, 120), limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26DecommissionEvent> findEvents(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_decommission_event
                WHERE decommission_job_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new Matrix26DecommissionEvent(
                rs.getLong("id"),
                rs.getLong("decommission_job_id"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("actor"),
                rs.getString("detail"),
                rs.getObject("created_at", LocalDateTime.class)
        ), jobId);
    }

    public void snapshotSchedules(long jobId, long instanceId) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO matrix26_decommission_schedule_state (
                    decommission_job_id, schedule_id, schedule_name, was_enabled,
                    previous_next_run_at, disabled, created_at
                )
                SELECT ?, id, name, enabled, next_run_at, 0, ?
                FROM matrix26_backup_schedule
                WHERE instance_id = ?
                """, jobId, LocalDateTime.now(), instanceId);
    }

    public int disableSchedules(long jobId, long instanceId, String actor) {
        int updated = jdbcTemplate.update("""
                UPDATE matrix26_backup_schedule
                SET enabled = 0, next_run_at = NULL, updated_by = ?, updated_at = ?
                WHERE instance_id = ? AND enabled = 1
                """, actor, LocalDateTime.now(), instanceId);
        jdbcTemplate.update("""
                UPDATE matrix26_decommission_schedule_state
                SET disabled = 1, disabled_at = ?
                WHERE decommission_job_id = ? AND was_enabled = 1
                """, LocalDateTime.now(), jobId);
        return updated;
    }

    public List<Matrix26DecommissionScheduleState> findScheduleStates(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_decommission_schedule_state
                WHERE decommission_job_id = ? ORDER BY schedule_name, schedule_id
                """, (rs, rowNum) -> new Matrix26DecommissionScheduleState(
                rs.getLong("id"),
                rs.getLong("decommission_job_id"),
                rs.getLong("schedule_id"),
                rs.getString("schedule_name"),
                rs.getBoolean("was_enabled"),
                rs.getObject("previous_next_run_at", LocalDateTime.class),
                rs.getBoolean("disabled"),
                rs.getObject("disabled_at", LocalDateTime.class)
        ), jobId);
    }

    public int countEnabledSchedules(long instanceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule WHERE instance_id = ? AND enabled = 1",
                Integer.class,
                instanceId
        );
        return count == null ? 0 : count;
    }

    public Matrix26DecommissionBackupView finalBackup(long backupJobId) {
        return jdbcTemplate.query("""
                SELECT j.id, j.public_id, j.completed_at, e.verified_at, e.key_id,
                       e.package_sha256, e.retention_class, e.protected_flag,
                       e.verification_status
                FROM matrix26_backup_job j
                JOIN matrix26_backup_encryption e ON e.job_id = j.id
                WHERE j.id = ? AND j.status = 'COMPLETED' AND e.encrypted = 1
                """, (rs, rowNum) -> new Matrix26DecommissionBackupView(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getObject("verified_at", LocalDateTime.class),
                rs.getString("key_id"),
                rs.getString("package_sha256"),
                rs.getString("retention_class"),
                rs.getBoolean("protected_flag"),
                rs.getString("verification_status")
        ), backupJobId).stream().findFirst().orElse(new Matrix26DecommissionBackupView(
                null, null, null, null, null, null, null, false, null
        ));
    }

    public boolean hasActiveBackup(long instanceId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_backup_job
                WHERE instance_id = ? AND status IN ('PENDING','VALIDATING','RUNNING','COMPRESSING','VERIFYING')
                """, instanceId);
    }

    public boolean hasActiveScheduleExecution(long instanceId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_backup_schedule_execution
                WHERE instance_id = ? AND status IN ('SCHEDULED','QUEUED','RUNNING','RETRY_WAITING')
                """, instanceId);
    }

    public boolean hasActiveRuntimeOperation(long instanceId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_runtime_operation
                WHERE instance_id = ? AND status IN ('REQUESTED','RUNNING','STOP_TIMEOUT')
                """, instanceId);
    }

    public boolean hasActiveCloneRestore(long instanceId, String instanceCode) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_restore_job
                WHERE (source_instance_id = ? OR target_instance_code = ?)
                  AND status NOT IN ('COMPLETED','FAILED','CLEANUP_REQUIRED','PARTIALLY_CLEANED','CLEANED','CANCELLED')
                """, instanceId, instanceCode);
    }

    public boolean hasActiveInPlaceRestore(long instanceId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_inplace_restore_job
                WHERE source_instance_id = ?
                  AND status NOT IN ('COMPLETED','ROLLED_BACK','FAILED','CANCELLED')
                """, instanceId);
    }

    public boolean hasActiveLifecycle(long instanceId) {
        return positive("""
                SELECT COUNT(*) FROM matrix26_lifecycle_job
                WHERE instance_id = ?
                  AND status IN ('REQUESTED','VALIDATING','SUSPENDING','REACTIVATING')
                """, instanceId);
    }

    public Matrix26DecommissionSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM matrix26_decommission_job) total,
                    (SELECT COUNT(*) FROM matrix26_decommission_job WHERE status = 'READY_TO_DECOMMISSION') ready,
                    (SELECT COUNT(*) FROM platform_business_client WHERE status = 'DECOMMISSIONED') decommissioned,
                    (SELECT COUNT(*) FROM matrix26_decommission_job
                        WHERE status IN ('FAILED','MANUAL_REVIEW_REQUIRED')) failed
                """, (rs, rowNum) -> new Matrix26DecommissionSummary(
                rs.getLong("total"), rs.getLong("ready"), rs.getLong("decommissioned"), rs.getLong("failed")
        ));
    }

    private boolean positive(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private RowMapper<Matrix26DecommissionJob> jobMapper() {
        return (rs, rowNum) -> new Matrix26DecommissionJob(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                Matrix26DecommissionStatus.valueOf(rs.getString("status")),
                rs.getString("reason"),
                rs.getString("administrative_notes"),
                rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getObject("retention_days", Integer.class),
                rs.getObject("retention_until", LocalDateTime.class),
                rs.getString("previous_instance_status"),
                rs.getString("resulting_instance_status"),
                rs.getObject("final_backup_job_id", Long.class),
                rs.getString("final_backup_public_id"),
                rs.getObject("final_backup_completed_at", LocalDateTime.class),
                rs.getObject("final_backup_verified_at", LocalDateTime.class),
                rs.getString("final_backup_key_id"),
                rs.getString("final_backup_sha256"),
                rs.getObject("disabled_schedule_count", Integer.class),
                rs.getString("last_error")
        );
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }
}
