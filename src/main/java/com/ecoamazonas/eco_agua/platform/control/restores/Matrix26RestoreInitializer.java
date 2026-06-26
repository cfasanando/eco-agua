package com.ecoamazonas.eco_agua.platform.control.restores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreInitializer {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26RestoreInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_job (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    backup_job_id BIGINT NOT NULL,
                    backup_public_id VARCHAR(80) NOT NULL,
                    source_instance_id BIGINT NOT NULL,
                    source_instance_code VARCHAR(80) NOT NULL,
                    source_instance_name VARCHAR(180) NOT NULL,
                    source_database_name VARCHAR(120) NOT NULL,
                    target_instance_id BIGINT NULL,
                    target_instance_code VARCHAR(80) NOT NULL,
                    target_instance_name VARCHAR(180) NOT NULL,
                    target_database_name VARCHAR(120) NOT NULL,
                    target_runtime_profile VARCHAR(120) NOT NULL,
                    target_runtime_port INT NOT NULL,
                    target_public_url VARCHAR(500) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    start_after_restore BIT NOT NULL DEFAULT 1,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    temporary_directory VARCHAR(900) NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_restore_public_id (public_id),
                    KEY idx_matrix26_restore_backup (backup_job_id),
                    KEY idx_matrix26_restore_status (status, requested_at),
                    KEY idx_matrix26_restore_target (target_instance_code, target_database_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_step (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    restore_job_id BIGINT NOT NULL,
                    step_code VARCHAR(80) NOT NULL,
                    sequence_number INT NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_restore_step (restore_job_id, step_code),
                    KEY idx_matrix26_restore_step_order (restore_job_id, sequence_number),
                    CONSTRAINT fk_matrix26_restore_step_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_artifact (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    restore_job_id BIGINT NOT NULL,
                    artifact_type VARCHAR(60) NOT NULL,
                    path VARCHAR(900) NOT NULL,
                    size_bytes BIGINT NULL,
                    sha256 VARCHAR(64) NULL,
                    status VARCHAR(30) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_restore_artifact_job (restore_job_id),
                    CONSTRAINT fk_matrix26_restore_artifact_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_verification (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    restore_job_id BIGINT NOT NULL,
                    check_code VARCHAR(80) NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_restore_verification_job (restore_job_id),
                    CONSTRAINT fk_matrix26_restore_verification_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
