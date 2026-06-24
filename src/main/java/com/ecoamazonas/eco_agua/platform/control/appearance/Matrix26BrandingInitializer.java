package com.ecoamazonas.eco_agua.platform.control.appearance;

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
public class Matrix26BrandingInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26BrandingInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_instance_branding_draft (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    display_name VARCHAR(180) NOT NULL,
                    short_name VARCHAR(100) NOT NULL,
                    tagline VARCHAR(220) NULL,
                    welcome_message VARCHAR(300) NULL,
                    hero_title VARCHAR(220) NULL,
                    hero_subtitle VARCHAR(500) NULL,
                    primary_cta_label VARCHAR(80) NULL,
                    secondary_cta_label VARCHAR(80) NULL,
                    contact_phone VARCHAR(80) NULL,
                    whatsapp VARCHAR(40) NULL,
                    location VARCHAR(160) NULL,
                    reason VARCHAR(500) NULL,
                    updated_by VARCHAR(120) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_branding_draft_instance (instance_id),
                    CONSTRAINT fk_matrix26_branding_draft_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client (id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_instance_branding_asset (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    asset_type VARCHAR(60) NOT NULL,
                    relative_path VARCHAR(500) NOT NULL,
                    original_name VARCHAR(255) NOT NULL,
                    content_type VARCHAR(100) NOT NULL,
                    extension VARCHAR(12) NOT NULL,
                    size_bytes BIGINT NOT NULL,
                    width_px INT NULL,
                    height_px INT NULL,
                    sha256 VARCHAR(64) NOT NULL,
                    updated_by VARCHAR(120) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_branding_asset_instance_type (instance_id, asset_type),
                    CONSTRAINT fk_matrix26_branding_asset_instance
                        FOREIGN KEY (instance_id) REFERENCES platform_business_client (id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
