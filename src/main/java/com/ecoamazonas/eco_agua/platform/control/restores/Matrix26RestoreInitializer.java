package com.ecoamazonas.eco_agua.platform.control.restores;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_validation_run (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    restore_job_id BIGINT NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NOT NULL,
                    completed_at DATETIME(6) NULL,
                    summary TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_restore_validation_public_id (public_id),
                    KEY idx_matrix26_restore_validation_job (restore_job_id, requested_at),
                    KEY idx_matrix26_restore_validation_status (status, requested_at),
                    CONSTRAINT fk_matrix26_restore_validation_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_validation_item (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    validation_run_id BIGINT NOT NULL,
                    check_code VARCHAR(100) NOT NULL,
                    category VARCHAR(60) NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    source_value TEXT NULL,
                    target_value TEXT NULL,
                    detail TEXT NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_restore_validation_item (validation_run_id, check_code),
                    KEY idx_matrix26_restore_validation_item_status (validation_run_id, status),
                    CONSTRAINT fk_matrix26_restore_validation_item_run
                        FOREIGN KEY (validation_run_id) REFERENCES matrix26_restore_validation_run(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_resume_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    restore_job_id BIGINT NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    starting_step_code VARCHAR(80) NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    completed_at DATETIME(6) NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_restore_resume_job (restore_job_id, requested_at),
                    CONSTRAINT fk_matrix26_restore_resume_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_cleanup_plan (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    restore_job_id BIGINT NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    snapshot_fingerprint VARCHAR(64) NOT NULL,
                    plan_signature VARCHAR(64) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    approved_by VARCHAR(120) NULL,
                    approved_at DATETIME(6) NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    summary TEXT NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_restore_cleanup_public_id (public_id),
                    KEY idx_matrix26_restore_cleanup_job (restore_job_id, requested_at),
                    KEY idx_matrix26_restore_cleanup_status (status, requested_at),
                    CONSTRAINT fk_matrix26_restore_cleanup_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_cleanup_item (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    cleanup_plan_id BIGINT NOT NULL,
                    sequence_number INT NOT NULL,
                    resource_type VARCHAR(80) NOT NULL,
                    location VARCHAR(900) NOT NULL,
                    existed_at_preview BIT NOT NULL DEFAULT 0,
                    ownership VARCHAR(60) NOT NULL,
                    planned_action VARCHAR(40) NOT NULL,
                    confirmation_group VARCHAR(40) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_restore_cleanup_item (cleanup_plan_id, resource_type),
                    KEY idx_matrix26_restore_cleanup_item_order (cleanup_plan_id, sequence_number),
                    KEY idx_matrix26_restore_cleanup_item_status (cleanup_plan_id, status),
                    CONSTRAINT fk_matrix26_restore_cleanup_item_plan
                        FOREIGN KEY (cleanup_plan_id) REFERENCES matrix26_restore_cleanup_plan(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_restore_cleanup_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    cleanup_plan_id BIGINT NOT NULL,
                    cleanup_item_id BIGINT NULL,
                    event_type VARCHAR(80) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    actor_username VARCHAR(120) NOT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_restore_cleanup_event_plan (cleanup_plan_id, created_at),
                    CONSTRAINT fk_matrix26_restore_cleanup_event_plan
                        FOREIGN KEY (cleanup_plan_id) REFERENCES matrix26_restore_cleanup_plan(id)
                        ON DELETE CASCADE,
                    CONSTRAINT fk_matrix26_restore_cleanup_event_item
                        FOREIGN KEY (cleanup_item_id) REFERENCES matrix26_restore_cleanup_item(id)
                        ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_item
                SET status = 'FAILED', last_error = COALESCE(last_error, 'Interrupted while Matrix26 was offline'),
                    completed_at = COALESCE(completed_at, NOW(6))
                WHERE status = 'RUNNING'
                """);
        jdbcTemplate.update("""
                UPDATE matrix26_restore_cleanup_plan
                SET status = 'PARTIALLY_CLEANED',
                    last_error = COALESCE(last_error, 'Cleanup was interrupted while Matrix26 was offline'),
                    completed_at = COALESCE(completed_at, NOW(6))
                WHERE status = 'RUNNING'
                """);
        jdbcTemplate.update("""
                UPDATE matrix26_restore_job
                SET status = 'PARTIALLY_CLEANED',
                    last_error = COALESCE(last_error, 'Cleanup was interrupted while Matrix26 was offline'),
                    completed_at = COALESCE(completed_at, NOW(6))
                WHERE status = 'CLEANING'
                """);
    }
}
