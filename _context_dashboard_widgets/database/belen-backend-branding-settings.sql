SET NAMES utf8mb4;

INSERT INTO platform_setting (variable, value, type, category, description)
VALUES
('admin.brand.title', 'Productos de la Selva Belén', 'string', 'public_site', 'Nombre mostrado en el encabezado del backend'),
('admin.brand.subtitle', 'Sistema integral', 'string', 'public_site', 'Subtítulo mostrado debajo del nombre del backend'),
('admin.brand.logo', COALESCE((SELECT value FROM (SELECT value FROM platform_setting WHERE variable = 'platform.logo' LIMIT 1) AS current_platform_logo), '/img/logo-eco.png'), 'image', 'public_site', 'Logo mostrado en el menú lateral del backend'),
('admin.home.background_image', COALESCE((SELECT value FROM (SELECT value FROM platform_setting WHERE variable = 'public.hero.background_image' LIMIT 1) AS current_hero_background), ''), 'image', 'public_site', 'Imagen de fondo del inicio administrativo'),
('admin.home.background_color', '#f3f5f9', 'string', 'public_site', 'Color de fondo del inicio administrativo'),
('admin.home.background_overlay', 'rgba(243,245,249,0.88)', 'string', 'public_site', 'Capa sobre la imagen del inicio administrativo')
ON DUPLICATE KEY UPDATE
    value = IF(value IS NULL OR value = '', VALUES(value), value),
    type = VALUES(type),
    category = VALUES(category),
    description = VALUES(description);
