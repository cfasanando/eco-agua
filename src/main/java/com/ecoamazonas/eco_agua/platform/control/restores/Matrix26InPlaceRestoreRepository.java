package com.ecoamazonas.eco_agua.platform.control.restores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26InPlaceRestoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26InPlaceRestoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertJob(Matrix26InPlaceRestoreJob job) {
        Number key = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("matrix26_inplace_restore_job")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(new MapSqlParameterSource()
                        .addValue("public_id", job.publicId())
                        .addValue("backup_job_id", job.backupJobId())
                        .addValue("backup_public_id", job.backupPublicId())
                        .addValue("source_instance_id", job.sourceInstanceId())
                        .addValue("source_instance_code", job.sourceInstanceCode())
                        .addValue("source_instance_name", job.sourceInstanceName())
                        .addValue("source_database_name", job.sourceDatabaseName())
                        .addValue("stage_database_name", job.stageDatabaseName())
                        .addValue("rollback_database_name", job.rollbackDatabaseName())
                        .addValue("source_runtime_profile", job.sourceRuntimeProfile())
                        .addValue("source_runtime_port", job.sourceRuntimePort())
                        .addValue("source_public_url", job.sourcePublicUrl())
                        .addValue("status", job.status().name())
                        .addValue("requested_by", job.requestedBy())
                        .addValue("requested_at", job.requestedAt())
                        .addValue("work_directory", job.workDirectory())
                        .addValue("stage_data_directory", job.stageDataDirectory())
                        .addValue("rollback_data_directory", job.rollbackDataDirectory()));
        return key.longValue();
    }

    public void insertStep(long jobId, String code, int sequence, String label) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_inplace_restore_step (
                    inplace_restore_job_id, step_code, sequence_number, label, status
                ) VALUES (?, ?, ?, ?, ?)
                """, jobId, code, sequence, label, Matrix26InPlaceRestoreStepStatus.PENDING.name());
    }

    public Optional<Matrix26InPlaceRestoreJob> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_inplace_restore_job WHERE id = ?", jobMapper(), id)
                .stream().findFirst();
    }

    public List<Matrix26InPlaceRestoreJob> findRecent() {
        return jdbcTemplate.query("SELECT * FROM matrix26_inplace_restore_job ORDER BY requested_at DESC, id DESC LIMIT 100", jobMapper());
    }

    public List<Matrix26InPlaceRestoreStep> findSteps(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_inplace_restore_step
                WHERE inplace_restore_job_id = ? ORDER BY sequence_number, id
                """, (rs, rowNum) -> new Matrix26InPlaceRestoreStep(
                rs.getLong("id"), rs.getLong("inplace_restore_job_id"), rs.getString("step_code"),
                rs.getInt("sequence_number"), rs.getString("label"),
                Matrix26InPlaceRestoreStepStatus.valueOf(rs.getString("status")), rs.getString("detail"),
                rs.getObject("started_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class)
        ), jobId);
    }

    public List<Matrix26InPlaceRestoreCheck> findChecks(long jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_inplace_restore_check
                WHERE inplace_restore_job_id = ? ORDER BY id
                """, (rs, rowNum) -> new Matrix26InPlaceRestoreCheck(
                rs.getLong("id"), rs.getLong("inplace_restore_job_id"), rs.getString("check_code"),
                rs.getString("label"), rs.getString("status"), rs.getString("expected_value"),
                rs.getString("actual_value"), rs.getString("detail"),
                rs.getObject("checked_at", LocalDateTime.class)
        ), jobId);
    }

    public void updateStatus(long jobId, Matrix26InPlaceRestoreStatus status) {
        jdbcTemplate.update("UPDATE matrix26_inplace_restore_job SET status = ? WHERE id = ?", status.name(), jobId);
    }

    public void markStarted(long jobId) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET status = ?, started_at = ?, completed_at = NULL, last_error = NULL
                WHERE id = ?
                """, Matrix26InPlaceRestoreStatus.PRECHECKING.name(), LocalDateTime.now(), jobId);
    }

    public void attachSafetyBackup(long jobId, long backupJobId, String backupPublicId) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET safety_backup_job_id = ?, safety_backup_public_id = ?
                WHERE id = ?
                """, backupJobId, backupPublicId, jobId);
    }

    public void markSwitchMutation(long jobId) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET switched_at = COALESCE(switched_at, ?)
                WHERE id = ?
                """, LocalDateTime.now(), jobId);
    }

    public void markSwitched(long jobId, LocalDateTime rollbackExpiresAt) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET status = ?, switched_at = ?, rollback_expires_at = ?, last_error = NULL
                WHERE id = ?
                """, Matrix26InPlaceRestoreStatus.AWAITING_CONFIRMATION.name(), LocalDateTime.now(), rollbackExpiresAt, jobId);
    }

    public void confirm(long jobId) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET status = ?, confirmed_at = ?, completed_at = ?, last_error = NULL
                WHERE id = ?
                """, Matrix26InPlaceRestoreStatus.COMPLETED.name(), LocalDateTime.now(), LocalDateTime.now(), jobId);
    }

    public void rolledBack(long jobId) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET status = ?, completed_at = ?, last_error = NULL
                WHERE id = ?
                """, Matrix26InPlaceRestoreStatus.ROLLED_BACK.name(), LocalDateTime.now(), jobId);
    }

    public void fail(long jobId, Matrix26InPlaceRestoreStatus status, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET status = ?, completed_at = ?, last_error = ?
                WHERE id = ?
                """, status.name(), LocalDateTime.now(), limit(error, 8000), jobId);
    }

    public void startStep(long jobId, String code, Matrix26InPlaceRestoreStatus jobStatus, String detail) {
        updateStatus(jobId, jobStatus);
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_step
                SET status = ?, detail = ?, started_at = ?, completed_at = NULL
                WHERE inplace_restore_job_id = ? AND step_code = ?
                """, Matrix26InPlaceRestoreStepStatus.RUNNING.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void completeStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_step
                SET status = ?, detail = ?, completed_at = ?
                WHERE inplace_restore_job_id = ? AND step_code = ?
                """, Matrix26InPlaceRestoreStepStatus.COMPLETED.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void failStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_step
                SET status = ?, detail = ?, completed_at = ?
                WHERE inplace_restore_job_id = ? AND step_code = ?
                """, Matrix26InPlaceRestoreStepStatus.FAILED.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void resetStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_step
                SET status = ?, detail = ?, started_at = NULL, completed_at = NULL
                WHERE inplace_restore_job_id = ? AND step_code = ?
                """, Matrix26InPlaceRestoreStepStatus.PENDING.name(), limit(detail, 8000), jobId, code);
    }

    public void addCheck(long jobId, String code, String label, String status, String expected, String actual, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_inplace_restore_check (
                    inplace_restore_job_id, check_code, label, status,
                    expected_value, actual_value, detail, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, jobId, code, label, status, limit(expected, 8000), limit(actual, 8000), limit(detail, 8000), LocalDateTime.now());
    }

    public void addEvent(long jobId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_inplace_restore_event (
                    inplace_restore_job_id, event_type, status, actor_username, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, jobId, eventType, status, actor, limit(detail, 8000), LocalDateTime.now());
    }

    public boolean hasActiveJob() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM matrix26_inplace_restore_job
                WHERE status NOT IN ('COMPLETED','ROLLED_BACK','FAILED','CANCELLED')
                """, Integer.class);
        return count != null && count > 0;
    }

    public Matrix26InPlaceRestoreSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       SUM(status = 'COMPLETED') AS completed,
                       SUM(status = 'AWAITING_CONFIRMATION') AS awaiting_confirmation,
                       SUM(status IN ('FAILED','MANUAL_RECOVERY_REQUIRED')) AS failed
                FROM matrix26_inplace_restore_job
                """, (rs, rowNum) -> new Matrix26InPlaceRestoreSummary(
                rs.getLong("total"), rs.getLong("completed"),
                rs.getLong("awaiting_confirmation"), rs.getLong("failed")
        ));
    }

    private RowMapper<Matrix26InPlaceRestoreJob> jobMapper() {
        return (rs, rowNum) -> new Matrix26InPlaceRestoreJob(
                rs.getLong("id"), rs.getString("public_id"), rs.getLong("backup_job_id"),
                rs.getString("backup_public_id"), rs.getLong("source_instance_id"),
                rs.getString("source_instance_code"), rs.getString("source_instance_name"),
                rs.getString("source_database_name"), rs.getString("stage_database_name"),
                rs.getString("rollback_database_name"), rs.getString("source_runtime_profile"),
                rs.getObject("source_runtime_port", Integer.class), rs.getString("source_public_url"),
                rs.getObject("safety_backup_job_id", Long.class), rs.getString("safety_backup_public_id"),
                Matrix26InPlaceRestoreStatus.valueOf(rs.getString("status")), rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class), rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("switched_at", LocalDateTime.class), rs.getObject("confirmed_at", LocalDateTime.class),
                rs.getObject("rollback_expires_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class),
                rs.getString("work_directory"), rs.getString("stage_data_directory"),
                rs.getString("rollback_data_directory"), rs.getString("last_error")
        );
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
