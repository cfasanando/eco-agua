-- Marketing featured products schema.
-- Run this once before using /marketing/admin/featured-products when spring.jpa.hibernate.ddl-auto=none.

CREATE TABLE IF NOT EXISTS marketing_featured_product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    short_text TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    display_place VARCHAR(30) NOT NULL DEFAULT 'HOME',
    priority INT NOT NULL DEFAULT 1,
    start_date DATE NULL,
    end_date DATE NULL,
    call_to_action VARCHAR(180) NULL,
    observations TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_marketing_featured_product_product (product_id),
    KEY idx_marketing_featured_product_status (status),
    KEY idx_marketing_featured_product_priority (priority),
    CONSTRAINT fk_marketing_featured_product_product
        FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
