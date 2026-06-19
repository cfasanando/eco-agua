# Restaurante 2B - Flujo completo de comanda

## Objetivo

Validar el flujo central del restaurante: mesa libre, crear comanda, enviar a cocina, actualizar estado, agregar productos, cobrar y liberar mesa.

## Prueba sugerida

1. Levantar el runtime restaurante en `8084`.
2. Entrar con `admin_demo / Demo12345`.
3. Abrir `/admin/restaurant/dashboard`.
4. Elegir una mesa libre y crear una comanda.
5. Agregar al menos un producto y guardar.
6. Verificar que abre el detalle de la comanda.
7. Agregar un producto adicional desde el detalle.
8. Cambiar cantidad de un producto.
9. Abrir cocina y marcar la comanda como lista.
10. Volver al detalle y marcarla como servida.
11. Cobrar con efectivo, Yape, Plin o tarjeta.
12. Verificar que la mesa vuelve a libre en el dashboard.

## Rutas principales

- `/admin/restaurant/dashboard`
- `/admin/restaurant/orders/new`
- `/admin/restaurant/orders/{id}`
- `/admin/restaurant/kitchen`
- `/admin/restaurant/tables`

## Notas

El instalador del módulo agrega columnas operativas a `restaurant_order` si faltan: `payment_method` y `paid_at`.
El SQL manual incluido solo aplica a `restaurante_buen_sabor` y sirve para reparar la instancia actual si el runtime no llega a ejecutar el instalador.
