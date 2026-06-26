package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupSecurityRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26BackupSecurityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertEncryption(
            long jobId,
            String algorithm,
            int formatVersion,
            String keyId,
            String packagePath,
            long packageSizeBytes,
            String packageSha256,
            Matrix26BackupVerificationState verificationStatus,
            LocalDateTime verifiedAt,
            Matrix26BackupRetentionClass retentionClass,
            LocalDateTime expiresAt,
            boolean protectedFlag,
            String protectionReason,
            LocalDateTime encryptedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO matrix26_backup_encryption (
                    job_id, encrypted, algorithm, format_version, key_id, package_path,
                    package_size_bytes, package_sha256, verification_status, verified_at,
                    retention_class, expires_at, protected_flag, protection_reason,
                    encrypted_at, created_at, updated_at
                ) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    encrypted = VALUES(encrypted),
                    algorithm = VALUES(algorithm),
                    format_version = VALUES(format_version),
                    key_id = VALUES(key_id),
                    package_path = VALUES(package_path),
                    package_size_bytes = VALUES(package_size_bytes),
                    package_sha256 = VALUES(package_sha256),
                    verification_status = VALUES(verification_status),
                    verified_at = VALUES(verified_at),
                    retention_class = VALUES(retention_class),
                    expires_at = VALUES(expires_at),
                    protected_flag = VALUES(protected_flag),
                    protection_reason = VALUES(protection_reason),
                    encrypted_at = VALUES(encrypted_at),
                    updated_at = VALUES(updated_at)
                """,
                jobId,
                algorithm,
                formatVersion,
                keyId,
                packagePath,
                packageSizeBytes,
                packageSha256,
                verificationStatus.name(),
                verifiedAt,
                retentionClass.name(),
                expiresAt,
                protectedFlag,
                protectionReason,
                encryptedAt,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public Optional<Matrix26BackupEncryption> findEncryption(long jobId) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_encryption WHERE job_id = ?",
                (rs, rowNum) -> new Matrix26BackupEncryption(
                        rs.getLong("id"),
                        rs.getLong("job_id"),
                        rs.getBoolean("encrypted"),
                        rs.getString("algorithm"),
                        rs.getObject("format_version", Integer.class),
                        rs.getString("key_id"),
                        rs.getString("package_path"),
                        rs.getObject("package_size_bytes", Long.class),
                        rs.getString("package_sha256"),
                        Matrix26BackupVerificationState.valueOf(rs.getString("verification_status")),
                        rs.getObject("verified_at", LocalDateTime.class),
                        Matrix26BackupRetentionClass.from(rs.getString("retention_class")),
                        rs.getObject("expires_at", LocalDateTime.class),
                        rs.getBoolean("protected_flag"),
                        rs.getString("protection_reason"),
                        rs.getObject("encrypted_at", LocalDateTime.class),
                        rs.getObject("updated_at", LocalDateTime.class)
                ),
                jobId
        ).stream().findFirst();
    }

    public void updateVerification(
            long jobId,
            Matrix26BackupVerificationState status,
            LocalDateTime verifiedAt
    ) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_encryption
                SET verification_status = ?, verified_at = ?, updated_at = ?
                WHERE job_id = ?
                """,
                status.name(), verifiedAt, LocalDateTime.now(), jobId
        );
    }

    public void updateJobPackage(
            long jobId,
            long packageBytes,
            String packageSha256,
            String manifestPath,
            String reportPath,
            String verificationSummary
    ) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_job
                SET backup_type = 'MANUAL_FULL_ENCRYPTED', compressed_size_bytes = ?, sha256 = ?,
                    manifest_path = ?, report_path = ?, verification_summary = ?, completed_at = ?,
                    status = ?, last_error = NULL
                WHERE id = ?
                """,
                packageBytes,
                packageSha256,
                manifestPath,
                reportPath,
                verificationSummary,
                LocalDateTime.now(),
                Matrix26BackupStatus.COMPLETED.name(),
                jobId
        );
    }

    public void replaceArtifacts(
            long jobId,
            List<Matrix26BackupArtifact> artifacts
    ) {
        jdbcTemplate.update("DELETE FROM matrix26_backup_artifact WHERE job_id = ?", jobId);
        for (Matrix26BackupArtifact artifact : artifacts) {
            jdbcTemplate.update(
                    """
                    INSERT INTO matrix26_backup_artifact (
                        job_id, artifact_type, file_name, relative_path, size_bytes, sha256, status, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    jobId,
                    artifact.artifactType(),
                    artifact.fileName(),
                    artifact.relativePath(),
                    artifact.sizeBytes(),
                    artifact.sha256(),
                    artifact.status(),
                    LocalDateTime.now()
            );
        }
    }

    public Matrix26BackupPolicy findPolicy(long instanceId, String instanceCode) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_policy WHERE instance_id = ?",
                (rs, rowNum) -> new Matrix26BackupPolicy(
                        rs.getLong("id"),
                        rs.getLong("instance_id"),
                        rs.getString("instance_code"),
                        rs.getInt("daily_keep"),
                        rs.getInt("weekly_keep"),
                        rs.getInt("monthly_keep"),
                        rs.getBoolean("final_keep_indefinitely"),
                        rs.getBoolean("enabled"),
                        rs.getString("updated_by"),
                        rs.getObject("updated_at", LocalDateTime.class)
                ),
                instanceId
        ).stream().findFirst().orElse(Matrix26BackupPolicy.defaults(instanceId, instanceCode));
    }

    public void upsertPolicy(Matrix26BackupPolicy policy) {
        jdbcTemplate.update(
                """
                INSERT INTO matrix26_backup_policy (
                    instance_id, instance_code, daily_keep, weekly_keep, monthly_keep,
                    final_keep_indefinitely, enabled, updated_by, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    instance_code = VALUES(instance_code),
                    daily_keep = VALUES(daily_keep),
                    weekly_keep = VALUES(weekly_keep),
                    monthly_keep = VALUES(monthly_keep),
                    final_keep_indefinitely = VALUES(final_keep_indefinitely),
                    enabled = VALUES(enabled),
                    updated_by = VALUES(updated_by),
                    updated_at = VALUES(updated_at)
                """,
                policy.instanceId(),
                policy.instanceCode(),
                policy.dailyKeep(),
                policy.weeklyKeep(),
                policy.monthlyKeep(),
                policy.finalKeepIndefinitely(),
                policy.enabled(),
                policy.updatedBy(),
                LocalDateTime.now()
        );
    }

    public List<RetentionRow> findRetentionRows(long instanceId) {
        return jdbcTemplate.query(
                """
                SELECT j.id, j.public_id, j.requested_at, j.backup_directory,
                       COALESCE(e.package_size_bytes, j.compressed_size_bytes, 0) AS stored_bytes,
                       e.retention_class, e.verification_status, e.protected_flag,
                       e.protection_reason
                FROM matrix26_backup_job j
                JOIN matrix26_backup_encryption e ON e.job_id = j.id
                WHERE j.instance_id = ?
                  AND j.status = 'COMPLETED'
                  AND e.encrypted = 1
                ORDER BY j.requested_at DESC, j.id DESC
                """,
                (rs, rowNum) -> new RetentionRow(
                        rs.getLong("id"),
                        rs.getString("public_id"),
                        rs.getObject("requested_at", LocalDateTime.class),
                        rs.getString("backup_directory"),
                        rs.getLong("stored_bytes"),
                        Matrix26BackupRetentionClass.from(rs.getString("retention_class")),
                        Matrix26BackupVerificationState.valueOf(rs.getString("verification_status")),
                        rs.getBoolean("protected_flag"),
                        rs.getString("protection_reason")
                ),
                instanceId
        );
    }

    public void insertRetentionEvent(
            long instanceId,
            String instanceCode,
            Long jobId,
            String backupPublicId,
            String action,
            String actor,
            String reason,
            long bytes
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO matrix26_backup_retention_event (
                    instance_id, instance_code, job_id, backup_public_id, action,
                    actor, reason, bytes_affected, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                instanceId,
                instanceCode,
                jobId,
                backupPublicId,
                action,
                actor,
                reason,
                bytes,
                LocalDateTime.now()
        );
    }

    public void deleteJob(long jobId) {
        jdbcTemplate.update("DELETE FROM matrix26_backup_job WHERE id = ?", jobId);
    }

    public record RetentionRow(
            long jobId,
            String publicId,
            LocalDateTime requestedAt,
            String backupDirectory,
            long storedBytes,
            Matrix26BackupRetentionClass retentionClass,
            Matrix26BackupVerificationState verificationStatus,
            boolean protectedFlag,
            String protectionReason
    ) {
    }
}
