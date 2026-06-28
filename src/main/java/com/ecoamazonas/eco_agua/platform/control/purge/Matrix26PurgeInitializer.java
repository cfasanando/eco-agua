package com.ecoamazonas.eco_agua.platform.control.purge;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 80)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26PurgeInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26PurgeInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_purge_plan (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(80) NOT NULL,
                    archive_record_id BIGINT NOT NULL,
                    archive_public_id VARCHAR(80) NOT NULL,
                    decommission_job_id BIGINT NULL,
                    instance_id BIGINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    instance_name VARCHAR(180) NOT NULL,
                    database_name VARCHAR(120) NULL,
                    runtime_profile VARCHAR(120) NULL,
                    runtime_port INT NULL,
                    final_backup_job_id BIGINT NULL,
                    final_backup_public_id VARCHAR(80) NULL,
                    final_backup_sha256 VARCHAR(64) NULL,
                    retention_until DATETIME(6) NULL,
                    retention_status VARCHAR(60) NULL,
                    status VARCHAR(40) NOT NULL,
                    run_number INT NOT NULL DEFAULT 1,
                    eligible_for_future_purge TINYINT(1) NOT NULL DEFAULT 0,
                    blockers_count INT NOT NULL DEFAULT 0,
                    would_delete_count INT NOT NULL DEFAULT 0,
                    would_keep_count INT NOT NULL DEFAULT 0,
                    protected_count INT NOT NULL DEFAULT 0,
                    review_count INT NOT NULL DEFAULT 0,
                    not_found_count INT NOT NULL DEFAULT 0,
                    reason TEXT NOT NULL,
                    requested_by VARCHAR(120) NOT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    evaluated_at DATETIME(6) NULL,
                    last_error TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_purge_public (public_id),
                    KEY idx_matrix26_purge_instance (instance_id, status),
                    KEY idx_matrix26_purge_archive (archive_record_id),
                    KEY idx_matrix26_purge_final_backup (final_backup_job_id),
                    CONSTRAINT fk_matrix26_purge_archive
                        FOREIGN KEY (archive_record_id) REFERENCES matrix26_archive_record(id),
                    CONSTRAINT fk_matrix26_purge_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_purge_item (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    purge_plan_id BIGINT NOT NULL,
                    run_number INT NOT NULL,
                    resource_type VARCHAR(80) NOT NULL,
                    resource_name VARCHAR(255) NULL,
                    resource_path VARCHAR(1000) NULL,
                    disposition VARCHAR(40) NOT NULL,
                    size_bytes BIGINT NULL,
                    file_count INT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    execution_status VARCHAR(40) NULL,
                    executed_at DATETIME(6) NULL,
                    execution_detail TEXT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_purge_item_plan (purge_plan_id, run_number, disposition),
                    KEY idx_matrix26_purge_item_execution (purge_plan_id, run_number, execution_status),
                    CONSTRAINT fk_matrix26_purge_item_plan
                        FOREIGN KEY (purge_plan_id) REFERENCES matrix26_purge_plan(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        addColumnIfMissing("matrix26_purge_item", "execution_status", "VARCHAR(40) NULL");
        addColumnIfMissing("matrix26_purge_item", "executed_at", "DATETIME(6) NULL");
        addColumnIfMissing("matrix26_purge_item", "execution_detail", "TEXT NULL");
        addIndexIfMissing("matrix26_purge_item", "idx_matrix26_purge_item_execution",
                "CREATE INDEX idx_matrix26_purge_item_execution ON matrix26_purge_item (purge_plan_id, run_number, execution_status)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_purge_check (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    purge_plan_id BIGINT NOT NULL,
                    run_number INT NOT NULL,
                    check_code VARCHAR(100) NOT NULL,
                    label VARCHAR(220) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    detail TEXT NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_purge_check_plan (purge_plan_id, run_number, status),
                    CONSTRAINT fk_matrix26_purge_check_plan
                        FOREIGN KEY (purge_plan_id) REFERENCES matrix26_purge_plan(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_purge_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    purge_plan_id BIGINT NOT NULL,
                    event_type VARCHAR(100) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    actor VARCHAR(120) NOT NULL,
                    detail TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_purge_event_plan (purge_plan_id, created_at),
                    CONSTRAINT fk_matrix26_purge_event_plan
                        FOREIGN KEY (purge_plan_id) REFERENCES matrix26_purge_plan(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void addIndexIfMissing(String table, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, table, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
