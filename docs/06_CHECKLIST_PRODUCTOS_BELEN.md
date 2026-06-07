# Checklist Productos de la Selva Belén

## Configuración técnica

- Base de datos propia: `productos_belen` o equivalente.
- Puerto recomendado local: `8082`.
- `.properties` solo con conexión, puerto y configuración técnica.

## Settings desde base de datos

Configurar en `/admin/platform-settings`:

- Nombre: `Productos de la Selva Belén`.
- WhatsApp: `+51 928 527 493`.
- Etiqueta de producto: `Producto`.
- Etiqueta de productos: `Catálogo`.
- Etiqueta de cliente: `Cliente`.
- Etiqueta de proveedor: `Proveedor`.
- Etiqueta de insumo: `Material`.
- Etiqueta de envase: `Empaque`.
- Etiqueta de producción: `Preparación` si se activa producción.
- Etiqueta de reposición: `Seguimiento` si se usa.

## Módulos recomendados activos

- Dashboard.
- Ventas / CRM.
- Clientes.
- Pedidos.
- Inventario / catálogo.
- Productos.
- Categorías.
- Proveedores.
- Egresos / compras.
- Ingresos.
- Cashflow.
- Marketing.
- Promociones.
- Portal público.
- Blog.
- Delivery si se controlará entrega.
- RRHH si se controlará personal.

## Módulos recomendados apagados al inicio

- Envases, salvo que se use empaque retornable.
- Reposición automática específica de agua.
- Producción, salvo que se use como preparación/empaque.
- Insumos si todavía no se controlan materiales de empaque.

## Pruebas críticas

### Catálogo

1. Crear productos reales confirmados.
2. Evitar productos no confirmados.
3. Revisar catálogo público.
4. Verificar WhatsApp correcto.

### Ventas

1. Crear cliente.
2. Crear pedido.
3. Registrar pago o fiado.
4. Revisar cuentas por cobrar.
5. Revisar canal de venta.

### Inventario

1. Registrar stock inicial.
2. Registrar compras/reposición.
3. Revisar stock bajo.
4. Revisar rentabilidad por producto.

### Marketing

1. Crear campaña.
2. Crear idea de contenido.
3. Crear plan de publicación.
4. Revisar reporte de acciones.

## Criterio de cierre para Belén

Belén está listo para prueba real si catálogo, pedido, stock, marketing, WhatsApp y dashboard cargan sin errores.
