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
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    original_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    current_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    monthly_due_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    minimum_payment DECIMAL(14,2) NULL DEFAULT 0.00,
                    interest_rate_monthly DECIMAL(8,4) NULL DEFAULT 0.0000,
                    due_day INT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                    has_fixed_payment BIT NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_debt_user_status (user_id, status),
                    KEY idx_pf_debt_user_due (user_id, due_day),
                    CONSTRAINT fk_pf_debt_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
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
