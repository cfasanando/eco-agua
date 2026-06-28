package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 75)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ArchiveInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26ArchiveInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_archive_record (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    decommission_job_id BIGINT NOT NULL,
                    decommission_public_id VARCHAR(80) NOT NULL,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    instance_name VARCHAR(180) NOT NULL,
                    instance_status VARCHAR(50) NOT NULL,
                    final_backup_job_id BIGINT NOT NULL,
                    final_backup_public_id VARCHAR(80) NOT NULL,
                    final_backup_sha256 VARCHAR(64) NULL,
                    final_backup_key_id VARCHAR(100) NULL,
                    final_backup_verified_at DATETIME(6) NULL,
                    retention_until DATETIME(6) NULL,
                    archive_status VARCHAR(40) NOT NULL,
                    retention_status VARCHAR(40) NOT NULL,
                    runtime_file_count INT NOT NULL DEFAULT 0,
                    data_file_count INT NOT NULL DEFAULT 0,
                    inventory_summary TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    last_verified_at DATETIME(6) NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_archive_public (public_id),
                    UNIQUE KEY uk_matrix26_archive_decommission (decommission_job_id),
                    KEY idx_matrix26_archive_instance (instance_id, archive_status),
                    KEY idx_matrix26_archive_backup (final_backup_job_id),
                    CONSTRAINT fk_matrix26_archive_decommission
                        FOREIGN KEY (decommission_job_id) REFERENCES matrix26_decommission_job(id),
                    CONSTRAINT fk_matrix26_archive_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_archive_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    archive_record_id BIGINT NOT NULL,
                    event_type VARCHAR(100) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    actor VARCHAR(120) NOT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_archive_event_record (archive_record_id, created_at),
                    CONSTRAINT fk_matrix26_archive_event_record
                        FOREIGN KEY (archive_record_id) REFERENCES matrix26_archive_record(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_archive_restore_link (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    archive_record_id BIGINT NOT NULL,
                    restore_job_id BIGINT NOT NULL,
                    restore_public_id VARCHAR(80) NOT NULL,
                    target_instance_code VARCHAR(80) NOT NULL,
                    target_database_name VARCHAR(120) NOT NULL,
                    target_runtime_profile VARCHAR(120) NOT NULL,
                    target_runtime_port INT NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    completed_at DATETIME(6) NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_archive_restore_record (archive_record_id, requested_at),
                    KEY idx_matrix26_archive_restore_job (restore_job_id),
                    CONSTRAINT fk_matrix26_archive_restore_record
                        FOREIGN KEY (archive_record_id) REFERENCES matrix26_archive_record(id)
                        ON DELETE CASCADE,
                    CONSTRAINT fk_matrix26_archive_restore_job
                        FOREIGN KEY (restore_job_id) REFERENCES matrix26_restore_job(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
