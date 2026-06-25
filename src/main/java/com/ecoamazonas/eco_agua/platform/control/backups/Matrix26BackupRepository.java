package com.ecoamazonas.eco_agua.platform.control.backups;

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
public class Matrix26BackupRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26BackupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertJob(Matrix26BackupJob job) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO matrix26_backup_job (
                        public_id, instance_id, instance_code, instance_name, database_name,
                        backup_type, status, requested_by, requested_at, backup_root,
                        backup_directory, database_host, database_port
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, job.publicId());
            statement.setLong(2, job.instanceId());
            statement.setString(3, job.instanceCode());
            statement.setString(4, job.instanceName());
            statement.setString(5, job.databaseName());
            statement.setString(6, job.backupType());
            statement.setString(7, job.status().name());
            statement.setString(8, job.requestedBy());
            statement.setObject(9, job.requestedAt());
            statement.setString(10, job.backupRoot());
            statement.setString(11, job.backupDirectory());
            statement.setString(12, job.databaseHost());
            statement.setObject(13, job.databasePort());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("The backup job could not be created.");
        }
        return key.longValue();
    }

    public void markStarted(long id, Matrix26BackupStatus status, String toolPath, String toolVersion) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_job
                SET status = ?, started_at = ?, tool_path = ?, tool_version = ?, last_error = NULL
                WHERE id = ?
                """,
                status.name(), LocalDateTime.now(), toolPath, toolVersion, id
        );
    }

    public void updateStatus(long id, Matrix26BackupStatus status) {
        jdbcTemplate.update(
                "UPDATE matrix26_backup_job SET status = ? WHERE id = ?",
                status.name(), id
        );
    }

    public void complete(
            long id,
            long databaseSizeBytes,
            long dumpSizeBytes,
            long compressedSizeBytes,
            int tableCount,
            String sha256,
            String manifestPath,
            String reportPath,
            String verificationSummary
    ) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_job
                SET status = ?, completed_at = ?, database_size_bytes = ?, dump_size_bytes = ?,
                    compressed_size_bytes = ?, table_count = ?, sha256 = ?, manifest_path = ?,
                    report_path = ?, verification_summary = ?, last_error = NULL
                WHERE id = ?
                """,
                Matrix26BackupStatus.COMPLETED.name(),
                LocalDateTime.now(),
                databaseSizeBytes,
                dumpSizeBytes,
                compressedSizeBytes,
                tableCount,
                sha256,
                manifestPath,
                reportPath,
                verificationSummary,
                id
        );
    }

    public void fail(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_job
                SET status = ?, completed_at = ?, last_error = ?
                WHERE id = ?
                """,
                Matrix26BackupStatus.FAILED.name(), LocalDateTime.now(), limit(error, 4000), id
        );
    }

    public void insertArtifact(
            long jobId,
            String artifactType,
            String fileName,
            String relativePath,
            long sizeBytes,
            String sha256,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO matrix26_backup_artifact (
                    job_id, artifact_type, file_name, relative_path, size_bytes, sha256, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                jobId, artifactType, fileName, relativePath, sizeBytes, sha256, status, LocalDateTime.now()
        );
    }

    public void insertVerification(
            long jobId,
            String code,
            String label,
            Matrix26BackupVerificationStatus status,
            String detail
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO matrix26_backup_verification (
                    job_id, check_code, label, status, detail, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                jobId, code, label, status.name(), limit(detail, 4000), LocalDateTime.now()
        );
    }

    public Optional<Matrix26BackupJob> findById(long id) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_job WHERE id = ?",
                jobMapper(), id
        ).stream().findFirst();
    }

    public List<Matrix26BackupJob> findRecent() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_job ORDER BY requested_at DESC, id DESC LIMIT 200",
                jobMapper()
        );
    }

    public List<Matrix26BackupJob> findByInstanceId(long instanceId) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_job WHERE instance_id = ? ORDER BY requested_at DESC, id DESC LIMIT 200",
                jobMapper(), instanceId
        );
    }

    public List<Matrix26BackupArtifact> findArtifacts(long jobId) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_artifact WHERE job_id = ? ORDER BY id",
                (rs, rowNum) -> new Matrix26BackupArtifact(
                        rs.getLong("id"),
                        rs.getLong("job_id"),
                        rs.getString("artifact_type"),
                        rs.getString("file_name"),
                        rs.getString("relative_path"),
                        rs.getObject("size_bytes", Long.class),
                        rs.getString("sha256"),
                        rs.getString("status"),
                        rs.getObject("created_at", LocalDateTime.class)
                ),
                jobId
        );
    }

    public List<Matrix26BackupVerification> findVerifications(long jobId) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_verification WHERE job_id = ? ORDER BY id",
                (rs, rowNum) -> new Matrix26BackupVerification(
                        rs.getLong("id"),
                        rs.getLong("job_id"),
                        rs.getString("check_code"),
                        rs.getString("label"),
                        Matrix26BackupVerificationStatus.valueOf(rs.getString("status")),
                        rs.getString("detail"),
                        rs.getObject("checked_at", LocalDateTime.class)
                ),
                jobId
        );
    }

    public Matrix26BackupSummary summary() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) AS total,
                       SUM(status = 'COMPLETED') AS completed,
                       SUM(status = 'FAILED') AS failed,
                       SUM(status IN ('PENDING','VALIDATING','RUNNING','COMPRESSING','VERIFYING')) AS running,
                       COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN compressed_size_bytes ELSE 0 END), 0) AS total_bytes
                FROM matrix26_backup_job
                """,
                (rs, rowNum) -> new Matrix26BackupSummary(
                        rs.getLong("total"),
                        rs.getLong("completed"),
                        rs.getLong("failed"),
                        rs.getLong("running"),
                        rs.getLong("total_bytes")
                )
        );
    }

    public boolean hasActiveJob(long instanceId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM matrix26_backup_job
                WHERE instance_id = ?
                  AND status IN ('PENDING','VALIDATING','RUNNING','COMPRESSING','VERIFYING')
                """,
                Integer.class,
                instanceId
        );
        return count != null && count > 0;
    }

    private RowMapper<Matrix26BackupJob> jobMapper() {
        return (rs, rowNum) -> new Matrix26BackupJob(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                rs.getString("database_name"),
                rs.getString("backup_type"),
                Matrix26BackupStatus.valueOf(rs.getString("status")),
                rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getString("backup_root"),
                rs.getString("backup_directory"),
                rs.getString("tool_path"),
                rs.getString("tool_version"),
                rs.getString("database_host"),
                rs.getObject("database_port", Integer.class),
                rs.getObject("database_size_bytes", Long.class),
                rs.getObject("dump_size_bytes", Long.class),
                rs.getObject("compressed_size_bytes", Long.class),
                rs.getObject("table_count", Integer.class),
                rs.getString("sha256"),
                rs.getString("manifest_path"),
                rs.getString("report_path"),
                rs.getString("verification_summary"),
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
