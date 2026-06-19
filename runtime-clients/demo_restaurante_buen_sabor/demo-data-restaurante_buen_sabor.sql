-- Datos demo generados desde Super Admin para: Restaurante El Buen Sabor
-- Plantilla detectada: restaurante restaurante / comida restaurante restaurante demo-restaurante-buen-sabor restaurante el buen sabor
-- Ejecutar después del bootstrap inicial.

USE `restaurante_buen_sabor`;

SET @demo_now = NOW();

INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`) VALUES ('public.hero.badge1', 'Carta digital', 'string', 'public_site', 'Beneficio público demo') ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `type` = VALUES(`type`), `category` = VALUES(`category`), `description` = VALUES(`description`);
INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`) VALUES ('public.hero.badge2', 'Pedidos para llevar', 'string', 'public_site', 'Beneficio público demo') ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `type` = VALUES(`type`), `category` = VALUES(`category`), `description` = VALUES(`description`);
INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`) VALUES ('public.hero.badge3', 'Delivery coordinado', 'string', 'public_site', 'Beneficio público demo') ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `type` = VALUES(`type`), `category` = VALUES(`category`), `description` = VALUES(`description`);
-- Productos demo.
INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Juane amazónico especial', 'Plato típico con arroz, presa y sazón regional. Ideal para carta digital.', '/img/catalog/restaurant-juane.jpg', 18.00, true, true, 30, 5
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Juane amazónico especial');
INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Tacacho con cecina', 'Plato fuerte regional para salón, delivery y promociones.', '/img/catalog/restaurant-tacacho.jpg', 25.00, true, true, 25, 5
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Tacacho con cecina');
INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Refresco de camu camu', 'Bebida natural amazónica para acompañar el menú.', '/img/catalog/restaurant-camu-camu.jpg', 7.00, true, false, 50, 10
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Refresco de camu camu');
INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Combo familiar amazónico', 'Combo de platos regionales para compartir y vender por WhatsApp.', '/img/catalog/restaurant-combo.jpg', 69.00, true, true, 12, 3
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Combo familiar amazónico');

-- Clientes demo.
INSERT INTO client (`name`, `doc_type`, `doc_number`, `address`, `reference`, `phone`, `active`, `registration_date`, `latitude`, `longitude`)
SELECT 'Mesa demo 01', 'DNI', '71000001', 'Salón principal', 'Cliente para prueba de comanda', '+51944444444', true, @demo_now, -3.7487000, -73.2479000
WHERE NOT EXISTS (SELECT 1 FROM client WHERE `doc_number` = '71000001');
INSERT INTO client (`name`, `doc_type`, `doc_number`, `address`, `reference`, `phone`, `active`, `registration_date`, `latitude`, `longitude`)
SELECT 'Delivery Oficina Centro', 'DNI', '71000002', 'Calle Putumayo 640', 'Pedido para almuerzo', '+51955555555', true, @demo_now, -3.7468000, -73.2439000
WHERE NOT EXISTS (SELECT 1 FROM client WHERE `doc_number` = '71000002');
INSERT INTO client (`name`, `doc_type`, `doc_number`, `address`, `reference`, `phone`, `active`, `registration_date`, `latitude`, `longitude`)
SELECT 'Cliente frecuente familiar', 'DNI', '71000003', 'Av. Quiñones 1800', 'Compra combos fines de semana', '+51966666666', true, @demo_now, -3.7670000, -73.2825000
WHERE NOT EXISTS (SELECT 1 FROM client WHERE `doc_number` = '71000003');

-- Zonas demo de entrega.
INSERT INTO delivery_zone (`name`, `latitude`, `longitude`, `radius_meters`, `note`, `created_at`, `updated_at`)
SELECT 'Centro Iquitos', -3.7481000, -73.2448000, 2500, 'Zona demo para entregas céntricas', @demo_now, @demo_now
WHERE NOT EXISTS (SELECT 1 FROM delivery_zone WHERE `name` = 'Centro Iquitos');
INSERT INTO delivery_zone (`name`, `latitude`, `longitude`, `radius_meters`, `note`, `created_at`, `updated_at`)
SELECT 'San Juan / Aeropuerto', -3.7840000, -73.3080000, 3500, 'Zona demo para entregas extendidas', @demo_now, @demo_now
WHERE NOT EXISTS (SELECT 1 FROM delivery_zone WHERE `name` = 'San Juan / Aeropuerto');

-- Marketing demo.
INSERT INTO marketing_campaign_calendar (`name`, `type`, `status`, `start_date`, `end_date`, `channel`, `target_segment`, `objective`, `main_message`, `next_action`, `observations`, `created_at`, `updated_at`)
SELECT 'Menú del día y combos familiares', 'WHATSAPP', 'PLANNED', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'WhatsApp / Redes sociales', 'Clientes potenciales', 'Publicar plato del día a las 10:30 a. m. y reforzar combos por WhatsApp.', 'Hoy cocina regional lista para llevar. Reserva tu plato por WhatsApp.', 'Pedir menú de hoy', 'Dato demo generado por plantilla.', @demo_now, @demo_now
WHERE NOT EXISTS (SELECT 1 FROM marketing_campaign_calendar WHERE `name` = 'Menú del día y combos familiares');
INSERT INTO marketing_content_idea (`title`, `channel`, `content_type`, `status`, `priority`, `suggested_date`, `target_segment`, `hook`, `main_message`, `call_to_action`, `next_action`, `observations`, `created_at`, `updated_at`)
SELECT 'Idea demo: Menú del día y combos familiares', 'WHATSAPP', 'SHORT_VIDEO', 'NEW', 'HIGH', CURDATE(), 'Clientes potenciales', 'Hoy cocina regional lista para llevar. Reserva tu plato por WhatsApp.', 'Publicar plato del día a las 10:30 a. m. y reforzar combos por WhatsApp.', 'Pedir menú de hoy', 'Crear pieza visual y publicar.', 'Dato demo generado por plantilla.', @demo_now, @demo_now
WHERE NOT EXISTS (SELECT 1 FROM marketing_content_idea WHERE `title` = 'Idea demo: Menú del día y combos familiares');


-- Marca interna para auditoría del aprovisionamiento demo.
INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`) VALUES ('platform.demo.loaded', 'true', 'boolean', 'platform', 'Indica que los datos demo de plantilla fueron cargados') ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `type` = VALUES(`type`), `category` = VALUES(`category`), `description` = VALUES(`description`);
INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`) VALUES ('platform.demo.template', 'restaurante restaurante / comida restaurante restaurante demo-restaurante-buen-sabor restaurante el buen sabor', 'string', 'platform', 'Plantilla usada para cargar datos demo') ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `type` = VALUES(`type`), `category` = VALUES(`category`), `description` = VALUES(`description`);


-- Restaurante runtime fallback generated by restaurant links hotfix.
-- Repair current Restaurante El Buen Sabor runtime.
-- Safe for the current restaurant database only. It does not touch Eco Agua or Productos de la Selva.

USE `restaurante_buen_sabor`;

-- Main module flag used by SystemModuleAccessFilter and sidebar.
INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`)
VALUES
  ('module.restaurant.enabled', 'true', 'boolean', 'system_modules', 'Módulo Restaurante / carta digital, mesas, comandas y cocina'),
  ('module.restaurant_tables.enabled', 'true', 'boolean', 'system_modules', 'Módulo Mesas y salón'),
  ('module.restaurant_kitchen.enabled', 'true', 'boolean', 'system_modules', 'Módulo Comandas y cocina'),
  ('module.restaurant_menu_qr.enabled', 'true', 'boolean', 'system_modules', 'Módulo Carta digital QR'),
  ('public.nav.restaurant_label', 'Carta', 'string', 'public_site', 'Etiqueta para la carta digital de restaurante')
ON DUPLICATE KEY UPDATE
  `value` = VALUES(`value`),
  `type` = VALUES(`type`),
  `category` = VALUES(`category`),
  `description` = VALUES(`description`);

-- Product table fallback for isolated restaurant runtimes.
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
    PRIMARY KEY (id),
    KEY idx_product_active (active),
    KEY idx_product_featured (featured),
    KEY idx_product_category (category_id),
    KEY idx_product_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS restaurant_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_code VARCHAR(80) NOT NULL,
    service_type VARCHAR(30) NOT NULL DEFAULT 'DINE_IN',
    table_id BIGINT NULL,
    customer_name VARCHAR(180) NULL,
    customer_phone VARCHAR(40) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_restaurant_order_code (order_code),
    KEY idx_restaurant_order_status (status),
    KEY idx_restaurant_order_created (created_at),
    KEY idx_restaurant_order_table (table_id),
    CONSTRAINT fk_restaurant_order_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS restaurant_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    kitchen_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    KEY idx_restaurant_order_item_order (order_id),
    KEY idx_restaurant_order_item_product (product_id),
    CONSTRAINT fk_restaurant_order_item_order FOREIGN KEY (order_id) REFERENCES restaurant_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Juane amazónico', 'Plato típico con arroz, gallina, huevo y aceituna.', '/img/product-default.svg', 18.00, true, true, 30, 5
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Juane amazónico');

INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Tacacho con cecina', 'Tacacho de plátano verde acompañado de cecina regional.', '/img/product-default.svg', 22.00, true, true, 25, 5
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Tacacho con cecina');

INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Inchicapi de gallina', 'Sopa tradicional amazónica con maní, yuca y gallina.', '/img/product-default.svg', 20.00, true, false, 20, 4
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Inchicapi de gallina');

INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)
SELECT 'Refresco de camu camu', 'Bebida fría de fruta amazónica natural.', '/img/product-default.svg', 8.00, true, false, 40, 8
WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = 'Refresco de camu camu');

INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)
SELECT 'MESA-01', 'Mesa 01', 'Salón principal', 4, 'FREE', true, 'Mesa demo cerca a caja'
WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-01');

INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)
SELECT 'MESA-02', 'Mesa 02', 'Salón principal', 4, 'OCCUPIED', true, 'Mesa demo con comanda activa'
WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-02');

INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)
SELECT 'MESA-03', 'Mesa 03', 'Terraza', 6, 'RESERVED', true, 'Reserva familiar de prueba'
WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-03');

INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)
SELECT 'MESA-04', 'Mesa 04', 'Salón principal', 2, 'FREE', true, 'Mesa demo para pareja'
WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-04');

SET @restaurant_demo_table_id = (SELECT id FROM restaurant_table WHERE code = 'MESA-02' LIMIT 1);
SET @restaurant_demo_product_id = (SELECT id FROM product WHERE active = true ORDER BY featured DESC, id ASC LIMIT 1);
SET @restaurant_demo_product_name = (SELECT name FROM product WHERE id = @restaurant_demo_product_id LIMIT 1);
SET @restaurant_demo_product_price = (SELECT price FROM product WHERE id = @restaurant_demo_product_id LIMIT 1);

INSERT INTO restaurant_order (`order_code`, `service_type`, `table_id`, `customer_name`, `customer_phone`, `status`, `subtotal`, `notes`)
SELECT 'CMD-DEMO-001', 'DINE_IN', @restaurant_demo_table_id, 'Cliente demo salón', '+51966666666', 'IN_KITCHEN', COALESCE(@restaurant_demo_product_price, 18.00), 'Comanda demo para cocina'
WHERE @restaurant_demo_table_id IS NOT NULL
  AND @restaurant_demo_product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM restaurant_order WHERE order_code = 'CMD-DEMO-001');

SET @restaurant_demo_order_id = (SELECT id FROM restaurant_order WHERE order_code = 'CMD-DEMO-001' LIMIT 1);

INSERT INTO restaurant_order_item (`order_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `line_total`, `kitchen_status`)
SELECT @restaurant_demo_order_id, @restaurant_demo_product_id, COALESCE(@restaurant_demo_product_name, 'Plato demo'), 1, COALESCE(@restaurant_demo_product_price, 18.00), COALESCE(@restaurant_demo_product_price, 18.00), 'PENDING'
WHERE @restaurant_demo_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM restaurant_order_item WHERE order_id = @restaurant_demo_order_id);

SELECT 'restaurant runtime repair completed' AS status;
