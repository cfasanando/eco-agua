-- Productos de la Selva Belén - monthly promotions and Lima Este delivery setup
-- Safe seed: updates public location settings, replaces old Iquitos delivery zone, and creates 12 monthly promotions for 2026.

SET NAMES utf8mb4;
USE `productos_selva_belen`;

START TRANSACTION;

-- Public WhatsApp and location settings for Lima / Ate Vitarte.
UPDATE platform_setting SET value = '928 527 493' WHERE variable = 'public.topbar.phone';
UPDATE platform_setting SET value = '51928527493' WHERE variable = 'public.whatsapp.number';
UPDATE platform_setting SET value = 'Atención en Ate Vitarte, Lima y zonas cercanas' WHERE variable = 'public.topbar.location';
UPDATE platform_setting SET value = 'Productos amazónicos seleccionados en Ate Vitarte, Lima' WHERE variable = 'public.footer.right';
UPDATE platform_setting SET value = 'Atención en Ate Vitarte' WHERE variable = 'public.hero.stat_2';
UPDATE platform_setting SET value = 'Atención en Lima Este' WHERE variable = 'public.trust.3.title';
UPDATE platform_setting SET value = 'Consulta disponibilidad para Ate Vitarte y zonas cercanas.' WHERE variable = 'public.trust.3.text';
UPDATE platform_setting SET value = 'Atención y entregas en Lima Este' WHERE variable = 'public.delivery.title';
UPDATE platform_setting SET value = 'Consulta si podemos atender tu zona o coordinar un punto de entrega en Ate Vitarte y distritos cercanos.' WHERE variable = 'public.delivery.subtitle';
UPDATE platform_setting SET value = 'También atendemos consultas para hogares, restaurantes, bodegas y negocios de comida en Ate Vitarte, Santa Anita, La Molina, El Agustino y zonas cercanas.' WHERE variable = 'public.delivery.extra_text';
UPDATE platform_setting SET value = 'Consultar reparto »' WHERE variable = 'public.delivery.cta_label';
UPDATE platform_setting SET value = 'Hola, deseo consultar disponibilidad de productos amazónicos de Productos de la Selva Belén para Lima Este' WHERE variable = 'public.home.whatsapp_intro';
UPDATE platform_setting SET value = 'Hola, deseo consultar por un producto del catálogo de Productos de la Selva Belén para entrega o coordinación en Lima Este' WHERE variable = 'public.catalog.whatsapp_intro';

-- Replace the previous Iquitos-only delivery setup with Lima Este / Ate Vitarte zones.
DELETE FROM delivery_zone
WHERE name IN (
  'Iquitos - zona urbana',
  'Ate Vitarte - zona principal',
  'Santa Anita - coordinación cercana',
  'La Molina - coordinación previa',
  'El Agustino / San Luis - punto de encuentro',
  'Lima Este - reparto por coordinación',
  'Chosica / Huaycán - consultar disponibilidad'
);

INSERT INTO delivery_zone (name, latitude, longitude, radius_meters, note, created_at, updated_at) VALUES
('Ate Vitarte - zona principal', -12.026700, -76.921200, 6000, 'Zona principal de atención. Reparto o punto de coordinación según disponibilidad.', NOW(), NOW()),
('Santa Anita - coordinación cercana', -12.043200, -76.970200, 4500, 'Atención cercana a Ate. Coordinar día, horario y punto de entrega por WhatsApp.', NOW(), NOW()),
('La Molina - coordinación previa', -12.068600, -76.943600, 4500, 'Atención sujeta a disponibilidad y coordinación previa.', NOW(), NOW()),
('El Agustino / San Luis - punto de encuentro', -12.065000, -76.993700, 4500, 'Punto de coordinación para pedidos programados.', NOW(), NOW()),
('Lima Este - reparto por coordinación', -12.035000, -76.945000, 10000, 'Pedidos para hogares, restaurantes, bodegas y negocios de comida. Confirmar disponibilidad.', NOW(), NOW()),
('Chosica / Huaycán - consultar disponibilidad', -11.943000, -76.709000, 8000, 'Zona extendida. Consultar disponibilidad, volumen mínimo y fecha de entrega.', NOW(), NOW());

-- Replace previous generic seasonal promotion and create 12 monthly promotions for 2026.
DELETE FROM promotion
WHERE promo_number BETWEEN 202601 AND 202612
   OR name = 'Consulta por temporada';

INSERT INTO promotion
(id, name, description, start_date, end_date, promo_number, enabled, max_counter, color_border, created_at, updated_at, banner_image_path)
VALUES
(202601, 'Enero fresco: frutas amazónicas para empezar el año', 'Consulta frutas amazónicas de temporada como aguaje, camu camu, cocona, taperibá y otros productos disponibles para casa o negocio.', '2026-01-01', '2026-01-31', 202601, 1, NULL, '#16a34a', NOW(), NOW(), '/uploads/belen/promotions/2026-01-enero-frutas-amazonicas.jpg'),
(202602, 'Febrero para hogares y restaurantes', 'Productos amazónicos seleccionados para pedidos familiares, restaurantes, bodegas y negocios de comida en Lima Este, según stock disponible.', '2026-02-01', '2026-02-28', 202602, 1, NULL, '#0f766e', NOW(), NOW(), '/uploads/belen/promotions/2026-02-febrero-hogar-restaurante.jpg'),
(202603, 'Marzo mayorista: stock para negocios', 'Consulta productos por volumen para restaurantes, bodegas o reventa: pescados, ahumados, harinas y productos secos según disponibilidad.', '2026-03-01', '2026-03-31', 202603, 1, NULL, '#0ea5e9', NOW(), NOW(), '/uploads/belen/promotions/2026-03-marzo-mayorista.jpg'),
(202604, 'Semana Santa amazónica: pescado seco y paiche', 'Coordina con anticipación pescado seco, paiche y productos tradicionales para comidas familiares o negocios durante Semana Santa.', '2026-04-01', '2026-04-30', 202604, 1, NULL, '#166534', NOW(), NOW(), '/uploads/belen/promotions/2026-04-semana-santa-pescado-paiche.jpg'),
(202605, 'Mayo familiar: canasta amazónica', 'Arma una canasta con productos de la selva para casa, reuniones o regalos: frutas, secos, harinas y ahumados según disponibilidad.', '2026-05-01', '2026-05-31', 202605, 1, NULL, '#7c3aed', NOW(), NOW(), '/uploads/belen/promotions/2026-05-canasta-amazonica.jpg'),
(202606, 'San Juan: hojas de bijao y productos tradicionales', 'Temporada fuerte para hojas de bijao, hojas de plátano, pescados, paiche seco y productos amazónicos. Consulta disponibilidad antes de comprar.', '2026-06-01', '2026-06-30', 202606, 1, NULL, '#15803d', NOW(), NOW(), '/uploads/belen/promotions/2026-06-san-juan-hojas-productos.jpg'),
(202607, 'Fiestas Patrias con sabor de la selva', 'Celebra julio con productos amazónicos para reuniones familiares, parrillas, comidas típicas y pedidos por WhatsApp en Lima Este.', '2026-07-01', '2026-07-31', 202607, 1, NULL, '#dc2626', NOW(), NOW(), '/uploads/belen/promotions/2026-07-fiestas-patrias-selva.jpg'),
(202608, 'Agosto de ahumados amazónicos', 'Consulta cecina, chorizo, cuero, oreja, manteca, chicharrón y otros ahumados para casa o negocio, según stock.', '2026-08-01', '2026-08-31', 202608, 1, NULL, '#92400e', NOW(), NOW(), '/uploads/belen/promotions/2026-08-ahumados-amazonicos.jpg'),
(202609, 'Despensa amazónica: harinas y productos secos', 'Productos secos y rendidores para tener en casa o negocio: fariña, tapioca, almidón de yuca, harinas y frejoles según disponibilidad.', '2026-09-01', '2026-09-30', 202609, 1, NULL, '#ca8a04', NOW(), NOW(), '/uploads/belen/promotions/2026-09-despensa-harinas-secos.jpg'),
(202610, 'Restaurantes y bodegas: compra por mayor', 'Planifica tus compras por volumen para negocios de comida, restaurantes, bodegas y reventa. Confirma producto, cantidad y zona.', '2026-10-01', '2026-10-31', 202610, 1, NULL, '#0369a1', NOW(), NOW(), '/uploads/belen/promotions/2026-10-restaurantes-bodegas-mayor.jpg'),
(202611, 'Precampaña navideña amazónica', 'Consulta productos para preparar canastas, reuniones, pedidos especiales y compras anticipadas antes de diciembre.', '2026-11-01', '2026-11-30', 202611, 1, NULL, '#be123c', NOW(), NOW(), '/uploads/belen/promotions/2026-11-pre-navidad-amazonica.jpg'),
(202612, 'Navidad amazónica: canastas y reuniones', 'Arma canastas y pedidos para reuniones de fin de año con productos amazónicos disponibles. Coordina con anticipación por WhatsApp.', '2026-12-01', '2026-12-31', 202612, 1, NULL, '#b91c1c', NOW(), NOW(), '/uploads/belen/promotions/2026-12-navidad-canastas-reuniones.jpg');

COMMIT;
