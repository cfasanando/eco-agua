-- Productos de la Selva Belén - product catalog content cleanup
-- Updates 52 product descriptions and removes the placeholder text from the current dump.
SET NAMES utf8mb4;
START TRANSACTION;

CREATE TABLE IF NOT EXISTS product_description_backup_belen_20260529 AS
SELECT id, name, description, price, suggested_price
FROM product
WHERE id BETWEEN 1 AND 52;

UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Plátano verde amazónico ideal para tacacho, patacones, frituras y comidas de negocio.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 2.80</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 2.50</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Tacacho, patacones, frituras y acompañamientos contundentes.</span></div>
<div><strong>Presentación</strong><span>Unidad o cantidad según disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener en lugar fresco, seco y ventilado.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Restaurantes y puestos de comida</li><li>Pedidos por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 1;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Fruta amazónica de sabor ácido-dulce, muy usada en refrescos, jugos y preparaciones caseras.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 5.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 4.50</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Refrescos, jugos, cremoladas y mezclas tropicales.</span></div>
<div><strong>Presentación</strong><span>Consultar madurez y disponibilidad por temporada.</span></div>
<div><strong>Conservación</strong><span>Consumir pronto si está madura o refrigerar.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Juguerías</li><li>Familias</li><li>Pedidos por temporada</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 2;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Fruta amazónica ácida, recomendada para refrescos, jugos y preparaciones con sabor intenso.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 12.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 11.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Jugos, refrescos, pulpas y cremoladas.</span></div>
<div><strong>Presentación</strong><span>Disponible según temporada y abastecimiento.</span></div>
<div><strong>Conservación</strong><span>Mantener refrigerado si ya está maduro.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Bebidas naturales</li><li>Negocios de jugos</li><li>Consumo familiar</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 3;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Fruto amazónico tradicional, consultado para masa, refrescos, postres y consumo regional.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 6.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 5.50</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Masa de aguaje, refrescos, helados y postres.</span></div>
<div><strong>Presentación</strong><span>Consultar si está en fruto, masa o presentación disponible.</span></div>
<div><strong>Conservación</strong><span>Consumir pronto si está maduro.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Preparaciones regionales</li><li>Postres y bebidas</li><li>Venta por temporada</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 4;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Fruta amazónica ácida y aromática, perfecta para ajíes, salsas, refrescos y comidas regionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 3.50</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 3.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Ají de cocona, salsas, refrescos y acompañamientos.</span></div>
<div><strong>Presentación</strong><span>Consultar si está verde, pintona o madura.</span></div>
<div><strong>Conservación</strong><span>Guardar fresca; refrigerar si está muy madura.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina amazónica</li><li>Restaurantes</li><li>Salsas caseras</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 5;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Coco fresco para bebidas, postres, rallado, dulces y preparaciones caseras.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 4.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 3.50</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Refrescos, postres, rallado y consumo directo.</span></div>
<div><strong>Presentación</strong><span>Unidad o cantidad según disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Si está abierto, refrigerar.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Bebidas y postres</li><li>Consumo familiar</li><li>Negocios de comida</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 6;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Fruta aromática de la selva, consultada para consumo directo, refrescos y preparaciones caseras.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 12.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 10.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Frutas amazónicas</span></div>
<div><strong>Uso sugerido</strong><span>Consumo directo, refrescos y mezclas de frutas.</span></div>
<div><strong>Presentación</strong><span>Disponibilidad variable por temporada.</span></div>
<div><strong>Conservación</strong><span>Consumir pronto cuando está madura.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Consumo familiar</li><li>Jugos y refrescos</li><li>Canastas amazónicas</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 7;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico consultado para frituras, caldos y preparaciones familiares.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 29.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 27.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Frito, sudado, caldo, parrilla y comidas del día.</span></div>
<div><strong>Presentación</strong><span>Consultar tamaño, peso y disponibilidad antes de separar.</span></div>
<div><strong>Conservación</strong><span>Mantener refrigerado y consumir pronto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Comidas familiares</li><li>Restaurantes</li><li>Pedidos de pescado</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 8;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado tradicional de la selva, apreciado para frituras, sopas y platos regionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 18.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Frito, en caldo, envuelto en hoja o preparado al gusto.</span></div>
<div><strong>Presentación</strong><span>Consultar tamaño y cantidad disponible.</span></div>
<div><strong>Conservación</strong><span>Refrigerar y consumir pronto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina regional</li><li>Pedidos familiares</li><li>Negocios de comida</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 9;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado de buena carne, usado en frituras, parrilla, caldos y comidas regionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 18.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Parrilla, fritura, sudado y platos familiares.</span></div>
<div><strong>Presentación</strong><span>Consultar tamaño, peso y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener frío hasta su preparación.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Parrilladas</li><li>Restaurantes</li><li>Consumo familiar</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 10;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico de carne apreciada, ideal para platos de casa y cocina regional.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 22.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 20.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Frito, sudado, caldo o preparación al gusto.</span></div>
<div><strong>Presentación</strong><span>Confirmar tamaño, peso y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Conservar refrigerado y evitar exposición al calor.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Restaurantes</li><li>Comidas familiares</li><li>Pedidos programados</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 11;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico recomendado para caldos, guisos, frituras y preparaciones de sabor intenso.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 29.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 27.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Caldos, guisos, frituras y sudados.</span></div>
<div><strong>Presentación</strong><span>Consultar tamaño, corte y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Refrigerar y preparar pronto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Caldos regionales</li><li>Hogares</li><li>Negocios de comida</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 12;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico para frituras y comidas regionales, sujeto a disponibilidad del día.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 22.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 20.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Fritura, sudado y preparaciones tradicionales.</span></div>
<div><strong>Presentación</strong><span>Consultar tamaño y cantidad disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener frío y cocinar pronto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina regional</li><li>Venta del día</li><li>Pedidos por WhatsApp</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 13;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado de la selva usado en frituras, caldos y comidas familiares.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 18.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Frito, caldo, sudado y preparaciones caseras.</span></div>
<div><strong>Presentación</strong><span>Consultar disponibilidad y tamaño.</span></div>
<div><strong>Conservación</strong><span>Conservar refrigerado.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Restaurantes pequeños</li><li>Pedidos diarios</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 14;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado fresco recomendado para frituras, sudados y comidas de buen rendimiento.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 24.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 22.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Fritura, sudado, parrilla o caldo ligero.</span></div>
<div><strong>Presentación</strong><span>Consultar peso, tamaño y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener frío y consumir pronto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Consumo familiar</li><li>Restaurantes</li><li>Pedidos por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 15;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico consultado para comidas regionales, frituras y preparación del día.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 23.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 21.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Frito, guiso o preparado al gusto.</span></div>
<div><strong>Presentación</strong><span>Confirmar disponibilidad antes de separar.</span></div>
<div><strong>Conservación</strong><span>Refrigerar y evitar cambios de temperatura.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina de casa</li><li>Restaurantes</li><li>Pedidos programados</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 16;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico de carne apreciada para parrilla, fritura, sudado y platos familiares.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 23.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 21.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Parrilla, fritura, sudado y preparaciones familiares.</span></div>
<div><strong>Presentación</strong><span>Consultar tamaño, peso y presentación.</span></div>
<div><strong>Conservación</strong><span>Mantener frío hasta cocinar.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Parrillas familiares</li><li>Restaurantes</li><li>Pedidos especiales</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 17;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado amazónico disponible según temporada, útil para frituras, caldos y comidas diarias.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 18.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 16.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados frescos</span></div>
<div><strong>Uso sugerido</strong><span>Fritura, caldo y comida casera.</span></div>
<div><strong>Presentación</strong><span>Consultar disponibilidad del día.</span></div>
<div><strong>Conservación</strong><span>Refrigerar y consumir pronto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Pedidos rápidos</li><li>Cocina regional</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 18;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Producto seco de yuca, rendidor y práctico como acompañamiento en comidas amazónicas.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 12.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 10.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Acompañamiento de caldos, pescados, guisos y comidas regionales.</span></div>
<div><strong>Presentación</strong><span>Bolsa o cantidad según disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Guardar en envase cerrado, seco y lejos de humedad.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Despensa del hogar</li><li>Restaurantes</li><li>Compras por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 19;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Derivado de yuca usado en postres, bebidas, preparaciones caseras y cocina regional.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 18.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 16.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Postres, bebidas, mazamorras y recetas caseras.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación y peso disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener en recipiente cerrado y seco.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Postres caseros</li><li>Negocios de bebidas</li><li>Despensa familiar</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 20;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Almidón práctico para espesar, preparar masas y complementar recetas caseras.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 16.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 14.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Masas, panes, espesantes, postres y cocina tradicional.</span></div>
<div><strong>Presentación</strong><span>Venta por peso o paquete según stock.</span></div>
<div><strong>Conservación</strong><span>Evitar humedad; cerrar bien después de abrir.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina casera</li><li>Panadería o snacks</li><li>Negocios de comida</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 21;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Harina útil para preparaciones caseras, masas, acompañamientos y cocina diaria.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 12.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 10.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Masas, frituras, preparados caseros y acompañamientos.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación disponible.</span></div>
<div><strong>Conservación</strong><span>Guardar en lugar seco y cerrado.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Despensa familiar</li><li>Preparaciones caseras</li><li>Compras por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 22;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Harina de plátano para bebidas, mezclas, recetas caseras y consumo familiar.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 12.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 10.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Bebidas, mezclas, preparaciones caseras y recetas tradicionales.</span></div>
<div><strong>Presentación</strong><span>Consultar peso y empaque disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener seca y protegida del calor.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Bebidas caseras</li><li>Despensa práctica</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 23;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Frejol regional para guisos, menestras, comidas familiares y preparación por cantidad.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 14.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 12.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, menestras, sopas y platos familiares.</span></div>
<div><strong>Presentación</strong><span>Venta por peso o cantidad según disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Guardar seco, ventilado y protegido.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Restaurantes</li><li>Compras para la semana</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 24;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Frejol seco práctico para guisos, sopas y comidas de buen rendimiento.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 10.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 8.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, menestras, sopas y acompañamientos.</span></div>
<div><strong>Presentación</strong><span>Consultar disponibilidad por peso.</span></div>
<div><strong>Conservación</strong><span>Mantener seco y en envase cerrado.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Despensa del hogar</li><li>Negocios de comida</li><li>Compras por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 25;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Frejol regional para comidas caseras, guisos y preparaciones tradicionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 10.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 8.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Harinas y secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, menestras y platos familiares.</span></div>
<div><strong>Presentación</strong><span>Venta según stock disponible.</span></div>
<div><strong>Conservación</strong><span>Guardar seco y protegido.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina diaria</li><li>Restaurantes pequeños</li><li>Pedidos familiares</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 26;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Palometa seca amazónica, práctica para guisos, comidas tradicionales y pedidos por cantidad.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 28.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 26.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, sopas, comida regional y preparaciones con sabor concentrado.</span></div>
<div><strong>Presentación</strong><span>Consultar peso, corte y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Guardar seco, cerrado y lejos de humedad.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Despensa familiar</li><li>Restaurantes</li><li>Pedidos por mayor</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 27;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado seco amazónico de sabor concentrado, útil para guisos y platos tradicionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 16.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 14.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, sopas, platos regionales y preparaciones con sabor intenso.</span></div>
<div><strong>Presentación</strong><span>Consultar peso, corte y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Guardar seco, cerrado y lejos de humedad.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Comidas tradicionales</li><li>Compra por cantidad</li><li>Despensa amazónica</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 28;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Pescado seco consultado para cocina regional, con sabor marcado y buena conservación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 18.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, caldos, platos regionales y comidas de casa.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación y peso disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener en bolsa o envase cerrado.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Restaurantes</li><li>Pedidos por temporada</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 29;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Paiche seco amazónico, producto destacado para platos tradicionales y pedidos por cantidad.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 32.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 28.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, platos regionales, recetas familiares y negocios de comida.</span></div>
<div><strong>Presentación</strong><span>Consultar corte, peso y disponibilidad antes de separar.</span></div>
<div><strong>Conservación</strong><span>Guardar seco y protegido; refrigerar si ya fue abierto.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Restaurantes</li><li>Eventos familiares</li><li>Compras por mayor</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 30;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Doncella seca amazónica, recomendada para cocina regional y conservación por más tiempo.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 28.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 26.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Pescados secos</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, caldos, sopas y platos tradicionales.</span></div>
<div><strong>Presentación</strong><span>Consultar corte, peso y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener en bolsa cerrada, seca y protegida.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina familiar</li><li>Restaurantes</li><li>Compras por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 31;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Cecina roja ahumada para frituras, desayunos, acompañamientos y comidas rápidas.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 35.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 34.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Frita, asada, en desayunos, tacacho o platos regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación, grosor y peso.</span></div>
<div><strong>Conservación</strong><span>Refrigerar si está abierta y evitar calor.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Desayunos regionales</li><li>Restaurantes</li><li>Pedidos familiares</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 32;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Chorizo rojo ahumado para parrilla, frituras, desayunos y comidas rápidas.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 25.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 24.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Parrilla, fritura, acompañamientos y platos regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar unidad, peso o paquete disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener refrigerado, especialmente después de abrir.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Parrillas</li><li>Negocios de comida</li><li>Consumo familiar</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 33;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Cecina natural ahumada de sabor tradicional, recomendada para comidas regionales y pedidos por coordinación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 38.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 36.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Tacacho con cecina, frituras, desayunos, almuerzos y platos de venta.</span></div>
<div><strong>Presentación</strong><span>Consultar corte, grosor y peso disponible.</span></div>
<div><strong>Conservación</strong><span>Refrigerar si está abierta; mantener protegida de humedad y calor.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Restaurantes</li><li>Hogares</li><li>Pedidos por mayor</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 34;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Chorizo natural ahumado para desayunos, parrilla, frituras y platos amazónicos.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 28.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 26.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Parrilla, fritura, desayunos y acompañamientos.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener refrigerado después de abrir.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Comida rápida regional</li><li>Hogares</li><li>Negocios de comida</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 35;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Cuero ahumado de sabor intenso, usado en guisos, sopas y preparaciones tradicionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 16.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 14.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, sopas, menestras y platos con sabor ahumado.</span></div>
<div><strong>Presentación</strong><span>Consultar peso y corte disponible.</span></div>
<div><strong>Conservación</strong><span>Refrigerar si está abierto y usar utensilios limpios.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Guisos familiares</li><li>Restaurantes</li><li>Cocina tradicional</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 36;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Oreja ahumada para preparaciones con textura, guisos y platos de sabor regional.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 16.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 14.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Guisos, sopas, platos con textura y preparaciones tradicionales.</span></div>
<div><strong>Presentación</strong><span>Consultar peso y presentación.</span></div>
<div><strong>Conservación</strong><span>Mantener refrigerada y protegida.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina regional</li><li>Negocios de comida</li><li>Pedidos por cantidad</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 37;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Manteca usada como complemento de cocina para dar sabor y preparar platos tradicionales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 16.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 14.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Frituras, guisos, preparación de bases y cocina casera.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación y cantidad disponible.</span></div>
<div><strong>Conservación</strong><span>Guardar bien cerrada y en lugar fresco.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Cocina de casa</li><li>Restaurantes</li><li>Preparaciones regionales</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 38;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Chicharrón regional para acompañamientos, consumo directo y comidas rápidas.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 18.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 16.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Ahumados</span></div>
<div><strong>Uso sugerido</strong><span>Acompañamiento, snacks, desayunos o platos regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar cantidad y presentación del día.</span></div>
<div><strong>Conservación</strong><span>Consumir pronto para mejor textura.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Consumo familiar</li><li>Pedidos rápidos</li><li>Acompañamientos</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 39;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Trago Blanco disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 10.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 8.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 40;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Uvachado disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 41;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Chuchuhuasi disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 42;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Huitochado disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 43;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Svss disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 44;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Siete Raices disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 45;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Para Para disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 46;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">R.C disponible como producto regional para adultos, sujeto a stock y presentación.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 20.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 17.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Tragos regionales</span></div>
<div><strong>Uso sugerido</strong><span>Producto regional para consumo responsable de adultos o canastas regionales.</span></div>
<div><strong>Presentación</strong><span>Consultar botella, volumen, presentación y disponibilidad.</span></div>
<div><strong>Conservación</strong><span>Mantener cerrado, en lugar fresco y fuera del alcance de menores.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Clientes adultos</li><li>Canastas regionales para adultos</li><li>Pedidos por coordinación</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>
<div class="catalog-warning-box"><i class="bi bi-shield-exclamation"></i><span>Producto dirigido solo a mayores de edad. Comprar y consumir con responsabilidad.</span></div>
</div>' WHERE id = 47;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Dulce regional seco, práctico para compartir, vender o incluir en canastas amazónicas.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 35.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 29.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Dulces regionales</span></div>
<div><strong>Uso sugerido</strong><span>Acompañamiento, lonchera, merienda o detalle regional.</span></div>
<div><strong>Presentación</strong><span>Consultar bolsa, paquete o cantidad disponible.</span></div>
<div><strong>Conservación</strong><span>Guardar cerrado para mantener textura.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Consumo familiar</li><li>Canastas</li><li>Reventa</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 48;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Dulce regional amazónico para compartir, vender por paquete o incluir en pedidos familiares.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 35.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 29.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Dulces regionales</span></div>
<div><strong>Uso sugerido</strong><span>Merienda, detalle regional, venta por paquete o acompañamiento.</span></div>
<div><strong>Presentación</strong><span>Consultar presentación y cantidad disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener en envase cerrado y lugar seco.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Hogares</li><li>Canastas regionales</li><li>Reventa</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 49;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Dulce regional ligero y tradicional, ideal para compartir o agregar a pedidos especiales.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 35.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 29.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Dulces regionales</span></div>
<div><strong>Uso sugerido</strong><span>Meriendas, detalles, canastas y consumo familiar.</span></div>
<div><strong>Presentación</strong><span>Consultar bolsa o paquete disponible.</span></div>
<div><strong>Conservación</strong><span>Guardar protegido de humedad para mantener textura.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Canastas</li><li>Regalos regionales</li><li>Consumo familiar</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 50;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Roscas regionales de presentación vistosa, ideales para compartir, vender o regalar.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 25.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 20.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Dulces regionales</span></div>
<div><strong>Uso sugerido</strong><span>Meriendas, detalles regionales, canastas o venta por paquete.</span></div>
<div><strong>Presentación</strong><span>Consultar cantidad y empaque disponible.</span></div>
<div><strong>Conservación</strong><span>Mantener cerradas y secas.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Canastas amazónicas</li><li>Reventa</li><li>Familias</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 51;
UPDATE product SET description = '<div class="catalog-product-content">
<p class="catalog-card-summary">Dulce regional tipo galleta o biscocho, práctico para merienda, venta y pedidos familiares.</p>
<div class="catalog-price-panel">
<div><span>Precio referencial por menor</span><strong>S/ 25.00</strong></div>
<div><span>Precio referencial por mayor</span><strong>S/ 20.00</strong></div>
</div>
<div class="catalog-detail-grid">
<div><strong>Categoría</strong><span>Dulces regionales</span></div>
<div><strong>Uso sugerido</strong><span>Merienda, lonchera, canastas y acompañamiento de bebidas.</span></div>
<div><strong>Presentación</strong><span>Consultar paquete, bolsa o cantidad disponible.</span></div>
<div><strong>Conservación</strong><span>Guardar en lugar seco y cerrado.</span></div>
</div>
<h6>Ideal para</h6>
<ul class="catalog-benefit-list"><li>Consumo familiar</li><li>Reventa</li><li>Canastas regionales</li></ul>
<div class="catalog-tip-box"><i class="bi bi-whatsapp"></i><span>Consulta disponibilidad real, precio actualizado y coordinación de entrega por WhatsApp antes de separar el pedido.</span></div>

</div>' WHERE id = 52;

COMMIT;

SELECT id, name, price, suggested_price FROM product WHERE id BETWEEN 1 AND 52 ORDER BY id;