package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26BackupInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_backup_job (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    instance_name VARCHAR(180) NOT NULL,
                    database_name VARCHAR(120) NOT NULL,
                    backup_type VARCHAR(40) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    backup_root VARCHAR(700) NOT NULL,
                    backup_directory VARCHAR(900) NOT NULL,
                    tool_path VARCHAR(900) NULL,
                    tool_version VARCHAR(255) NULL,
                    database_host VARCHAR(255) NULL,
                    database_port INT NULL,
                    database_size_bytes BIGINT NULL,
                    dump_size_bytes BIGINT NULL,
                    compressed_size_bytes BIGINT NULL,
                    table_count INT NULL,
                    sha256 VARCHAR(64) NULL,
                    manifest_path VARCHAR(900) NULL,
                    report_path VARCHAR(900) NULL,
                    verification_summary VARCHAR(1000) NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_backup_public_id (public_id),
                    KEY idx_matrix26_backup_instance_requested (instance_id, requested_at),
                    KEY idx_matrix26_backup_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_backup_artifact (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    job_id BIGINT NOT NULL,
                    artifact_type VARCHAR(50) NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    relative_path VARCHAR(900) NOT NULL,
                    size_bytes BIGINT NOT NULL,
                    sha256 VARCHAR(64) NULL,
                    status VARCHAR(30) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_backup_artifact_job (job_id),
                    CONSTRAINT fk_matrix26_backup_artifact_job
                        FOREIGN KEY (job_id) REFERENCES matrix26_backup_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_backup_verification (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    job_id BIGINT NOT NULL,
                    check_code VARCHAR(80) NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_backup_verification_job (job_id),
                    CONSTRAINT fk_matrix26_backup_verification_job
                        FOREIGN KEY (job_id) REFERENCES matrix26_backup_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_backup_encryption (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    job_id BIGINT NOT NULL,
                    encrypted BIT NOT NULL DEFAULT 1,
                    algorithm VARCHAR(80) NOT NULL,
                    format_version INT NOT NULL,
                    key_id VARCHAR(80) NOT NULL,
                    package_path VARCHAR(900) NOT NULL,
                    package_size_bytes BIGINT NOT NULL,
                    package_sha256 VARCHAR(64) NOT NULL,
                    verification_status VARCHAR(40) NOT NULL,
                    verified_at DATETIME(6) NULL,
                    retention_class VARCHAR(30) NOT NULL,
                    expires_at DATETIME(6) NULL,
                    protected_flag BIT NOT NULL DEFAULT 0,
                    protection_reason VARCHAR(500) NULL,
                    encrypted_at DATETIME(6) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_backup_encryption_job (job_id),
                    KEY idx_matrix26_backup_encryption_retention (retention_class, expires_at),
                    KEY idx_matrix26_backup_encryption_verification (verification_status),
                    CONSTRAINT fk_matrix26_backup_encryption_job
                        FOREIGN KEY (job_id) REFERENCES matrix26_backup_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_backup_policy (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    daily_keep INT NOT NULL DEFAULT 7,
                    weekly_keep INT NOT NULL DEFAULT 4,
                    monthly_keep INT NOT NULL DEFAULT 6,
                    final_keep_indefinitely BIT NOT NULL DEFAULT 1,
                    enabled BIT NOT NULL DEFAULT 1,
                    updated_by VARCHAR(120) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_backup_policy_instance (instance_id),
                    KEY idx_matrix26_backup_policy_code (instance_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_backup_retention_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    job_id BIGINT NULL,
                    backup_public_id VARCHAR(80) NULL,
                    action VARCHAR(40) NOT NULL,
                    actor VARCHAR(120) NOT NULL,
                    reason VARCHAR(1000) NULL,
                    bytes_affected BIGINT NOT NULL DEFAULT 0,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_retention_instance_created (instance_id, created_at),
                    KEY idx_matrix26_retention_backup_public (backup_public_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }
}
