package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RuntimeControlInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26RuntimeControlInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_runtime_state (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    runtime_key VARCHAR(80) NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    current_state VARCHAR(30) NOT NULL,
                    last_known_pid BIGINT NULL,
                    process_started_at DATETIME(6) NULL,
                    last_online_at DATETIME(6) NULL,
                    last_stopped_at DATETIME(6) NULL,
                    last_operation_id BIGINT NULL,
                    standard_log_path VARCHAR(500) NULL,
                    error_log_path VARCHAR(500) NULL,
                    pid_file_path VARCHAR(500) NULL,
                    message VARCHAR(500) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_runtime_state_instance (instance_id),
                    UNIQUE KEY idx_matrix26_runtime_state_code (instance_code),
                    UNIQUE KEY idx_matrix26_runtime_state_runtime_key (runtime_key),
                    CONSTRAINT fk_matrix26_runtime_state_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_runtime_operation (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    runtime_key VARCHAR(80) NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    action VARCHAR(30) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    previous_pid BIGINT NULL,
                    resulting_pid BIGINT NULL,
                    runtime_port INT NULL,
                    initial_state VARCHAR(40) NULL,
                    final_state VARCHAR(40) NULL,
                    duration_ms BIGINT NULL,
                    message VARCHAR(500) NULL,
                    error_detail TEXT NULL,
                    standard_log_path VARCHAR(500) NULL,
                    error_log_path VARCHAR(500) NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_runtime_operation_instance (instance_id, requested_at),
                    KEY idx_matrix26_runtime_operation_status (status, requested_at),
                    CONSTRAINT fk_matrix26_runtime_operation_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
