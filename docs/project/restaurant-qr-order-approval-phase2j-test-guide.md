# Restaurant Phase 2J - QR order approval test guide

## Goal

Allow customers to send an order from the public QR menu, but keep the flow controlled by the waiter before sending it to kitchen.

## Test flow

1. Open the public QR menu with a table:
   `http://localhost:8084/restaurant/menu?tableId=1`
2. Choose quantities for 2 or 3 dishes.
3. Add an optional note.
4. Click **Enviar pedido QR**.
5. Log in as `admin_demo` or `mozo_demo`.
6. Open `/admin/restaurant/qr-orders`.
7. Verify the order is listed as pending.
8. Click **Aprobar y enviar a cocina**.
9. Verify it redirects to the generated or updated comanda.
10. Open `/admin/restaurant/kitchen` and confirm the items are visible.
11. Create another QR order and reject it.
12. Verify rejected orders do not create comandas or reserve stock.

## Expected behavior

- Public QR orders are saved as pending requests.
- Stock is not discounted until the waiter approves the QR order.
- Approval creates a new comanda if the table has no active one.
- Approval adds items to the active comanda if the table already has one.
- Rejection only changes the QR order status.
