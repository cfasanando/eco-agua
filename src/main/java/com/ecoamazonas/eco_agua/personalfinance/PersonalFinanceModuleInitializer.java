package com.ecoamazonas.eco_agua.personalfinance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "false", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 70)
public class PersonalFinanceModuleInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalFinanceModuleInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public PersonalFinanceModuleInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        upgradeDebtTable();
        ensureModuleSetting();
        LOGGER.info("GastoClaro Personal base schema is ready.");
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_income_source (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    name VARCHAR(160) NOT NULL,
                    type VARCHAR(40) NOT NULL,
                    default_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    is_active BIT NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_income_source_user_active (user_id, is_active),
                    CONSTRAINT fk_pf_income_source_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_income_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    income_source_id BIGINT NULL,
                    title VARCHAR(180) NOT NULL,
                    amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    expected_date DATE NULL,
                    received_date DATE NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_income_event_user_expected (user_id, expected_date),
                    KEY idx_pf_income_event_user_status (user_id, status),
                    KEY idx_pf_income_event_source (income_source_id),
                    CONSTRAINT fk_pf_income_event_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_pf_income_event_source FOREIGN KEY (income_source_id) REFERENCES personal_finance_income_source (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_fixed_expense (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    name VARCHAR(160) NOT NULL,
                    category VARCHAR(40) NOT NULL DEFAULT 'OTHER',
                    amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    due_day INT NULL,
                    frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
                    is_mandatory BIT NOT NULL DEFAULT 1,
                    is_active BIT NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_fixed_expense_user_active (user_id, is_active),
                    KEY idx_pf_fixed_expense_user_due (user_id, due_day),
                    CONSTRAINT fk_pf_fixed_expense_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_debt (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    debt_type VARCHAR(40) NOT NULL DEFAULT 'CREDIT_CARD',
                    name VARCHAR(160) NOT NULL,
                    creditor_name VARCHAR(160) NULL,
                    holder_type VARCHAR(40) NOT NULL DEFAULT 'OWN_NAME',
                    contact_name VARCHAR(160) NULL,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    original_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    current_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    monthly_due_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    minimum_payment DECIMAL(14,2) NULL DEFAULT 0.00,
                    interest_rate_monthly DECIMAL(8,4) NULL DEFAULT 0.0000,
                    due_day INT NULL,
                    schedule_mode VARCHAR(40) NOT NULL DEFAULT 'SIMPLE_MONTHLY',
                    schedule_start_date DATE NULL,
                    schedule_end_date DATE NULL,
                    installment_count INT NULL,
                    auto_generate_monthly BIT NOT NULL DEFAULT 1,
                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
                    has_fixed_payment BIT NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_debt_user_status (user_id, status),
                    KEY idx_pf_debt_user_due (user_id, due_day),
                    KEY idx_pf_debt_user_priority (user_id, priority),
                    KEY idx_pf_debt_user_schedule_mode (user_id, schedule_mode),
                    CONSTRAINT fk_pf_debt_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_payment_obligation (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    source_type VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
                    source_id BIGINT NULL,
                    schedule_line_id BIGINT NULL,
                    obligation_group VARCHAR(40) NOT NULL DEFAULT 'OTHER',
                    title VARCHAR(180) NOT NULL,
                    amount_due DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    amount_paid DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    due_date DATE NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_obligation_user_due (user_id, due_date),
                    KEY idx_pf_obligation_user_status (user_id, status),
                    KEY idx_pf_obligation_user_group (user_id, obligation_group),
                    KEY idx_pf_obligation_schedule_line (schedule_line_id),
                    CONSTRAINT fk_pf_obligation_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_debt_schedule_line (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    debt_id BIGINT NOT NULL,
                    line_number INT NULL,
                    line_type VARCHAR(40) NOT NULL DEFAULT 'INSTALLMENT',
                    title VARCHAR(180) NOT NULL,
                    principal_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    interest_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    fee_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    due_date DATE NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    generated_obligation_id BIGINT NULL,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_schedule_user_due (user_id, due_date),
                    KEY idx_pf_schedule_debt_due (debt_id, due_date),
                    KEY idx_pf_schedule_user_status (user_id, status),
                    KEY idx_pf_schedule_obligation (generated_obligation_id),
                    CONSTRAINT fk_pf_schedule_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_pf_schedule_debt FOREIGN KEY (debt_id) REFERENCES personal_finance_debt (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void upgradeDebtTable() {
        addColumnIfMissing("personal_finance_debt", "holder_type", "ALTER TABLE personal_finance_debt ADD COLUMN holder_type VARCHAR(40) NOT NULL DEFAULT 'OWN_NAME' AFTER creditor_name");
        addColumnIfMissing("personal_finance_debt", "contact_name", "ALTER TABLE personal_finance_debt ADD COLUMN contact_name VARCHAR(160) NULL AFTER holder_type");
        addColumnIfMissing("personal_finance_debt", "priority", "ALTER TABLE personal_finance_debt ADD COLUMN priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM' AFTER status");
        addColumnIfMissing("personal_finance_debt", "schedule_mode", "ALTER TABLE personal_finance_debt ADD COLUMN schedule_mode VARCHAR(40) NOT NULL DEFAULT 'SIMPLE_MONTHLY' AFTER due_day");
        addColumnIfMissing("personal_finance_debt", "schedule_start_date", "ALTER TABLE personal_finance_debt ADD COLUMN schedule_start_date DATE NULL AFTER schedule_mode");
        addColumnIfMissing("personal_finance_debt", "schedule_end_date", "ALTER TABLE personal_finance_debt ADD COLUMN schedule_end_date DATE NULL AFTER schedule_start_date");
        addColumnIfMissing("personal_finance_debt", "installment_count", "ALTER TABLE personal_finance_debt ADD COLUMN installment_count INT NULL AFTER schedule_end_date");
        addColumnIfMissing("personal_finance_debt", "auto_generate_monthly", "ALTER TABLE personal_finance_debt ADD COLUMN auto_generate_monthly BIT NOT NULL DEFAULT 1 AFTER installment_count");
        addColumnIfMissing("personal_finance_payment_obligation", "schedule_line_id", "ALTER TABLE personal_finance_payment_obligation ADD COLUMN schedule_line_id BIGINT NULL AFTER source_id");
        addIndexIfMissing("personal_finance_debt", "idx_pf_debt_user_priority", "CREATE INDEX idx_pf_debt_user_priority ON personal_finance_debt (user_id, priority)");
        addIndexIfMissing("personal_finance_debt", "idx_pf_debt_user_schedule_mode", "CREATE INDEX idx_pf_debt_user_schedule_mode ON personal_finance_debt (user_id, schedule_mode)");
        addIndexIfMissing("personal_finance_payment_obligation", "idx_pf_obligation_schedule_line", "CREATE INDEX idx_pf_obligation_schedule_line ON personal_finance_payment_obligation (schedule_line_id)");
    }

    private void addColumnIfMissing(String tableName, String columnName, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }

    private void ensureModuleSetting() {
        jdbcTemplate.update("""
                INSERT INTO platform_setting (variable, value, type, category, description)
                SELECT 'module.personal_finance.enabled', 'true', 'boolean', 'system_modules', 'GastoClaro personal finance module.'
                WHERE NOT EXISTS (
                    SELECT 1 FROM platform_setting WHERE variable = 'module.personal_finance.enabled'
                )
                """);
    }
}
