package com.ecoamazonas.eco_agua.platform.control.lifecycle;

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
public class Matrix26LifecycleRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26LifecycleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertJob(
            String publicId,
            long instanceId,
            String instanceCode,
            String instanceName,
            Matrix26LifecycleAction action,
            String reason,
            String actor,
            String previousStatus,
            Long relatedJobId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO matrix26_lifecycle_job (
                        public_id, instance_id, instance_code, instance_name, action, status,
                        reason, requested_by, requested_at, previous_instance_status,
                        resulting_instance_status, runtime_was_running, paused_schedule_count,
                        related_lifecycle_job_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, 0, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, publicId);
            statement.setLong(2, instanceId);
            statement.setString(3, instanceCode);
            statement.setString(4, instanceName);
            statement.setString(5, action.name());
            statement.setString(6, Matrix26LifecycleStatus.REQUESTED.name());
            statement.setString(7, limit(reason, 1000));
            statement.setString(8, actor);
            statement.setObject(9, LocalDateTime.now());
            statement.setString(10, previousStatus);
            if (relatedJobId == null) {
                statement.setNull(11, java.sql.Types.BIGINT);
            } else {
                statement.setLong(11, relatedJobId);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new Matrix26LifecycleException("Matrix26 could not persist the lifecycle job.");
        }
        return key.longValue();
    }

    public Optional<Matrix26LifecycleJob> findJob(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_lifecycle_job WHERE id = ?", jobMapper(), id)
                .stream().findFirst();
    }

    public List<Matrix26LifecycleJob> findRecentJobs() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_lifecycle_job ORDER BY requested_at DESC, id DESC LIMIT 100",
                jobMapper()
        );
    }

    public Optional<Matrix26LifecycleJob> findLatestSuspension(long instanceId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_lifecycle_job
                WHERE instance_id = ? AND action = 'SUSPEND' AND status = 'SUSPENDED'
                ORDER BY completed_at DESC, id DESC LIMIT 1
                """, jobMapper(), instanceId).stream().findFirst();
    }

    public boolean hasActiveJob(long instanceId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM matrix26_lifecycle_job
                WHERE instance_id = ?
                  AND status IN ('REQUESTED','VALIDATING','SUSPENDING','REACTIVATING')
                """, Integer.class, instanceId);
        return count != null && count > 0;
    }

    public void markStarted(long jobId, Matrix26LifecycleStatus status) {
        jdbcTemplate.update("""
                UPDATE matrix26_lifecycle_job
                SET status = ?, started_at = ?, completed_at = NULL, last_error = NULL
                WHERE id = ?
                """, status.name(), LocalDateTime.now(), jobId);
    }

    public void updateStatus(long jobId, Matrix26LifecycleStatus status) {
        jdbcTemplate.update("UPDATE matrix26_lifecycle_job SET status = ? WHERE id = ?", status.name(), jobId);
    }

    public void attachBackup(long jobId, Matrix26LifecycleBackupView backup) {
        jdbcTemplate.update("""
                UPDATE matrix26_lifecycle_job
                SET verified_backup_job_id = ?, verified_backup_public_id = ?, verified_backup_completed_at = ?
                WHERE id = ?
                """, backup.jobId(), backup.publicId(), backup.completedAt(), jobId);
    }

    public void setRuntimeWasRunning(long jobId, boolean running) {
        jdbcTemplate.update(
                "UPDATE matrix26_lifecycle_job SET runtime_was_running = ? WHERE id = ?",
                running, jobId
        );
    }

    public void setPausedScheduleCount(long jobId, int count) {
        jdbcTemplate.update(
                "UPDATE matrix26_lifecycle_job SET paused_schedule_count = ? WHERE id = ?",
                count, jobId
        );
    }

    public void complete(long jobId, Matrix26LifecycleStatus status, String resultingStatus) {
        jdbcTemplate.update("""
                UPDATE matrix26_lifecycle_job
                SET status = ?, resulting_instance_status = ?, completed_at = ?, last_error = NULL
                WHERE id = ?
                """, status.name(), resultingStatus, LocalDateTime.now(), jobId);
    }

    public void fail(long jobId, Matrix26LifecycleStatus status, String error, String resultingStatus) {
        jdbcTemplate.update("""
                UPDATE matrix26_lifecycle_job
                SET status = ?, resulting_instance_status = ?, completed_at = ?, last_error = ?
                WHERE id = ?
                """, status.name(), resultingStatus, LocalDateTime.now(), limit(error, 8000), jobId);
    }

    public void addEvent(long jobId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_lifecycle_event (
                    lifecycle_job_id, event_type, status, actor, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, jobId, eventType, status, actor, limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26LifecycleEvent> findEvents(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_lifecycle_event
                WHERE lifecycle_job_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new Matrix26LifecycleEvent(
                rs.getLong("id"),
                rs.getLong("lifecycle_job_id"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("actor"),
                rs.getString("detail"),
                rs.getObject("created_at", LocalDateTime.class)
        ), jobId);
    }

    public void snapshotEnabledSchedules(long jobId, long instanceId) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO matrix26_lifecycle_schedule_state (
                    lifecycle_job_id, schedule_id, schedule_name, was_enabled,
                    previous_next_run_at, restored, created_at
                )
                SELECT ?, id, name, enabled, next_run_at, 0, ?
                FROM matrix26_backup_schedule
                WHERE instance_id = ? AND enabled = 1
                """, jobId, LocalDateTime.now(), instanceId);
    }

    public int pauseSnapshottedSchedules(long jobId, String actor) {
        return jdbcTemplate.update("""
                UPDATE matrix26_backup_schedule s
                JOIN matrix26_lifecycle_schedule_state ls ON ls.schedule_id = s.id
                SET s.enabled = 0, s.next_run_at = NULL, s.updated_by = ?, s.updated_at = ?
                WHERE ls.lifecycle_job_id = ? AND ls.was_enabled = 1
                """, actor, LocalDateTime.now(), jobId);
    }

    public int restoreSnapshottedSchedules(long suspensionJobId, String actor) {
        int updated = jdbcTemplate.update("""
                UPDATE matrix26_backup_schedule s
                JOIN matrix26_lifecycle_schedule_state ls ON ls.schedule_id = s.id
                SET s.enabled = 1,
                    s.next_run_at = CASE
                        WHEN ls.previous_next_run_at IS NOT NULL AND ls.previous_next_run_at > ?
                            THEN ls.previous_next_run_at
                        ELSE DATE_ADD(?, INTERVAL 1 MINUTE)
                    END,
                    s.updated_by = ?, s.updated_at = ?
                WHERE ls.lifecycle_job_id = ? AND ls.was_enabled = 1 AND ls.restored = 0
                """, LocalDateTime.now(), LocalDateTime.now(), actor, LocalDateTime.now(), suspensionJobId);
        jdbcTemplate.update("""
                UPDATE matrix26_lifecycle_schedule_state
                SET restored = 1, restored_at = ?
                WHERE lifecycle_job_id = ? AND was_enabled = 1 AND restored = 0
                """, LocalDateTime.now(), suspensionJobId);
        return updated;
    }

    public List<Matrix26LifecycleScheduleState> findScheduleStates(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_lifecycle_schedule_state
                WHERE lifecycle_job_id = ? ORDER BY schedule_name, schedule_id
                """, (rs, rowNum) -> new Matrix26LifecycleScheduleState(
                rs.getLong("id"),
                rs.getLong("lifecycle_job_id"),
                rs.getLong("schedule_id"),
                rs.getString("schedule_name"),
                rs.getBoolean("was_enabled"),
                rs.getObject("previous_next_run_at", LocalDateTime.class),
                rs.getBoolean("restored"),
                rs.getObject("restored_at", LocalDateTime.class)
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

    public Matrix26LifecycleBackupView latestVerifiedBackup(long instanceId) {
        return jdbcTemplate.query("""
                SELECT j.id, j.public_id, j.completed_at, e.verified_at
                FROM matrix26_backup_job j
                JOIN matrix26_backup_encryption e ON e.job_id = j.id
                WHERE j.instance_id = ?
                  AND j.status = 'COMPLETED'
                  AND e.encrypted = 1
                  AND e.verification_status = 'VERIFIED'
                ORDER BY COALESCE(e.verified_at, j.completed_at) DESC, j.id DESC
                LIMIT 1
                """, (rs, rowNum) -> new Matrix26LifecycleBackupView(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getObject("verified_at", LocalDateTime.class)
        ), instanceId).stream().findFirst().orElse(new Matrix26LifecycleBackupView(null, null, null, null));
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

    public Matrix26LifecycleSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM matrix26_lifecycle_job) total,
                    (SELECT COUNT(*) FROM platform_business_client WHERE status = 'SUSPENDED') suspended,
                    (SELECT COUNT(*) FROM matrix26_lifecycle_job
                        WHERE status IN ('SUSPENSION_FAILED','REACTIVATION_FAILED')) failed,
                    (SELECT COUNT(*) FROM matrix26_lifecycle_job
                        WHERE status IN ('REQUESTED','VALIDATING','SUSPENDING','REACTIVATING')) active
                """, (rs, rowNum) -> new Matrix26LifecycleSummary(
                rs.getLong("total"), rs.getLong("suspended"), rs.getLong("failed"), rs.getLong("active")
        ));
    }

    private boolean positive(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private RowMapper<Matrix26LifecycleJob> jobMapper() {
        return (rs, rowNum) -> new Matrix26LifecycleJob(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                Matrix26LifecycleAction.valueOf(rs.getString("action")),
                Matrix26LifecycleStatus.valueOf(rs.getString("status")),
                rs.getString("reason"),
                rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getString("previous_instance_status"),
                rs.getString("resulting_instance_status"),
                rs.getBoolean("runtime_was_running"),
                rs.getObject("paused_schedule_count", Integer.class),
                rs.getObject("verified_backup_job_id", Long.class),
                rs.getString("verified_backup_public_id"),
                rs.getObject("verified_backup_completed_at", LocalDateTime.class),
                rs.getObject("related_lifecycle_job_id", Long.class),
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
