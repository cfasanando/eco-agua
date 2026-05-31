-- Run this only on the Productos de la Selva Belén database.
-- It updates public website texts after cloning a database from Agua Eco.

INSERT INTO platform_setting (variable, value, type, category, description) VALUES
('platform.name', 'Productos de la Selva Belén', 'string', 'platform', 'Nombre comercial mostrado en la web'),
('platform.tagline', 'Productos amazónicos', 'string', 'platform', 'Lema debajo del nombre'),
('platform.logo', '/img/logo3-transparente.png', 'string', 'public_site', 'Ruta del logo público'),
('public.topbar.location', 'Atención en Iquitos y alrededores', 'string', 'public_site', 'Texto de ubicación en la barra superior'),
('public.topbar.phone', '(065) 000000', 'string', 'public_site', 'Número de central en el top bar'),
('public.topbar.whatsapp_label', 'Consultas por WhatsApp', 'string', 'public_site', 'Texto del enlace de WhatsApp en el top bar'),
('public.whatsapp.number', '51900000000', 'string', 'public_site', 'Número para pedidos por WhatsApp'),
('public.footer.right', 'Productos amazónicos seleccionados en Iquitos, Perú', 'string', 'public_site', 'Texto footer derecha'),
('public.hero.pill', 'Productos amazónicos frescos y seleccionados', 'string', 'public_site', 'Texto del pill en el hero'),
('public.hero.title', 'Productos de la Selva Belén para tu hogar o negocio', 'string', 'public_site', 'Título principal del hero'),
('public.hero.subtitle', 'Encuentra pescados, paiche seco, hojas de bijao, hojas de plátano y otros productos de la selva según disponibilidad.', 'text', 'public_site', 'Subtítulo del hero'),
('public.hero.bullet_1', 'Productos reales y confirmados según stock disponible.', 'string', 'public_site', 'Bullet 1 del hero'),
('public.hero.bullet_2', 'Atención directa para coordinar pedidos y entregas.', 'string', 'public_site', 'Bullet 2 del hero'),
('public.hero.bullet_3', 'Consultas rápidas por WhatsApp.', 'string', 'public_site', 'Bullet 3 del hero'),
('public.hero.primary_cta_label', 'Consultar por WhatsApp', 'string', 'public_site', 'Texto del botón principal del hero'),
('public.hero.secondary_cta_label', 'Ver catálogo de productos', 'string', 'public_site', 'Texto del botón secundario del hero'),
('public.hero.stat_1', 'Productos de la selva', 'string', 'public_site', 'Estadística 1 del hero'),
('public.hero.stat_2', 'Atención en Iquitos', 'string', 'public_site', 'Estadística 2 del hero'),
('public.hero.stat_3', 'Confianza y disponibilidad', 'string', 'public_site', 'Estadística 3 del hero'),
('public.hero.card_title', 'Productos de la Selva Belén', 'string', 'public_site', 'Título de la tarjeta del hero'),
('public.hero.card_subtitle', 'Pescados, hojas y productos amazónicos', 'string', 'public_site', 'Subtítulo de la tarjeta del hero'),
('public.hero.badge_label', 'Productos destacados', 'string', 'public_site', 'Texto de la etiqueta del hero'),
('public.final_cta.button_label', 'Consultar disponibilidad', 'string', 'public_site', 'Texto del botón del bloque final'),
('public.final_cta.schedule', 'Atención según disponibilidad de productos y coordinación por WhatsApp.', 'string', 'public_site', 'Texto de horario del bloque final')
ON DUPLICATE KEY UPDATE
value = VALUES(value),
type = VALUES(type),
category = VALUES(category),
description = VALUES(description);
