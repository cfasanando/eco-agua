package com.ecoamazonas.eco_agua.restaurant;

import com.ecoamazonas.eco_agua.config.PlatformSetting;
import com.ecoamazonas.eco_agua.config.PlatformSettingRepository;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantModuleInstaller {

    private static final String MODULE_SETTING = "module.restaurant.enabled";

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSettingService platformSettingService;
    private final PlatformSettingRepository platformSettingRepository;
    private final RestaurantSettingsService restaurantSettingsService;
    private volatile Boolean installationState;

    public RestaurantModuleInstaller(JdbcTemplate jdbcTemplate,
                                     PlatformSettingService platformSettingService,
                                     PlatformSettingRepository platformSettingRepository,
                                     RestaurantSettingsService restaurantSettingsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.platformSettingService = platformSettingService;
        this.platformSettingRepository = platformSettingRepository;
        this.restaurantSettingsService = restaurantSettingsService;
    }

    public boolean isInstalled() {
        Boolean cached = installationState;
        if (cached != null) {
            return cached;
        }
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('restaurant_table', 'restaurant_order', 'restaurant_order_item', 'restaurant_table_request',
                                     'restaurant_qr_order', 'restaurant_qr_order_item', 'restaurant_reservation',
                                     'restaurant_ingredient', 'restaurant_recipe_item', 'restaurant_ingredient_movement',
                                     'restaurant_order_item_ingredient', 'restaurant_cash_session', 'restaurant_cash_movement')
                """, Integer.class);
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'restaurant_order'
                  AND column_name IN ('created_by', 'paid_by', 'kitchen_started_at', 'ready_at', 'delivered_at', 'cancelled_at')
                """, Integer.class);
        boolean installed = tableCount != null && tableCount == 13
                && columnCount != null && columnCount == 6;
        installationState = installed;
        return installed;
    }

    @Transactional
    public void installAndActivate(boolean demoData) {
        ensureInstalled(demoData);
        enable();
    }

    @Transactional
    public void ensureInstalled(boolean demoData) {
        createTables();
        ensureModuleCatalog();
        ensurePublicLabels();
        restaurantSettingsService.ensureDefaults();
        if (demoData) {
            seedDemoData();
        }
        installationState = true;
    }

    @Transactional
    public void disable() {
        setEnabled(false);
    }

    private void enable() {
        setEnabled(true);
    }

    private void setEnabled(boolean enabled) {
        PlatformSetting setting = platformSettingService.ensure(
                MODULE_SETTING,
                "false",
                "boolean",
                "system_modules",
                "Módulo Restaurante / carta digital, reservas, mesas, comandas, recetas y cocina"
        );
        setting.setValue(Boolean.toString(enabled));
        platformSettingRepository.save(setting);
    }

    private void ensurePublicLabels() {
        platformSettingService.ensure(
                "public.nav.restaurant_label",
                "Carta",
                "string",
                "public_site",
                "Etiqueta para la carta digital de restaurante"
        );
    }

    private void createTables() {
        ensureProductTableForRestaurant();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_ingredient (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(180) NOT NULL,
                    unit_code VARCHAR(20) NOT NULL DEFAULT 'UNIT',
                    unit_cost DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    stock DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    minimum_stock DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    active TINYINT(1) NOT NULL DEFAULT 1,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_ingredient_name (name),
                    KEY idx_restaurant_ingredient_active (active),
                    KEY idx_restaurant_ingredient_stock (stock, minimum_stock)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_recipe_item (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    product_id BIGINT NOT NULL,
                    ingredient_id BIGINT NOT NULL,
                    quantity DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_recipe_product_ingredient (product_id, ingredient_id),
                    KEY idx_restaurant_recipe_product (product_id),
                    KEY idx_restaurant_recipe_ingredient (ingredient_id),
                    CONSTRAINT fk_restaurant_recipe_ingredient FOREIGN KEY (ingredient_id)
                        REFERENCES restaurant_ingredient(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_table (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL,
                    name VARCHAR(120) NOT NULL,
                    area VARCHAR(120) NULL,
                    seats INT NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'FREE',
                    active TINYINT(1) NOT NULL DEFAULT 1,
                    notes TEXT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_table_code (code),
                    KEY idx_restaurant_table_status (status),
                    KEY idx_restaurant_table_area (area)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_order (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    order_code VARCHAR(80) NOT NULL,
                    service_type VARCHAR(30) NOT NULL DEFAULT 'DINE_IN',
                    table_id BIGINT NULL,
                    customer_name VARCHAR(180) NULL,
                    customer_phone VARCHAR(40) NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
                    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    service_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    delivery_address VARCHAR(255) NULL,
                    delivery_reference VARCHAR(255) NULL,
                    scheduled_at DATETIME NULL,
                    notes TEXT NULL,
                    payment_method VARCHAR(30) NULL,
                    paid_at DATETIME NULL,
                    created_by VARCHAR(120) NULL,
                    paid_by VARCHAR(120) NULL,
                    kitchen_started_at DATETIME NULL,
                    ready_at DATETIME NULL,
                    delivered_at DATETIME NULL,
                    cancelled_at DATETIME NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_order_code (order_code),
                    KEY idx_restaurant_order_status (status),
                    KEY idx_restaurant_order_created (created_at),
                    KEY idx_restaurant_order_table (table_id),
                    KEY idx_restaurant_order_service_status (service_type, status),
                    KEY idx_restaurant_order_scheduled (scheduled_at),
                    CONSTRAINT fk_restaurant_order_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        ensureRestaurantOperationalColumns();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_order_item (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    product_id BIGINT NULL,
                    product_name VARCHAR(200) NOT NULL,
                    quantity INT NOT NULL DEFAULT 1,
                    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    line_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    kitchen_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    stock_control_mode VARCHAR(20) NOT NULL DEFAULT 'PRODUCT',
                    PRIMARY KEY (id),
                    KEY idx_restaurant_order_item_order (order_id),
                    KEY idx_restaurant_order_item_product (product_id),
                    CONSTRAINT fk_restaurant_order_item_order FOREIGN KEY (order_id) REFERENCES restaurant_order(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        ensureRestaurantOrderItemStockColumns();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_order_item_ingredient (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    order_item_id BIGINT NOT NULL,
                    ingredient_id BIGINT NOT NULL,
                    ingredient_name VARCHAR(180) NOT NULL,
                    unit_code VARCHAR(20) NOT NULL DEFAULT 'UNIT',
                    quantity_per_unit DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    quantity_reserved DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_order_item_ingredient (order_item_id, ingredient_id),
                    KEY idx_restaurant_order_item_ingredient_item (order_item_id),
                    KEY idx_restaurant_order_item_ingredient_ingredient (ingredient_id),
                    CONSTRAINT fk_restaurant_order_item_ingredient_item FOREIGN KEY (order_item_id)
                        REFERENCES restaurant_order_item(id) ON DELETE CASCADE,
                    CONSTRAINT fk_restaurant_order_item_ingredient_ingredient FOREIGN KEY (ingredient_id)
                        REFERENCES restaurant_ingredient(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_ingredient_movement (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    ingredient_id BIGINT NOT NULL,
                    movement_type VARCHAR(30) NOT NULL DEFAULT 'ADJUSTMENT',
                    quantity_change DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    balance_after DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
                    order_id BIGINT NULL,
                    order_item_id BIGINT NULL,
                    notes VARCHAR(500) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_restaurant_ingredient_movement_ingredient (ingredient_id, created_at),
                    KEY idx_restaurant_ingredient_movement_order (order_id),
                    KEY idx_restaurant_ingredient_movement_item (order_item_id),
                    CONSTRAINT fk_restaurant_ingredient_movement_ingredient FOREIGN KEY (ingredient_id)
                        REFERENCES restaurant_ingredient(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        seedOpeningIngredientMovements();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_table_request (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    table_id BIGINT NOT NULL,
                    request_type VARCHAR(40) NOT NULL DEFAULT 'ATTENTION',
                    customer_note VARCHAR(500) NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    resolved_at DATETIME NULL,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_restaurant_table_request_table (table_id),
                    KEY idx_restaurant_table_request_status (status),
                    KEY idx_restaurant_table_request_type (request_type),
                    KEY idx_restaurant_table_request_created (created_at),
                    CONSTRAINT fk_restaurant_table_request_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);


        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_qr_order (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    table_id BIGINT NOT NULL,
                    customer_note VARCHAR(500) NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    approved_order_id BIGINT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    processed_at DATETIME NULL,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_restaurant_qr_order_table (table_id),
                    KEY idx_restaurant_qr_order_status (status),
                    KEY idx_restaurant_qr_order_created (created_at),
                    KEY idx_restaurant_qr_order_approved_order (approved_order_id),
                    CONSTRAINT fk_restaurant_qr_order_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_qr_order_item (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    qr_order_id BIGINT NOT NULL,
                    product_id BIGINT NULL,
                    product_name VARCHAR(200) NOT NULL,
                    quantity INT NOT NULL DEFAULT 1,
                    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    line_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    PRIMARY KEY (id),
                    KEY idx_restaurant_qr_order_item_order (qr_order_id),
                    KEY idx_restaurant_qr_order_item_product (product_id),
                    CONSTRAINT fk_restaurant_qr_order_item_order FOREIGN KEY (qr_order_id) REFERENCES restaurant_qr_order(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);


        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_reservation (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    reservation_code VARCHAR(80) NOT NULL,
                    table_id BIGINT NOT NULL,
                    customer_name VARCHAR(180) NOT NULL,
                    customer_phone VARCHAR(40) NULL,
                    reservation_at DATETIME NOT NULL,
                    duration_minutes INT NOT NULL DEFAULT 90,
                    party_size INT NOT NULL DEFAULT 1,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    notes TEXT NULL,
                    order_id BIGINT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_reservation_code (reservation_code),
                    KEY idx_restaurant_reservation_table (table_id),
                    KEY idx_restaurant_reservation_at (reservation_at),
                    KEY idx_restaurant_reservation_status (status),
                    KEY idx_restaurant_reservation_schedule (table_id, status, reservation_at),
                    KEY idx_restaurant_reservation_order (order_id),
                    CONSTRAINT fk_restaurant_reservation_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE RESTRICT,
                    CONSTRAINT fk_restaurant_reservation_order FOREIGN KEY (order_id) REFERENCES restaurant_order(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_cash_session (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    business_date DATE NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                    opening_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    opened_by VARCHAR(120) NOT NULL,
                    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    closing_amount DECIMAL(12,2) NULL,
                    expected_cash DECIMAL(12,2) NULL,
                    difference_amount DECIMAL(12,2) NULL,
                    closed_by VARCHAR(120) NULL,
                    closed_at DATETIME NULL,
                    notes VARCHAR(1000) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_restaurant_cash_session_date (business_date),
                    KEY idx_restaurant_cash_session_status (status),
                    KEY idx_restaurant_cash_session_opened (opened_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_cash_movement (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    cash_session_id BIGINT NOT NULL,
                    movement_type VARCHAR(20) NOT NULL,
                    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    description VARCHAR(500) NOT NULL,
                    created_by VARCHAR(120) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_restaurant_cash_movement_session (cash_session_id, created_at),
                    KEY idx_restaurant_cash_movement_type (movement_type),
                    CONSTRAINT fk_restaurant_cash_movement_session FOREIGN KEY (cash_session_id)
                        REFERENCES restaurant_cash_session(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }


    private void ensureRestaurantOrderItemStockColumns() {
        if (!tableExists("restaurant_order_item")) {
            return;
        }
        ensureColumn("restaurant_order_item", "stock_control_mode",
                "ALTER TABLE restaurant_order_item ADD COLUMN stock_control_mode VARCHAR(20) NOT NULL DEFAULT 'PRODUCT' AFTER kitchen_status");
    }

    private void seedOpeningIngredientMovements() {
        if (!tableExists("restaurant_ingredient") || !tableExists("restaurant_ingredient_movement")) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO restaurant_ingredient_movement
                (ingredient_id, movement_type, quantity_change, balance_after, notes, created_at)
                SELECT i.id, 'OPENING', i.stock, i.stock, 'Saldo inicial registrado al instalar el control de ingredientes', NOW()
                FROM restaurant_ingredient i
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM restaurant_ingredient_movement m
                    WHERE m.ingredient_id = i.id
                )
                """);
    }

    private void ensureRestaurantOperationalColumns() {
        if (!tableExists("restaurant_order")) {
            return;
        }
        ensureColumn("restaurant_order", "delivery_fee", "ALTER TABLE restaurant_order ADD COLUMN delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER subtotal");
        ensureColumn("restaurant_order", "service_charge", "ALTER TABLE restaurant_order ADD COLUMN service_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER delivery_fee");
        ensureColumn("restaurant_order", "delivery_address", "ALTER TABLE restaurant_order ADD COLUMN delivery_address VARCHAR(255) NULL AFTER customer_phone");
        ensureColumn("restaurant_order", "delivery_reference", "ALTER TABLE restaurant_order ADD COLUMN delivery_reference VARCHAR(255) NULL AFTER delivery_address");
        ensureColumn("restaurant_order", "scheduled_at", "ALTER TABLE restaurant_order ADD COLUMN scheduled_at DATETIME NULL AFTER delivery_reference");
        ensureColumn("restaurant_order", "payment_method", "ALTER TABLE restaurant_order ADD COLUMN payment_method VARCHAR(30) NULL AFTER notes");
        ensureColumn("restaurant_order", "paid_at", "ALTER TABLE restaurant_order ADD COLUMN paid_at DATETIME NULL AFTER payment_method");
        ensureColumn("restaurant_order", "created_by", "ALTER TABLE restaurant_order ADD COLUMN created_by VARCHAR(120) NULL AFTER paid_at");
        ensureColumn("restaurant_order", "paid_by", "ALTER TABLE restaurant_order ADD COLUMN paid_by VARCHAR(120) NULL AFTER created_by");
        ensureColumn("restaurant_order", "kitchen_started_at", "ALTER TABLE restaurant_order ADD COLUMN kitchen_started_at DATETIME NULL AFTER paid_by");
        ensureColumn("restaurant_order", "ready_at", "ALTER TABLE restaurant_order ADD COLUMN ready_at DATETIME NULL AFTER kitchen_started_at");
        ensureColumn("restaurant_order", "delivered_at", "ALTER TABLE restaurant_order ADD COLUMN delivered_at DATETIME NULL AFTER ready_at");
        ensureColumn("restaurant_order", "cancelled_at", "ALTER TABLE restaurant_order ADD COLUMN cancelled_at DATETIME NULL AFTER delivered_at");
        ensureIndex("restaurant_order", "idx_restaurant_order_paid_at",
                "ALTER TABLE restaurant_order ADD INDEX idx_restaurant_order_paid_at (paid_at)");
        ensureIndex("restaurant_order", "idx_restaurant_order_created_by",
                "ALTER TABLE restaurant_order ADD INDEX idx_restaurant_order_created_by (created_by)");
        ensureIndex("restaurant_order", "idx_restaurant_order_paid_by",
                "ALTER TABLE restaurant_order ADD INDEX idx_restaurant_order_paid_by (paid_by)");
        ensureIndex("restaurant_order", "idx_restaurant_order_service_status",
                "ALTER TABLE restaurant_order ADD INDEX idx_restaurant_order_service_status (service_type, status)");
        ensureIndex("restaurant_order", "idx_restaurant_order_scheduled",
                "ALTER TABLE restaurant_order ADD INDEX idx_restaurant_order_scheduled (scheduled_at)");
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }


    private void ensureIndex(String tableName, String indexName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }


    private void ensureProductTableForRestaurant() {
        if (!tableExists("product")) {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS product (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        name VARCHAR(200) NOT NULL,
                        description TEXT NULL,
                        image_path VARCHAR(255) NULL,
                        category_id BIGINT NULL,
                        price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                        active TINYINT(1) NOT NULL DEFAULT 1,
                        featured TINYINT(1) NOT NULL DEFAULT 0,
                        stock DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                        minimum_stock DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                        restaurant_visible TINYINT(1) NOT NULL DEFAULT 1,
                        restaurant_available TINYINT(1) NOT NULL DEFAULT 1,
                        restaurant_sort_order INT NOT NULL DEFAULT 0,
                        restaurant_stock_control VARCHAR(20) NOT NULL DEFAULT 'PRODUCT',
                        PRIMARY KEY (id),
                        KEY idx_product_active (active),
                        KEY idx_product_featured (featured),
                        KEY idx_product_category (category_id),
                        KEY idx_product_name (name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }

        ensureProductRestaurantColumns();
    }

    private void ensureProductRestaurantColumns() {
        if (!tableExists("product")) {
            return;
        }
        ensureColumn("product", "restaurant_visible", "ALTER TABLE product ADD COLUMN restaurant_visible TINYINT(1) NOT NULL DEFAULT 1 AFTER minimum_stock");
        ensureColumn("product", "restaurant_available", "ALTER TABLE product ADD COLUMN restaurant_available TINYINT(1) NOT NULL DEFAULT 1 AFTER restaurant_visible");
        ensureColumn("product", "restaurant_sort_order", "ALTER TABLE product ADD COLUMN restaurant_sort_order INT NOT NULL DEFAULT 0 AFTER restaurant_available");
        ensureColumn("product", "restaurant_stock_control", "ALTER TABLE product ADD COLUMN restaurant_stock_control VARCHAR(20) NOT NULL DEFAULT 'PRODUCT' AFTER restaurant_sort_order");
    }

    private void ensureModuleCatalog() {
        if (!tableExists("platform_module_catalog")) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO platform_module_catalog
                (`module_key`, `name`, `area`, `description`, `default_enabled`, `configurable`, `active`, `display_order`, `created_at`, `updated_at`)
                VALUES ('restaurant', 'Restaurante / carta y comandas', 'Operación restaurante',
                        'Carta digital, reservas, mesas, comandas, pedidos para llevar, delivery, ingredientes, recetas, caja, reportes e inventario por ingredientes.', 0, 1, 1, 10, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    `name` = VALUES(`name`),
                    area = VALUES(area),
                    description = VALUES(description),
                    configurable = VALUES(configurable),
                    active = VALUES(active),
                    display_order = VALUES(display_order),
                    updated_at = NOW()
                """);
    }

    private void seedDemoData() {
        seedRestaurantProducts();
        seedTables();
        seedDemoOrder();
    }

    private void seedRestaurantProducts() {
        if (!tableExists("product")) {
            return;
        }

        insertProductIfMissing("Juane amazónico", "Plato típico con arroz, gallina, huevo y aceituna.", "/img/product-default.svg", "18.00", true, "30", "5");
        insertProductIfMissing("Tacacho con cecina", "Tacacho de plátano verde acompañado de cecina regional.", "/img/product-default.svg", "22.00", true, "25", "5");
        insertProductIfMissing("Inchicapi de gallina", "Sopa tradicional amazónica con maní, yuca y gallina.", "/img/product-default.svg", "20.00", false, "20", "4");
        insertProductIfMissing("Refresco de camu camu", "Bebida fría de fruta amazónica natural.", "/img/product-default.svg", "8.00", true, "40", "8");
    }

    private void insertProductIfMissing(String name,
                                        String description,
                                        String imagePath,
                                        String price,
                                        boolean featured,
                                        String stock,
                                        String minimumStock) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE name = ?", Integer.class, name);
        if (exists != null && exists > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`, `restaurant_visible`, `restaurant_available`, `restaurant_sort_order`)
                VALUES (?, ?, ?, ?, true, ?, ?, ?, true, true, 0)
                """, name, description, imagePath, price, featured, stock, minimumStock);
    }

    private void seedTables() {
        insertTableIfMissing("MESA-01", "Mesa 01", "Salón principal", 4, "FREE", "Mesa demo cerca a caja");
        insertTableIfMissing("MESA-02", "Mesa 02", "Salón principal", 4, "OCCUPIED", "Mesa demo con comanda activa");
        insertTableIfMissing("MESA-03", "Mesa 03", "Terraza", 6, "RESERVED", "Reserva familiar de prueba");
        insertTableIfMissing("MESA-04", "Mesa 04", "Salón principal", 2, "FREE", "Mesa demo para pareja");
    }

    private void insertTableIfMissing(String code, String name, String area, int seats, String status, String notes) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM restaurant_table WHERE code = ?", Integer.class, code);
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)
                VALUES (?, ?, ?, ?, ?, true, ?)
                """, code, name, area, seats, status, notes);
    }

    private void seedDemoOrder() {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM restaurant_order WHERE order_code = 'CMD-DEMO-001'", Integer.class);
        if (exists != null && exists > 0) {
            return;
        }

        Long tableId = queryLong("SELECT id FROM restaurant_table WHERE code = 'MESA-02' LIMIT 1");
        Long productId = queryLong("SELECT id FROM product WHERE active = true ORDER BY featured DESC, id ASC LIMIT 1");
        if (tableId == null || productId == null) {
            return;
        }

        String productName = jdbcTemplate.query("SELECT name FROM product WHERE id = ?", rs -> rs.next() ? rs.getString(1) : "Plato demo", productId);
        java.math.BigDecimal price = jdbcTemplate.query("SELECT price FROM product WHERE id = ?", rs -> rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.valueOf(18), productId);

        jdbcTemplate.update("""
                INSERT INTO restaurant_order (`order_code`, `service_type`, `table_id`, `customer_name`, `customer_phone`, `status`, `subtotal`, `notes`)
                VALUES ('CMD-DEMO-001', 'DINE_IN', ?, 'Cliente demo salón', '+51966666666', 'IN_KITCHEN', ?, 'Comanda demo para cocina')
                """, tableId, price);
        Long orderId = queryLong("SELECT id FROM restaurant_order WHERE order_code = 'CMD-DEMO-001' LIMIT 1");
        if (orderId == null) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO restaurant_order_item (`order_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `line_total`, `kitchen_status`)
                VALUES (?, ?, ?, 1, ?, ?, 'PENDING')
                """, orderId, productId, productName, price, price);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private Long queryLong(String sql) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null);
    }
}
