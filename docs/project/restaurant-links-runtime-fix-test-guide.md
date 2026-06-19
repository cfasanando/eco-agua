# Restaurant links runtime fix - test guide

## Objetivo

Hacer que los enlaces del módulo Restaurante funcionen en `http://localhost:8084` sin tocar Eco Agua `8081` ni Productos de la Selva `8082`.

## Pasos

1. Copiar los archivos del paquete final.
2. Ejecutar `manual_sql/restaurant-links-current-runtime-repair.sql` sobre MySQL.
3. Compilar el proyecto.
4. Reiniciar solo el runtime del restaurante.
5. Entrar como `admin_demo / Demo12345`.
6. Probar:
   - `/admin/restaurant/dashboard`
   - `/admin/restaurant/tables`
   - `/admin/restaurant/orders/new`
   - `/admin/restaurant/kitchen`
   - `/restaurant/menu`

## Resultado esperado

- El módulo Restaurante sigue visible en el sidebar.
- El panel carga resumen de mesas/comandas.
- Mesas muestra 4 mesas demo.
- Nueva comanda muestra platos activos.
- Cocina muestra una comanda demo o el estado vacío sin error.
- La carta pública abre en una pestaña nueva.
