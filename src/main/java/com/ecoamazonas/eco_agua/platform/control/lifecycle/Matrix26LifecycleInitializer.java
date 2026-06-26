package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26LifecycleInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26LifecycleInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_lifecycle_job (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    instance_name VARCHAR(180) NOT NULL,
                    action VARCHAR(30) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    reason VARCHAR(1000) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    previous_instance_status VARCHAR(50) NULL,
                    resulting_instance_status VARCHAR(50) NULL,
                    runtime_was_running BIT NOT NULL DEFAULT 0,
                    paused_schedule_count INT NOT NULL DEFAULT 0,
                    verified_backup_job_id BIGINT NULL,
                    verified_backup_public_id VARCHAR(80) NULL,
                    verified_backup_completed_at DATETIME(6) NULL,
                    related_lifecycle_job_id BIGINT NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_lifecycle_job_public (public_id),
                    KEY idx_matrix26_lifecycle_job_instance (instance_id, requested_at),
                    KEY idx_matrix26_lifecycle_job_status (status, requested_at),
                    KEY idx_matrix26_lifecycle_job_related (related_lifecycle_job_id),
                    CONSTRAINT fk_matrix26_lifecycle_job_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_lifecycle_schedule_state (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    lifecycle_job_id BIGINT NOT NULL,
                    schedule_id BIGINT NOT NULL,
                    schedule_name VARCHAR(160) NOT NULL,
                    was_enabled BIT NOT NULL DEFAULT 1,
                    previous_next_run_at DATETIME(6) NULL,
                    restored BIT NOT NULL DEFAULT 0,
                    restored_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_lifecycle_schedule_job (lifecycle_job_id, schedule_id),
                    KEY idx_matrix26_lifecycle_schedule_id (schedule_id),
                    CONSTRAINT fk_matrix26_lifecycle_schedule_job
                        FOREIGN KEY (lifecycle_job_id) REFERENCES matrix26_lifecycle_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_lifecycle_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    lifecycle_job_id BIGINT NOT NULL,
                    event_type VARCHAR(80) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    actor VARCHAR(120) NOT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_lifecycle_event_job (lifecycle_job_id, created_at),
                    CONSTRAINT fk_matrix26_lifecycle_event_job
                        FOREIGN KEY (lifecycle_job_id) REFERENCES matrix26_lifecycle_job(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
