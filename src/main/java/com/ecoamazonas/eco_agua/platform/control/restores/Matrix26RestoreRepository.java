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
public class Matrix26RestoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26RestoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertJob(Matrix26RestoreJob job) {
        Number key = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("matrix26_restore_job")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(new MapSqlParameterSource()
                        .addValue("public_id", job.publicId())
                        .addValue("backup_job_id", job.backupJobId())
                        .addValue("backup_public_id", job.backupPublicId())
                        .addValue("source_instance_id", job.sourceInstanceId())
                        .addValue("source_instance_code", job.sourceInstanceCode())
                        .addValue("source_instance_name", job.sourceInstanceName())
                        .addValue("source_database_name", job.sourceDatabaseName())
                        .addValue("target_instance_id", job.targetInstanceId())
                        .addValue("target_instance_code", job.targetInstanceCode())
                        .addValue("target_instance_name", job.targetInstanceName())
                        .addValue("target_database_name", job.targetDatabaseName())
                        .addValue("target_runtime_profile", job.targetRuntimeProfile())
                        .addValue("target_runtime_port", job.targetRuntimePort())
                        .addValue("target_public_url", job.targetPublicUrl())
                        .addValue("status", job.status().name())
                        .addValue("start_after_restore", job.startAfterRestore())
                        .addValue("requested_by", job.requestedBy())
                        .addValue("requested_at", job.requestedAt())
                        .addValue("temporary_directory", job.temporaryDirectory()));
        return key.longValue();
    }

    public void insertStep(long jobId, String code, int sequence, String label) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_restore_step (
                    restore_job_id, step_code, sequence_number, label, status
                ) VALUES (?, ?, ?, ?, ?)
                """, jobId, code, sequence, label, Matrix26RestoreStepStatus.PENDING.name());
    }

    public void updateJobStatus(long id, Matrix26RestoreStatus status) {
        jdbcTemplate.update("UPDATE matrix26_restore_job SET status = ? WHERE id = ?", status.name(), id);
    }

    public void markStarted(long id, String temporaryDirectory) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_job
                SET status = ?, started_at = ?, temporary_directory = ?, last_error = NULL
                WHERE id = ?
                """, Matrix26RestoreStatus.VALIDATING.name(), LocalDateTime.now(), temporaryDirectory, id);
    }

    public void setTargetInstance(long id, long targetInstanceId) {
        jdbcTemplate.update("UPDATE matrix26_restore_job SET target_instance_id = ? WHERE id = ?", targetInstanceId, id);
    }

    public void complete(long id) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_job
                SET status = ?, completed_at = ?, temporary_directory = NULL, last_error = NULL
                WHERE id = ?
                """, Matrix26RestoreStatus.COMPLETED.name(), LocalDateTime.now(), id);
    }

    public void fail(long id, Matrix26RestoreStatus status, String error) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_job
                SET status = ?, completed_at = ?, last_error = ?
                WHERE id = ?
                """, status.name(), LocalDateTime.now(), limit(error, 8000), id);
    }

    public void startStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_step
                SET status = ?, detail = ?, started_at = ?, completed_at = NULL
                WHERE restore_job_id = ? AND step_code = ?
                """, Matrix26RestoreStepStatus.RUNNING.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void completeStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_step
                SET status = ?, detail = ?, completed_at = ?
                WHERE restore_job_id = ? AND step_code = ?
                """, Matrix26RestoreStepStatus.COMPLETED.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void failStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_step
                SET status = ?, detail = ?, completed_at = ?
                WHERE restore_job_id = ? AND step_code = ?
                """, Matrix26RestoreStepStatus.FAILED.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void skipStep(long jobId, String code, String detail) {
        jdbcTemplate.update("""
                UPDATE matrix26_restore_step
                SET status = ?, detail = ?, completed_at = ?
                WHERE restore_job_id = ? AND step_code = ?
                """, Matrix26RestoreStepStatus.SKIPPED.name(), limit(detail, 8000), LocalDateTime.now(), jobId, code);
    }

    public void insertArtifact(long jobId, String type, String path, Long size, String sha, String status) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_restore_artifact (
                    restore_job_id, artifact_type, path, size_bytes, sha256, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, jobId, type, path, size, sha, status, LocalDateTime.now());
    }

    public void insertVerification(long jobId, String code, String label, String status, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_restore_verification (
                    restore_job_id, check_code, label, status, detail, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, jobId, code, label, status, limit(detail, 8000), LocalDateTime.now());
    }

    public Optional<Matrix26RestoreJob> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM matrix26_restore_job WHERE id = ?", jobMapper(), id)
                .stream().findFirst();
    }

    public List<Matrix26RestoreJob> findRecent() {
        return jdbcTemplate.query("SELECT * FROM matrix26_restore_job ORDER BY requested_at DESC, id DESC LIMIT 100", jobMapper());
    }

    public List<Matrix26RestoreStep> findSteps(long jobId) {
        return jdbcTemplate.query("SELECT * FROM matrix26_restore_step WHERE restore_job_id = ? ORDER BY sequence_number, id",
                (rs, rowNum) -> new Matrix26RestoreStep(
                        rs.getLong("id"), rs.getLong("restore_job_id"), rs.getString("step_code"),
                        rs.getInt("sequence_number"), rs.getString("label"),
                        Matrix26RestoreStepStatus.valueOf(rs.getString("status")), rs.getString("detail"),
                        rs.getObject("started_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class)
                ), jobId);
    }

    public List<Matrix26RestoreArtifact> findArtifacts(long jobId) {
        return jdbcTemplate.query("SELECT * FROM matrix26_restore_artifact WHERE restore_job_id = ? ORDER BY id",
                (rs, rowNum) -> new Matrix26RestoreArtifact(
                        rs.getLong("id"), rs.getLong("restore_job_id"), rs.getString("artifact_type"),
                        rs.getString("path"), rs.getObject("size_bytes", Long.class), rs.getString("sha256"),
                        rs.getString("status"), rs.getObject("created_at", LocalDateTime.class)
                ), jobId);
    }

    public List<Matrix26RestoreVerification> findVerifications(long jobId) {
        return jdbcTemplate.query("SELECT * FROM matrix26_restore_verification WHERE restore_job_id = ? ORDER BY id",
                (rs, rowNum) -> new Matrix26RestoreVerification(
                        rs.getLong("id"), rs.getLong("restore_job_id"), rs.getString("check_code"),
                        rs.getString("label"), rs.getString("status"), rs.getString("detail"),
                        rs.getObject("checked_at", LocalDateTime.class)
                ), jobId);
    }

    public Matrix26RestoreSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       SUM(status = 'COMPLETED') AS completed,
                       SUM(status IN ('FAILED','CLEANUP_REQUIRED')) AS failed,
                       SUM(status NOT IN ('COMPLETED','FAILED','CLEANUP_REQUIRED','CANCELLED')) AS running
                FROM matrix26_restore_job
                """, (rs, rowNum) -> new Matrix26RestoreSummary(
                        rs.getLong("total"), rs.getLong("completed"), rs.getLong("failed"), rs.getLong("running")
                ));
    }

    public boolean hasActiveRestore() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM matrix26_restore_job
                WHERE status NOT IN ('COMPLETED','FAILED','CLEANUP_REQUIRED','CANCELLED')
                """, Integer.class);
        return count != null && count > 0;
    }

    private RowMapper<Matrix26RestoreJob> jobMapper() {
        return (rs, rowNum) -> new Matrix26RestoreJob(
                rs.getLong("id"), rs.getString("public_id"), rs.getLong("backup_job_id"),
                rs.getString("backup_public_id"), rs.getLong("source_instance_id"),
                rs.getString("source_instance_code"), rs.getString("source_instance_name"),
                rs.getString("source_database_name"), rs.getObject("target_instance_id", Long.class),
                rs.getString("target_instance_code"), rs.getString("target_instance_name"),
                rs.getString("target_database_name"), rs.getString("target_runtime_profile"),
                rs.getInt("target_runtime_port"), rs.getString("target_public_url"),
                Matrix26RestoreStatus.valueOf(rs.getString("status")), rs.getBoolean("start_after_restore"),
                rs.getString("requested_by"), rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("started_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class),
                rs.getString("temporary_directory"), rs.getString("last_error")
        );
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
