# Estado final V1 - Eco Agua

## Conclusión

La V1 está en etapa de cierre. Ya no conviene agregar más pantallas antes de probar con datos reales.

El sistema ya cubre:

- Marketing y contenido.
- Ventas / CRM.
- Clientes y seguimiento comercial.
- Pedidos, cuentas por cobrar y reportes comerciales.
- Logística, inventario, kardex y compras/stock.
- Finanzas internas, caja, cuentas por pagar y resumen mensual.
- Contabilidad interna de apoyo.
- RRHH interno: personal, ficha, pagos, recibos, adelantos/deudas y asistencia.
- Producción: recetas, lotes, merma, costos, calidad, vencimientos, trazabilidad, planificación, capacidad, requerimientos e insumos.
- Costos y rentabilidad: producto, canal y simulador de precios.
- Dashboard gerencial.
- Configuración por cliente desde base de datos.
- Módulos visibles por cliente.
- Endurecimiento mínimo de permisos V1.

## Criterio para cerrar V1

La V1 se puede considerar lista cuando se cumpla esto:

1. `mvn -DskipTests compile` pasa sin errores.
2. La aplicación levanta con `./scripts/run-dev.sh`.
3. Las rutas principales cargan sin pantalla blanca.
4. Agua Eco y Productos de la Selva Belén tienen bases separadas y cargan con su configuración.
5. Las tablas/columnas manuales existen en ambas bases.
6. Los módulos visibles por cliente están configurados.
7. No se vende como sistema SUNAT oficial.

## Pendientes antes de entregar

- Validar rutas principales como administrador.
- Validar una prueba completa de pedido pagado y pedido al crédito.
- Validar una prueba de stock/compra.
- Validar una producción completa en Agua Eco, si se usará producción.
- Validar configuración de Belén con módulos no usados ocultos.
- Revisar textos con caracteres raros en `platform_setting`.

## No hacer ahora

- No agregar SUNAT oficial, PLE, SIRE ni libros electrónicos oficiales.
- No agregar más pantallas de producción sin pruebas reales.
- No crear presets hardcodeados por cada cliente.
- No usar archivos `client_profiles` como mecanismo principal de configuración.
