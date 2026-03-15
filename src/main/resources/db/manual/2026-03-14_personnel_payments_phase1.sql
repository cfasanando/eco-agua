CREATE TABLE employee_payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    calculation_base_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_mode_snapshot VARCHAR(20) NOT NULL,
    commission_rate_snapshot DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    salary_period_snapshot VARCHAR(20) NULL,
    observation VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_payment_employee_date (employee_id, payment_date),
    KEY idx_employee_payment_period (period_year, period_month),
    CONSTRAINT fk_employee_payment_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employee_obligation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    issue_date DATE NOT NULL,
    original_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pending_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_mode VARCHAR(30) NOT NULL,
    fixed_discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    description VARCHAR(255) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_obligation_employee (employee_id),
    KEY idx_employee_obligation_active (employee_id, active),
    CONSTRAINT fk_employee_obligation_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employee_obligation_payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_payment_id BIGINT NOT NULL,
    employee_obligation_id BIGINT NOT NULL,
    applied_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    observation VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_employee_obligation_payment_payment (employee_payment_id),
    KEY idx_employee_obligation_payment_obligation (employee_obligation_id),
    CONSTRAINT fk_employee_obligation_payment_payment
        FOREIGN KEY (employee_payment_id) REFERENCES employee_payment (id),
    CONSTRAINT fk_employee_obligation_payment_obligation
        FOREIGN KEY (employee_obligation_id) REFERENCES employee_obligation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
