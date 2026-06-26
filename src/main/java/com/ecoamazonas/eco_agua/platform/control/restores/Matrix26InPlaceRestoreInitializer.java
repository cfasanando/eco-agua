package com.ecoamazonas.eco_agua.platform.control.restores;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26InPlaceRestoreInitializer {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26InPlaceRestoreInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_inplace_restore_job (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    backup_job_id BIGINT NOT NULL,
                    backup_public_id VARCHAR(80) NOT NULL,
                    source_instance_id BIGINT NOT NULL,
                    source_instance_code VARCHAR(80) NOT NULL,
                    source_instance_name VARCHAR(180) NOT NULL,
                    source_database_name VARCHAR(120) NOT NULL,
                    stage_database_name VARCHAR(120) NOT NULL,
                    rollback_database_name VARCHAR(120) NOT NULL,
                    source_runtime_profile VARCHAR(120) NOT NULL,
                    source_runtime_port INT NOT NULL,
                    source_public_url VARCHAR(500) NOT NULL,
                    safety_backup_job_id BIGINT NULL,
                    safety_backup_public_id VARCHAR(80) NULL,
                    status VARCHAR(50) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NULL,
                    switched_at DATETIME(6) NULL,
                    confirmed_at DATETIME(6) NULL,
                    rollback_expires_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    work_directory VARCHAR(900) NULL,
                    stage_data_directory VARCHAR(900) NULL,
                    rollback_data_directory VARCHAR(900) NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_inplace_public_id (public_id),
                    KEY idx_matrix26_inplace_status (status, requested_at),
                    KEY idx_matrix26_inplace_source (source_instance_id, requested_at),
                    KEY idx_matrix26_inplace_backup (backup_job_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_inplace_restore_step (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    inplace_restore_job_id BIGINT NOT NULL,
                    step_code VARCHAR(80) NOT NULL,
                    sequence_number INT NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_inplace_step (inplace_restore_job_id, step_code),
                    KEY idx_matrix26_inplace_step_order (inplace_restore_job_id, sequence_number),
                    CONSTRAINT fk_matrix26_inplace_step_job
                        FOREIGN KEY (inplace_restore_job_id) REFERENCES matrix26_inplace_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_inplace_restore_check (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    inplace_restore_job_id BIGINT NOT NULL,
                    check_code VARCHAR(100) NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    expected_value TEXT NULL,
                    actual_value TEXT NULL,
                    detail TEXT NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_inplace_check_job (inplace_restore_job_id, checked_at),
                    CONSTRAINT fk_matrix26_inplace_check_job
                        FOREIGN KEY (inplace_restore_job_id) REFERENCES matrix26_inplace_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_inplace_restore_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    inplace_restore_job_id BIGINT NOT NULL,
                    event_type VARCHAR(80) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    actor_username VARCHAR(120) NOT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_inplace_event_job (inplace_restore_job_id, created_at),
                    CONSTRAINT fk_matrix26_inplace_event_job
                        FOREIGN KEY (inplace_restore_job_id) REFERENCES matrix26_inplace_restore_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_job
                SET status = 'MANUAL_RECOVERY_REQUIRED',
                    last_error = COALESCE(last_error, 'Matrix26 restarted during an in-place switch or rollback.'),
                    completed_at = COALESCE(completed_at, NOW(6))
                WHERE status IN (
                    'STOPPING_RUNTIME','SWITCHING_DATABASE','SWITCHING_FILES',
                    'STARTING_RUNTIME','HEALTH_CHECKING','ROLLBACK_RUNNING'
                )
                """);
        jdbcTemplate.update("""
                UPDATE matrix26_inplace_restore_step
                SET status = 'FAILED',
                    detail = COALESCE(detail, 'Interrupted while Matrix26 was offline.'),
                    completed_at = COALESCE(completed_at, NOW(6))
                WHERE status = 'RUNNING'
                """);
    }
}
