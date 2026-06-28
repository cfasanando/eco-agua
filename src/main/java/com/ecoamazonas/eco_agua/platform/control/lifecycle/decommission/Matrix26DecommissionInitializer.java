package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 70)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26DecommissionInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26DecommissionInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_decommission_job (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    instance_name VARCHAR(180) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    reason VARCHAR(1000) NOT NULL,
                    administrative_notes TEXT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    retention_days INT NOT NULL,
                    retention_until DATETIME(6) NULL,
                    previous_instance_status VARCHAR(50) NULL,
                    resulting_instance_status VARCHAR(50) NULL,
                    final_backup_job_id BIGINT NULL,
                    final_backup_public_id VARCHAR(80) NULL,
                    final_backup_completed_at DATETIME(6) NULL,
                    final_backup_verified_at DATETIME(6) NULL,
                    final_backup_key_id VARCHAR(100) NULL,
                    final_backup_sha256 VARCHAR(64) NULL,
                    disabled_schedule_count INT NOT NULL DEFAULT 0,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_decommission_public (public_id),
                    KEY idx_matrix26_decommission_instance (instance_id, requested_at),
                    KEY idx_matrix26_decommission_status (status, requested_at),
                    KEY idx_matrix26_decommission_backup (final_backup_job_id),
                    CONSTRAINT fk_matrix26_decommission_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_decommission_check (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    decommission_job_id BIGINT NOT NULL,
                    check_code VARCHAR(100) NOT NULL,
                    label VARCHAR(180) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_decommission_check (decommission_job_id, check_code),
                    KEY idx_matrix26_decommission_check_job (decommission_job_id, checked_at),
                    CONSTRAINT fk_matrix26_decommission_check_job
                        FOREIGN KEY (decommission_job_id) REFERENCES matrix26_decommission_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_decommission_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    decommission_job_id BIGINT NOT NULL,
                    event_type VARCHAR(100) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    actor VARCHAR(120) NOT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_decommission_event_job (decommission_job_id, created_at),
                    CONSTRAINT fk_matrix26_decommission_event_job
                        FOREIGN KEY (decommission_job_id) REFERENCES matrix26_decommission_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_decommission_schedule_state (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    decommission_job_id BIGINT NOT NULL,
                    schedule_id BIGINT NOT NULL,
                    schedule_name VARCHAR(160) NOT NULL,
                    was_enabled BIT NOT NULL DEFAULT 0,
                    previous_next_run_at DATETIME(6) NULL,
                    disabled BIT NOT NULL DEFAULT 0,
                    disabled_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_decommission_schedule (decommission_job_id, schedule_id),
                    KEY idx_matrix26_decommission_schedule_id (schedule_id),
                    CONSTRAINT fk_matrix26_decommission_schedule_job
                        FOREIGN KEY (decommission_job_id) REFERENCES matrix26_decommission_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
