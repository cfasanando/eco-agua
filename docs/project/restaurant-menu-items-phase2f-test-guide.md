# Fase Restaurante 2F - Platos y carta

## Objetivo

Permitir que el restaurante administre su carta desde el módulo Restaurante, sin depender de pantallas genéricas de productos.

## Rutas nuevas

- `/admin/restaurant/menu-items`
- `/admin/restaurant/menu-items/new`
- `/admin/restaurant/menu-items/{id}/edit`

## Usuarios sugeridos

- `admin_demo / Demo12345`: puede administrar platos y carta.
- `mozo_demo`, `cocina_demo`, `caja_demo`: no deben ver el mantenimiento de platos.

## Prueba rápida

1. Levantar el restaurante en `8084`.
2. Entrar como `admin_demo`.
3. Abrir `/admin/restaurant/menu-items`.
4. Crear un plato nuevo con categoría nueva, por ejemplo `Fondos`.
5. Confirmar que aparece en la lista.
6. Abrir `/restaurant/menu` y validar que aparece en carta pública.
7. Marcar el plato como `Agotado` desde `/admin/restaurant/menu-items`.
8. Confirmar que ya no aparece en `/restaurant/menu` ni en `/admin/restaurant/orders/new`.
9. Marcarlo nuevamente como disponible.
10. Confirmar que vuelve a aparecer.

## Notas

- `Visible en carta QR` controla si aparece al cliente.
- `Disponible para vender` controla si aparece en comanda y carta pública.
- `Destacado` controla la sección de recomendados.
- `Orden en carta` permite ordenar platos dentro de cada categoría.
