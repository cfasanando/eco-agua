-- Productos de la Selva Belén: monthly promotions and Lima Este delivery zones
-- Safe seed for public promotions and delivery section.

SET NAMES utf8mb4;

DELETE FROM promotion
WHERE name = 'Consulta por temporada'
   OR promo_number BETWEEN 202601 AND 202612
   OR promo_number BETWEEN 20260101 AND 20261299
   OR banner_image_path LIKE '/uploads/belen/promotions/2026-%';

INSERT INTO promotion
(name, description, start_date, end_date, promo_number, enabled, max_counter, color_border, created_at, updated_at, banner_image_path)
VALUES
('Enero fresco','Frutas amazónicas para iniciar el año. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-01-01','2026-01-31',20260101,1,NULL,'#7c2d12',NOW(),NOW(),'/uploads/belen/promotions/2026-01-1-enero-fresco.jpg'),
('Enero familiar','Productos para la despensa de casa. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-01-01','2026-01-31',20260102,1,NULL,'#0f172a',NOW(),NOW(),'/uploads/belen/promotions/2026-01-2-enero-familiar.jpg'),
('Enero mayorista','Consulta productos por volumen. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-01-01','2026-01-31',20260103,1,NULL,'#365314',NOW(),NOW(),'/uploads/belen/promotions/2026-01-3-enero-mayorista.jpg'),
('Febrero de sabores','Productos para reuniones y loncheras. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-02-01','2026-02-28',20260201,1,NULL,'#0f172a',NOW(),NOW(),'/uploads/belen/promotions/2026-02-1-febrero-de-sabores.jpg'),
('Febrero para negocios','Abastecimiento para restaurantes. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-02-01','2026-02-28',20260202,1,NULL,'#365314',NOW(),NOW(),'/uploads/belen/promotions/2026-02-2-febrero-para-negocios.jpg'),
('Febrero por mayor','Coordina compras recurrentes. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-02-01','2026-02-28',20260203,1,NULL,'#581c87',NOW(),NOW(),'/uploads/belen/promotions/2026-02-3-febrero-por-mayor.jpg'),
('Marzo escolar','Opciones prácticas para casa. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-03-01','2026-03-31',20260301,1,NULL,'#365314',NOW(),NOW(),'/uploads/belen/promotions/2026-03-1-marzo-escolar.jpg'),
('Marzo restaurante','Productos de la selva para tu menú. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-03-01','2026-03-31',20260302,1,NULL,'#581c87',NOW(),NOW(),'/uploads/belen/promotions/2026-03-2-marzo-restaurante.jpg'),
('Marzo ahorro','Compra planificada por WhatsApp. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-03-01','2026-03-31',20260303,1,NULL,'#064e3b',NOW(),NOW(),'/uploads/belen/promotions/2026-03-3-marzo-ahorro.jpg'),
('Abril de pescado','Pescado seco y paiche según stock. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-04-01','2026-04-30',20260401,1,NULL,'#581c87',NOW(),NOW(),'/uploads/belen/promotions/2026-04-1-abril-de-pescado.jpg'),
('Abril familiar','Productos para cocinar en casa. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-04-01','2026-04-30',20260402,1,NULL,'#064e3b',NOW(),NOW(),'/uploads/belen/promotions/2026-04-2-abril-familiar.jpg'),
('Abril negocio','Atención a restaurantes y bodegas. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-04-01','2026-04-30',20260403,1,NULL,'#166534',NOW(),NOW(),'/uploads/belen/promotions/2026-04-3-abril-negocio.jpg'),
('Mayo familiar','Canasta amazónica para casa o regalo. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-05-01','2026-05-31',20260501,1,NULL,'#064e3b',NOW(),NOW(),'/uploads/belen/promotions/2026-05-1-mayo-familiar.jpg'),
('Mayo para mamá','Sabores de la selva para compartir. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-05-01','2026-05-31',20260502,1,NULL,'#166534',NOW(),NOW(),'/uploads/belen/promotions/2026-05-2-mayo-para-mama.jpg'),
('Mayo mayorista','Pedidos para restaurantes y negocios. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-05-01','2026-05-31',20260503,1,NULL,'#7c2d12',NOW(),NOW(),'/uploads/belen/promotions/2026-05-3-mayo-mayorista.jpg'),
('Junio San Juan','Hojas y productos para juanes. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-06-01','2026-06-30',20260601,1,NULL,'#166534',NOW(),NOW(),'/uploads/belen/promotions/2026-06-1-junio-san-juan.jpg'),
('Junio tradición','Productos amazónicos de temporada. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-06-01','2026-06-30',20260602,1,NULL,'#7c2d12',NOW(),NOW(),'/uploads/belen/promotions/2026-06-2-junio-tradicion.jpg'),
('Junio por encargo','Coordina tu pedido con anticipación. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-06-01','2026-06-30',20260603,1,NULL,'#0f172a',NOW(),NOW(),'/uploads/belen/promotions/2026-06-3-junio-por-encargo.jpg'),
('Julio patrio','Sabores de la selva para celebrar. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-07-01','2026-07-31',20260701,1,NULL,'#7c2d12',NOW(),NOW(),'/uploads/belen/promotions/2026-07-1-julio-patrio.jpg'),
('Julio restaurante','Insumos amazónicos para tu carta. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-07-01','2026-07-31',20260702,1,NULL,'#0f172a',NOW(),NOW(),'/uploads/belen/promotions/2026-07-2-julio-restaurante.jpg'),
('Julio familia','Productos para reuniones en casa. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-07-01','2026-07-31',20260703,1,NULL,'#365314',NOW(),NOW(),'/uploads/belen/promotions/2026-07-3-julio-familia.jpg'),
('Agosto ahumado','Cecina, chorizo y productos con sabor. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-08-01','2026-08-31',20260801,1,NULL,'#0f172a',NOW(),NOW(),'/uploads/belen/promotions/2026-08-1-agosto-ahumado.jpg'),
('Agosto seco','Despensa amazónica para el mes. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-08-01','2026-08-31',20260802,1,NULL,'#365314',NOW(),NOW(),'/uploads/belen/promotions/2026-08-2-agosto-seco.jpg'),
('Agosto negocio','Consulta por volumen y disponibilidad. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-08-01','2026-08-31',20260803,1,NULL,'#581c87',NOW(),NOW(),'/uploads/belen/promotions/2026-08-3-agosto-negocio.jpg'),
('Setiembre práctico','Productos secos que rinden más. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-09-01','2026-09-30',20260901,1,NULL,'#365314',NOW(),NOW(),'/uploads/belen/promotions/2026-09-1-setiembre-practico.jpg'),
('Setiembre fresco','Frutas amazónicas según temporada. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-09-01','2026-09-30',20260902,1,NULL,'#581c87',NOW(),NOW(),'/uploads/belen/promotions/2026-09-2-setiembre-fresco.jpg'),
('Setiembre delivery','Coordina entrega en Lima Este. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-09-01','2026-09-30',20260903,1,NULL,'#064e3b',NOW(),NOW(),'/uploads/belen/promotions/2026-09-3-setiembre-delivery.jpg'),
('Octubre mayorista','Compra para negocio sin complicarte. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-10-01','2026-10-31',20261001,1,NULL,'#581c87',NOW(),NOW(),'/uploads/belen/promotions/2026-10-1-octubre-mayorista.jpg'),
('Octubre cocina','Ingredientes amazónicos para tu menú. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-10-01','2026-10-31',20261002,1,NULL,'#064e3b',NOW(),NOW(),'/uploads/belen/promotions/2026-10-2-octubre-cocina.jpg'),
('Octubre familiar','Opciones para casa y reuniones. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-10-01','2026-10-31',20261003,1,NULL,'#166534',NOW(),NOW(),'/uploads/belen/promotions/2026-10-3-octubre-familiar.jpg'),
('Noviembre preventa','Separa productos para fin de año. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-11-01','2026-11-30',20261101,1,NULL,'#064e3b',NOW(),NOW(),'/uploads/belen/promotions/2026-11-1-noviembre-preventa.jpg'),
('Noviembre restaurantes','Planifica abastecimiento de temporada. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-11-01','2026-11-30',20261102,1,NULL,'#166534',NOW(),NOW(),'/uploads/belen/promotions/2026-11-2-noviembre-restaurantes.jpg'),
('Noviembre regalo','Detalles con sabor amazónico. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-11-01','2026-11-30',20261103,1,NULL,'#7c2d12',NOW(),NOW(),'/uploads/belen/promotions/2026-11-3-noviembre-regalo.jpg'),
('Diciembre navideño','Canastas amazónicas para compartir. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-12-01','2026-12-31',20261201,1,NULL,'#166534',NOW(),NOW(),'/uploads/belen/promotions/2026-12-1-diciembre-navideno.jpg'),
('Diciembre reuniones','Productos para cenas y encuentros. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-12-01','2026-12-31',20261202,1,NULL,'#7c2d12',NOW(),NOW(),'/uploads/belen/promotions/2026-12-2-diciembre-reuniones.jpg'),
('Diciembre cierre','Coordina pedidos antes de fiestas. Consulta disponibilidad, precio y coordinación de entrega por WhatsApp para Lima Este y Ate Vitarte.','2026-12-01','2026-12-31',20261203,1,NULL,'#0f172a',NOW(),NOW(),'/uploads/belen/promotions/2026-12-3-diciembre-cierre.jpg');

DELETE FROM delivery_zone;

INSERT INTO delivery_zone
(name, latitude, longitude, radius_meters, note, created_at, updated_at)
VALUES
('Ate Vitarte - zona principal', -12.0266, -76.9199, 4500, 'Zona principal de atención. Reparto o punto de coordinación según disponibilidad.', NOW(), NOW()),
('Santa Anita - coordinación cercana', -12.0432, -76.9714, 3500, 'Atención cercana a Ate. Coordinar día, horario y punto de entrega por WhatsApp.', NOW(), NOW()),
('El Agustino / San Luis - punto de encuentro', -12.0477, -77.0007, 3500, 'Punto de coordinación para pedidos programados.', NOW(), NOW()),
('La Molina - coordinación previa', -12.0864, -76.9297, 4000, 'Atención sujeta a disponibilidad y coordinación previa.', NOW(), NOW()),
('Lima Este - reparto por coordinación', -12.0453, -76.9304, 6500, 'Pedidos para hogares, restaurantes, bodegas y negocios de comida. Confirmar disponibilidad.', NOW(), NOW()),
('Chosica / Huaycán - consultar disponibilidad', -11.9431, -76.7091, 7000, 'Zona extendida. Consultar disponibilidad, volumen mínimo y fecha de entrega.', NOW(), NOW());

UPDATE platform_setting
SET value = '51928527493'
WHERE variable = 'public.whatsapp.number';

UPDATE platform_setting
SET value = 'Atención en Lima Este y zonas cercanas'
WHERE variable = 'public.topbar.location';

UPDATE platform_setting
SET value = 'Pedidos y consultas por WhatsApp'
WHERE variable = 'public.topbar.whatsapp_label';

UPDATE platform_setting
SET value = 'Lima Este / Ate Vitarte'
WHERE variable = 'public.topbar.phone';

UPDATE platform_setting
SET value = 'Zona:'
WHERE variable = 'public.topbar.phone_label';

UPDATE platform_setting
SET value = 'Atención y entregas en Lima Este'
WHERE variable = 'public.delivery.title';

UPDATE platform_setting
SET value = 'Atendemos principalmente Ate Vitarte y coordinamos pedidos en Santa Anita, La Molina, El Agustino, San Luis, Huaycán, Chosica y zonas cercanas.'
WHERE variable = 'public.delivery.subtitle';

UPDATE platform_setting
SET value = 'También atendemos consultas para hogares, restaurantes, bodegas y negocios de comida. Confirma disponibilidad, volumen mínimo y punto de entrega por WhatsApp.'
WHERE variable = 'public.delivery.extra_text';

UPDATE platform_setting
SET value = 'Consultar reparto por WhatsApp »'
WHERE variable = 'public.delivery.cta_label';

UPDATE platform_setting
SET value = 'Consulta promociones por temporada, productos por volumen y campañas activas del mes.'
WHERE variable = 'public.promotions.subtitle';

UPDATE platform_setting
SET value = 'Promociones y campañas del mes'
WHERE variable = 'public.promotions.title';

UPDATE platform_setting
SET value = '© 2026 Productos de la Selva Belén'
WHERE variable = 'public.footer.left';

UPDATE platform_setting
SET value = 'Productos amazónicos seleccionados para Lima Este y Ate Vitarte'
WHERE variable = 'public.footer.right';
