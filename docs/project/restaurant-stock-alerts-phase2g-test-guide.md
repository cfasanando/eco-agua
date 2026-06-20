# Fase Restaurante 2G - Stock simple, agotados y alertas

## Objetivo

Validar que la carta restaurante controle disponibilidad con stock referencial:

- Platos con stock cero quedan agotados.
- Platos agotados no aparecen en carta pública ni en nueva comanda.
- Al crear o agregar productos a una comanda, el stock baja.
- Si el stock llega a cero, el plato se marca como agotado.
- Si se anula una comanda o se reduce una cantidad, el stock se devuelve.
- Administración puede reponer stock desde Platos y carta.

## Rutas principales

- `/admin/restaurant/menu-items`
- `/admin/restaurant/orders/new`
- `/admin/restaurant/orders/{id}`
- `/restaurant/menu?tableId=1`

## Prueba rápida

1. Entrar como `admin_demo`.
2. Abrir `/admin/restaurant/menu-items`.
3. Elegir un plato y dejar stock bajo, por ejemplo `2`, con stock mínimo `5`.
4. Confirmar que aparece con alerta `Stock bajo`.
5. Crear una comanda con cantidad `2` de ese plato.
6. Confirmar que el plato queda agotado.
7. Verificar que ya no aparece en `/admin/restaurant/orders/new` ni en `/restaurant/menu?tableId=1`.
8. Volver a `/admin/restaurant/menu-items` y usar `Reponer` con cantidad `5`.
9. Confirmar que vuelve como disponible.
10. Crear otra comanda y luego anularla para confirmar que el stock se devuelve.

## Roles esperados

- `admin_demo`: administra platos, stock y comandas.
- `mozo_demo`: crea comandas y respeta stock disponible.
- `cocina_demo`: no administra stock; solo cocina.
- `caja_demo`: no administra stock; cobra y reporta.

## Resultado esperado

El restaurante ya puede controlar agotados de forma simple sin implementar todavía recetas ni inventario avanzado.
