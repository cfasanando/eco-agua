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
        upgradeDebtScheduleTable();
        upgradeRecurringTables();
        upgradePaymentHistory();
        backfillLegacyPayments();
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
                    frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
                    expected_day INT NULL,
                    start_date DATE NULL,
                    end_date DATE NULL,
                    auto_generate_monthly BIT NOT NULL DEFAULT 1,
                    is_active BIT NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_income_source_user_active (user_id, is_active),
                    KEY idx_pf_income_source_user_recurrence (user_id, is_active, auto_generate_monthly),
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
                    start_date DATE NULL,
                    end_date DATE NULL,
                    auto_generate_monthly BIT NOT NULL DEFAULT 1,
                    is_mandatory BIT NOT NULL DEFAULT 1,
                    is_active BIT NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_fixed_expense_user_active (user_id, is_active),
                    KEY idx_pf_fixed_expense_user_due (user_id, due_day),
                    KEY idx_pf_fixed_expense_user_recurrence (user_id, is_active, auto_generate_monthly),
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
                    previous_monthly_payment DECIMAL(14,2) NULL DEFAULT 0.00,
                    last_payment_date DATE NULL,
                    delinquency_start_date DATE NULL,
                    collection_status VARCHAR(40) NOT NULL DEFAULT 'NONE',
                    negotiation_status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
                    next_review_date DATE NULL,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_debt_user_status (user_id, status),
                    KEY idx_pf_debt_user_due (user_id, due_day),
                    KEY idx_pf_debt_user_priority (user_id, priority),
                    KEY idx_pf_debt_user_schedule_mode (user_id, schedule_mode),
                    KEY idx_pf_debt_user_delinquency (user_id, delinquency_start_date),
                    KEY idx_pf_debt_user_review (user_id, next_review_date),
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
                    KEY idx_pf_obligation_source_month (user_id, source_type, source_id, due_date),
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
                    insurance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    fee_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    due_date DATE NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_principal_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_interest_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_insurance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_fee_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_penalty_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_other_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    paid_at DATE NULL,
                    generated_obligation_id BIGINT NULL,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_pf_schedule_user_due (user_id, due_date),
                    KEY idx_pf_schedule_debt_due (debt_id, due_date),
                    KEY idx_pf_schedule_user_status (user_id, status),
                    KEY idx_pf_schedule_obligation (generated_obligation_id),
                    KEY idx_pf_schedule_debt_line (debt_id, line_number),
                    CONSTRAINT fk_pf_schedule_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_pf_schedule_debt FOREIGN KEY (debt_id) REFERENCES personal_finance_debt (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_payment (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(36) NOT NULL,
                    user_id INT NOT NULL,
                    obligation_id BIGINT NULL,
                    debt_id BIGINT NULL,
                    schedule_line_id BIGINT NULL,
                    obligation_title VARCHAR(180) NOT NULL,
                    debt_name VARCHAR(160) NULL,
                    payment_date DATE NOT NULL,
                    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    principal_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    interest_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    insurance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    fee_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    penalty_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    other_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    payment_method VARCHAR(30) NOT NULL DEFAULT 'OTHER',
                    origin VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
                    operation_number VARCHAR(100) NULL,
                    recipient VARCHAR(180) NULL,
                    notes VARCHAR(1500) NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    receipt_original_name VARCHAR(255) NULL,
                    receipt_stored_path VARCHAR(500) NULL,
                    receipt_content_type VARCHAR(120) NULL,
                    receipt_size_bytes BIGINT NULL,
                    legacy_source_key VARCHAR(100) NULL,
                    reversed_at DATETIME(6) NULL,
                    reversed_by VARCHAR(120) NULL,
                    reversal_reason VARCHAR(500) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_pf_payment_public_id (public_id),
                    UNIQUE KEY uk_pf_payment_legacy_key (legacy_source_key),
                    KEY idx_pf_payment_user_date (user_id, payment_date),
                    KEY idx_pf_payment_user_status (user_id, status),
                    KEY idx_pf_payment_obligation (obligation_id),
                    KEY idx_pf_payment_debt (debt_id),
                    KEY idx_pf_payment_schedule_line (schedule_line_id),
                    CONSTRAINT fk_pf_payment_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_pf_payment_obligation FOREIGN KEY (obligation_id) REFERENCES personal_finance_payment_obligation (id) ON DELETE SET NULL,
                    CONSTRAINT fk_pf_payment_debt FOREIGN KEY (debt_id) REFERENCES personal_finance_debt (id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personal_finance_debt_negotiation (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    public_id VARCHAR(36) NOT NULL,
                    user_id INT NOT NULL,
                    debt_id BIGINT NOT NULL,
                    conversation_date DATE NOT NULL,
                    channel VARCHAR(30) NOT NULL DEFAULT 'WHATSAPP',
                    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                    contact_person VARCHAR(180) NULL,
                    currency VARCHAR(8) NOT NULL DEFAULT 'PEN',
                    creditor_requested_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    affordable_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    initial_payment_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    installment_count INT NULL,
                    proposed_installment_amount DECIMAL(14,2) NULL DEFAULT 0.00,
                    proposed_monthly_rate DECIMAL(8,4) NULL DEFAULT 0.0000,
                    first_payment_date DATE NULL,
                    response_deadline DATE NULL,
                    next_action_date DATE NULL,
                    next_action VARCHAR(500) NULL,
                    private_notes VARCHAR(2000) NULL,
                    snapshot_current_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    snapshot_monthly_payment DECIMAL(14,2) NOT NULL DEFAULT 0.00,
                    snapshot_monthly_rate DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
                    evidence_original_name VARCHAR(255) NULL,
                    evidence_stored_path VARCHAR(500) NULL,
                    evidence_content_type VARCHAR(120) NULL,
                    evidence_size_bytes BIGINT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_pf_negotiation_public_id (public_id),
                    KEY idx_pf_negotiation_user_date (user_id, conversation_date),
                    KEY idx_pf_negotiation_user_status (user_id, status),
                    KEY idx_pf_negotiation_debt_date (debt_id, conversation_date),
                    KEY idx_pf_negotiation_next_action (user_id, next_action_date),
                    CONSTRAINT fk_pf_negotiation_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_pf_negotiation_debt FOREIGN KEY (debt_id) REFERENCES personal_finance_debt (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void upgradeDebtScheduleTable() {
        addColumnIfMissing("personal_finance_debt_schedule_line", "insurance_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN insurance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER interest_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER status");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_principal_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_principal_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER paid_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_interest_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_interest_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER paid_principal_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_insurance_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_insurance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER paid_interest_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_fee_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_fee_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER paid_insurance_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_penalty_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_penalty_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER paid_fee_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_other_amount", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_other_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER paid_penalty_amount");
        addColumnIfMissing("personal_finance_debt_schedule_line", "paid_at", "ALTER TABLE personal_finance_debt_schedule_line ADD COLUMN paid_at DATE NULL AFTER paid_other_amount");
        addIndexIfMissing("personal_finance_debt_schedule_line", "idx_pf_schedule_debt_line", "CREATE INDEX idx_pf_schedule_debt_line ON personal_finance_debt_schedule_line (debt_id, line_number)");
    }

    private void upgradeRecurringTables() {
        addColumnIfMissing("personal_finance_income_source", "frequency", "ALTER TABLE personal_finance_income_source ADD COLUMN frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY' AFTER currency");
        addColumnIfMissing("personal_finance_income_source", "expected_day", "ALTER TABLE personal_finance_income_source ADD COLUMN expected_day INT NULL AFTER frequency");
        addColumnIfMissing("personal_finance_income_source", "start_date", "ALTER TABLE personal_finance_income_source ADD COLUMN start_date DATE NULL AFTER expected_day");
        addColumnIfMissing("personal_finance_income_source", "end_date", "ALTER TABLE personal_finance_income_source ADD COLUMN end_date DATE NULL AFTER start_date");
        addColumnIfMissing("personal_finance_income_source", "auto_generate_monthly", "ALTER TABLE personal_finance_income_source ADD COLUMN auto_generate_monthly BIT NOT NULL DEFAULT 1 AFTER end_date");
        addIndexIfMissing("personal_finance_income_source", "idx_pf_income_source_user_recurrence", "CREATE INDEX idx_pf_income_source_user_recurrence ON personal_finance_income_source (user_id, is_active, auto_generate_monthly)");

        addColumnIfMissing("personal_finance_fixed_expense", "start_date", "ALTER TABLE personal_finance_fixed_expense ADD COLUMN start_date DATE NULL AFTER frequency");
        addColumnIfMissing("personal_finance_fixed_expense", "end_date", "ALTER TABLE personal_finance_fixed_expense ADD COLUMN end_date DATE NULL AFTER start_date");
        addColumnIfMissing("personal_finance_fixed_expense", "auto_generate_monthly", "ALTER TABLE personal_finance_fixed_expense ADD COLUMN auto_generate_monthly BIT NOT NULL DEFAULT 1 AFTER end_date");
        addIndexIfMissing("personal_finance_fixed_expense", "idx_pf_fixed_expense_user_recurrence", "CREATE INDEX idx_pf_fixed_expense_user_recurrence ON personal_finance_fixed_expense (user_id, is_active, auto_generate_monthly)");

        addIndexIfMissing("personal_finance_payment_obligation", "idx_pf_obligation_source_month", "CREATE INDEX idx_pf_obligation_source_month ON personal_finance_payment_obligation (user_id, source_type, source_id, due_date)");
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
        addColumnIfMissing("personal_finance_debt", "previous_monthly_payment", "ALTER TABLE personal_finance_debt ADD COLUMN previous_monthly_payment DECIMAL(14,2) NULL DEFAULT 0.00 AFTER has_fixed_payment");
        addColumnIfMissing("personal_finance_debt", "last_payment_date", "ALTER TABLE personal_finance_debt ADD COLUMN last_payment_date DATE NULL AFTER previous_monthly_payment");
        addColumnIfMissing("personal_finance_debt", "delinquency_start_date", "ALTER TABLE personal_finance_debt ADD COLUMN delinquency_start_date DATE NULL AFTER last_payment_date");
        addColumnIfMissing("personal_finance_debt", "collection_status", "ALTER TABLE personal_finance_debt ADD COLUMN collection_status VARCHAR(40) NOT NULL DEFAULT 'NONE' AFTER delinquency_start_date");
        addColumnIfMissing("personal_finance_debt", "negotiation_status", "ALTER TABLE personal_finance_debt ADD COLUMN negotiation_status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED' AFTER collection_status");
        addColumnIfMissing("personal_finance_debt", "next_review_date", "ALTER TABLE personal_finance_debt ADD COLUMN next_review_date DATE NULL AFTER negotiation_status");
        addColumnIfMissing("personal_finance_payment_obligation", "schedule_line_id", "ALTER TABLE personal_finance_payment_obligation ADD COLUMN schedule_line_id BIGINT NULL AFTER source_id");
        addIndexIfMissing("personal_finance_debt", "idx_pf_debt_user_priority", "CREATE INDEX idx_pf_debt_user_priority ON personal_finance_debt (user_id, priority)");
        addIndexIfMissing("personal_finance_debt", "idx_pf_debt_user_schedule_mode", "CREATE INDEX idx_pf_debt_user_schedule_mode ON personal_finance_debt (user_id, schedule_mode)");
        addIndexIfMissing("personal_finance_debt", "idx_pf_debt_user_delinquency", "CREATE INDEX idx_pf_debt_user_delinquency ON personal_finance_debt (user_id, delinquency_start_date)");
        addIndexIfMissing("personal_finance_debt", "idx_pf_debt_user_review", "CREATE INDEX idx_pf_debt_user_review ON personal_finance_debt (user_id, next_review_date)");
        addIndexIfMissing("personal_finance_payment_obligation", "idx_pf_obligation_schedule_line", "CREATE INDEX idx_pf_obligation_schedule_line ON personal_finance_payment_obligation (schedule_line_id)");
    }

    private void upgradePaymentHistory() {
        addColumnIfMissing("personal_finance_payment", "origin", "ALTER TABLE personal_finance_payment ADD COLUMN origin VARCHAR(30) NOT NULL DEFAULT 'MANUAL' AFTER payment_method");
        addIndexIfMissing("personal_finance_payment", "idx_pf_payment_user_date", "CREATE INDEX idx_pf_payment_user_date ON personal_finance_payment (user_id, payment_date)");
        addIndexIfMissing("personal_finance_payment", "idx_pf_payment_user_status", "CREATE INDEX idx_pf_payment_user_status ON personal_finance_payment (user_id, status)");
        addIndexIfMissing("personal_finance_payment", "idx_pf_payment_obligation", "CREATE INDEX idx_pf_payment_obligation ON personal_finance_payment (obligation_id)");
        addIndexIfMissing("personal_finance_payment", "idx_pf_payment_debt", "CREATE INDEX idx_pf_payment_debt ON personal_finance_payment (debt_id)");
        addIndexIfMissing("personal_finance_payment", "idx_pf_payment_schedule_line", "CREATE INDEX idx_pf_payment_schedule_line ON personal_finance_payment (schedule_line_id)");
    }

    private void backfillLegacyPayments() {
        jdbcTemplate.update("""
                INSERT INTO personal_finance_payment (
                    public_id, user_id, obligation_id, debt_id, schedule_line_id,
                    obligation_title, debt_name, payment_date,
                    total_amount, principal_amount, interest_amount, insurance_amount,
                    fee_amount, penalty_amount, other_amount, currency, payment_method, origin,
                    notes, status, legacy_source_key, created_at, updated_at
                )
                SELECT
                    UUID(),
                    o.user_id,
                    o.id,
                    d.id,
                    o.schedule_line_id,
                    o.title,
                    d.name,
                    COALESCE(s.paid_at, o.due_date, DATE(o.updated_at), CURRENT_DATE()),
                    o.amount_paid,
                    CASE
                        WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.principal_amount
                        ELSE 0.00
                    END,
                    CASE
                        WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.interest_amount
                        ELSE 0.00
                    END,
                    CASE
                        WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.insurance_amount
                        ELSE 0.00
                    END,
                    CASE
                        WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.fee_amount
                        ELSE 0.00
                    END,
                    0.00,
                    GREATEST(0.00, o.amount_paid - (
                        CASE WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.principal_amount ELSE 0.00 END +
                        CASE WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.interest_amount ELSE 0.00 END +
                        CASE WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.insurance_amount ELSE 0.00 END +
                        CASE WHEN s.id IS NOT NULL AND s.total_amount > 0 AND o.amount_paid >= s.total_amount THEN s.fee_amount ELSE 0.00 END
                    )),
                    o.currency,
                    'OTHER',
                    'LEGACY_MIGRATION',
                    'Migrated from the previous paid amount during GastoClaro Phase 5D.',
                    'ACTIVE',
                    CONCAT('OBLIGATION:', o.id),
                    COALESCE(o.updated_at, CURRENT_TIMESTAMP(6)),
                    COALESCE(o.updated_at, CURRENT_TIMESTAMP(6))
                FROM personal_finance_payment_obligation o
                LEFT JOIN personal_finance_debt_schedule_line s ON s.id = o.schedule_line_id
                LEFT JOIN personal_finance_debt d ON d.id = CASE
                    WHEN o.source_type IN ('DEBT','DEBT_SCHEDULE','PRIVATE_LENDER_INTEREST','AUTO_DEDUCTION','DEBT_VOLUNTARY_PAYMENT') THEN o.source_id
                    ELSE NULL
                END
                WHERE o.amount_paid > 0
                  AND NOT EXISTS (
                    SELECT 1
                    FROM personal_finance_payment p
                    WHERE p.legacy_source_key = CONCAT('OBLIGATION:', o.id)
                  )
                """);

        jdbcTemplate.update("""
                INSERT INTO personal_finance_payment (
                    public_id, user_id, obligation_id, debt_id, schedule_line_id,
                    obligation_title, debt_name, payment_date,
                    total_amount, principal_amount, interest_amount, insurance_amount,
                    fee_amount, penalty_amount, other_amount, currency, payment_method, origin,
                    notes, status, legacy_source_key, created_at, updated_at
                )
                SELECT
                    UUID(),
                    s.user_id,
                    NULL,
                    s.debt_id,
                    s.id,
                    s.title,
                    d.name,
                    COALESCE(s.paid_at, s.due_date, DATE(s.updated_at), CURRENT_DATE()),
                    s.paid_amount,
                    CASE WHEN s.paid_amount >= s.total_amount THEN s.principal_amount ELSE 0.00 END,
                    CASE WHEN s.paid_amount >= s.total_amount THEN s.interest_amount ELSE 0.00 END,
                    CASE WHEN s.paid_amount >= s.total_amount THEN s.insurance_amount ELSE 0.00 END,
                    CASE WHEN s.paid_amount >= s.total_amount THEN s.fee_amount ELSE 0.00 END,
                    0.00,
                    CASE WHEN s.paid_amount >= s.total_amount
                        THEN GREATEST(0.00, s.paid_amount - s.principal_amount - s.interest_amount - s.insurance_amount - s.fee_amount)
                        ELSE s.paid_amount
                    END,
                    s.currency,
                    'OTHER',
                    'LEGACY_MIGRATION',
                    'Migrated from a legacy schedule payment during GastoClaro Phase 5D.',
                    'ACTIVE',
                    CONCAT('SCHEDULE:', s.id),
                    COALESCE(s.updated_at, CURRENT_TIMESTAMP(6)),
                    COALESCE(s.updated_at, CURRENT_TIMESTAMP(6))
                FROM personal_finance_debt_schedule_line s
                JOIN personal_finance_debt d ON d.id = s.debt_id
                WHERE s.paid_amount > 0
                  AND NOT EXISTS (
                    SELECT 1
                    FROM personal_finance_payment p
                    WHERE p.schedule_line_id = s.id
                  )
                  AND NOT EXISTS (
                    SELECT 1
                    FROM personal_finance_payment p
                    WHERE p.legacy_source_key = CONCAT('SCHEDULE:', s.id)
                  )
                """);

        jdbcTemplate.update("""
                UPDATE personal_finance_debt_schedule_line s
                LEFT JOIN (
                    SELECT
                        schedule_line_id,
                        SUM(CASE WHEN status = 'ACTIVE' THEN total_amount ELSE 0 END) AS total_paid,
                        SUM(CASE WHEN status = 'ACTIVE' THEN principal_amount ELSE 0 END) AS principal_paid,
                        SUM(CASE WHEN status = 'ACTIVE' THEN interest_amount ELSE 0 END) AS interest_paid,
                        SUM(CASE WHEN status = 'ACTIVE' THEN insurance_amount ELSE 0 END) AS insurance_paid,
                        SUM(CASE WHEN status = 'ACTIVE' THEN fee_amount ELSE 0 END) AS fee_paid,
                        SUM(CASE WHEN status = 'ACTIVE' THEN penalty_amount ELSE 0 END) AS penalty_paid,
                        SUM(CASE WHEN status = 'ACTIVE' THEN other_amount ELSE 0 END) AS other_paid,
                        MAX(CASE WHEN status = 'ACTIVE' THEN payment_date ELSE NULL END) AS last_paid_at
                    FROM personal_finance_payment
                    WHERE schedule_line_id IS NOT NULL
                    GROUP BY schedule_line_id
                ) p ON p.schedule_line_id = s.id
                SET
                    s.paid_amount = COALESCE(p.total_paid, s.paid_amount, 0.00),
                    s.paid_principal_amount = COALESCE(p.principal_paid, 0.00),
                    s.paid_interest_amount = COALESCE(p.interest_paid, 0.00),
                    s.paid_insurance_amount = COALESCE(p.insurance_paid, 0.00),
                    s.paid_fee_amount = COALESCE(p.fee_paid, 0.00),
                    s.paid_penalty_amount = COALESCE(p.penalty_paid, 0.00),
                    s.paid_other_amount = COALESCE(p.other_paid, 0.00),
                    s.paid_at = COALESCE(p.last_paid_at, s.paid_at)
                WHERE p.schedule_line_id IS NOT NULL
                """);
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
