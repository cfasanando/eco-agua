# Restaurant table requests - Phase 2I test guide

## Scope

This phase turns the public QR menu into a simple request channel for each table.
Customers can request attention, call the waiter, ask for the bill, notify payment, or send a short note.
The restaurant team can see pending requests from the dashboard and a dedicated inbox.

## Public test

1. Open `/restaurant/menu?tableId=1`.
2. Confirm that the table request panel appears under the hero section.
3. Click **Solicitar atención**.
4. Confirm that a success message appears on the public menu.
5. Send a note, for example: `Traer servilletas por favor`.

## Admin test

1. Log in as `admin_demo`, `mozo_demo`, or `caja_demo`.
2. Open `/admin/restaurant/dashboard`.
3. Confirm that the pending table request appears in **Solicitudes de mesa**.
4. Open `/admin/restaurant/table-requests`.
5. Confirm filters: pending, resolved, all.
6. Mark a request as **Atendida**.
7. Confirm it disappears from pending and appears in resolved/all.

## Cashier test

1. From the QR menu, use **Pedir la cuenta**.
2. Log in as `caja_demo`.
3. Open `/admin/restaurant/cash`.
4. Confirm that the bill request is visible at the top.
5. Mark it as attended.

## Expected result

- Public users do not need to log in.
- Admin users can see and resolve requests.
- Requests are stored in `restaurant_table_request`.
- No direct public ordering is enabled yet.
