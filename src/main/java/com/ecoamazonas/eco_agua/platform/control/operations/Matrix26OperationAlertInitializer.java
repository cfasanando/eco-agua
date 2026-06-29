package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 90)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationAlertInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26OperationAlertInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_operation_alert (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    source_key VARCHAR(180) NOT NULL,
                    source VARCHAR(40) NOT NULL,
                    severity VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    instance_code VARCHAR(80) NULL,
                    title VARCHAR(240) NOT NULL,
                    message TEXT NULL,
                    href VARCHAR(1000) NULL,
                    action_label VARCHAR(120) NULL,
                    first_detected_at DATETIME(6) NOT NULL,
                    last_detected_at DATETIME(6) NOT NULL,
                    detect_count INT NOT NULL DEFAULT 1,
                    acknowledged_at DATETIME(6) NULL,
                    acknowledged_by VARCHAR(120) NULL,
                    resolved_at DATETIME(6) NULL,
                    resolved_by VARCHAR(120) NULL,
                    ignored_at DATETIME(6) NULL,
                    ignored_by VARCHAR(120) NULL,
                    resolution_notes TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_operation_alert_source_key (source_key),
                    KEY idx_matrix26_operation_alert_status (status, severity, last_detected_at),
                    KEY idx_matrix26_operation_alert_source (source, status),
                    KEY idx_matrix26_operation_alert_instance (instance_code, status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_operation_alert_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    alert_id BIGINT NOT NULL,
                    event_type VARCHAR(80) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    actor VARCHAR(120) NOT NULL,
                    note TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_operation_alert_event_alert (alert_id, created_at),
                    CONSTRAINT fk_matrix26_operation_alert_event_alert
                        FOREIGN KEY (alert_id) REFERENCES matrix26_operation_alert(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
