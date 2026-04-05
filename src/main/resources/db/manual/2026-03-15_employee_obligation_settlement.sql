CREATE TABLE employee_obligation_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    employee_obligation_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    applied_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    observation VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_obligation_settlement_employee_date (employee_id, settlement_date),
    KEY idx_employee_obligation_settlement_obligation (employee_obligation_id),
    CONSTRAINT fk_employee_obligation_settlement_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT fk_employee_obligation_settlement_obligation
        FOREIGN KEY (employee_obligation_id) REFERENCES employee_obligation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
