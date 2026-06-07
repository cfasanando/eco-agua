# Checklist Agua Eco del Amazonas

## Configuración técnica

- Base de datos propia: `eco_agua` o `eco_agua_dev`.
- Puerto recomendado local: `8081`.
- `.properties` solo con conexión, puerto y configuración técnica.

## Settings desde base de datos

Configurar en `/admin/platform-settings`:

- Nombre: `Agua Eco del Amazonas`.
- WhatsApp oficial del negocio.
- Etiqueta de producto: `Producto`.
- Etiqueta de productos: `Productos`.
- Etiqueta de cliente: `Cliente`.
- Etiqueta de insumo: `Insumo`.
- Etiqueta de envase: `Envase`.
- Etiqueta de producción: `Producción`.
- Etiqueta de reposición: `Reposición`.

## Módulos recomendados activos

En `/admin/system-modules`:

- Dashboard.
- Estado del negocio.
- Seguimiento mensual.
- Ventas / CRM.
- Clientes.
- Delivery.
- Reposición.
- Inventario.
- Productos.
- Insumos.
- Envases.
- Producción.
- Finanzas.
- Cashflow.
- Contabilidad interna.
- Marketing.
- RRHH si se usa personal.
- Portal público y catálogo si se usará web.

## Pruebas críticas

### Venta

1. Crear cliente.
2. Crear pedido pagado.
3. Crear pedido al crédito.
4. Registrar pago de cuenta por cobrar.
5. Confirmar que contabilidad no bloquee el pedido si el asiento automático falla.

### Stock

1. Registrar entrada de producto.
2. Registrar entrada de insumo.
3. Revisar stock bajo.
4. Revisar kardex/movimientos.

### Producción

1. Configurar receta de producto.
2. Planificar producción.
3. Revisar capacidad según stock.
4. Crear producción en borrador.
5. Confirmar producción.
6. Revisar descuento de insumos.
7. Revisar ingreso de producto terminado.
8. Registrar calidad.
9. Revisar trazabilidad, vencimiento y ficha imprimible.

### Finanzas

1. Revisar caja.
2. Revisar cuentas por cobrar.
3. Revisar cuentas por pagar.
4. Revisar rentabilidad por producto.
5. Revisar dashboard gerencial.

## Criterio de cierre para Agua Eco

Agua Eco está lista para pruebas reales si venta, stock, producción, caja y dashboard cargan sin errores.
