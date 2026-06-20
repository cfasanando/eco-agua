# Restaurant Phase 2H - Kitchen ticket, bill and receipt

## Goal

Add printable documents for the restaurant operation without adding electronic invoicing yet.

## Routes

- `/admin/restaurant/orders/{id}/kitchen-ticket`
- `/admin/restaurant/orders/{id}/bill`
- `/admin/restaurant/orders/{id}/receipt`

## Test flow

1. Log in as `admin_demo` or `mozo_demo`.
2. Create a comanda from a free table.
3. Open the comanda detail.
4. Click **Ticket cocina** and verify the printable kitchen ticket opens.
5. Click **Cuenta** and verify the printable customer bill opens.
6. Send the comanda to kitchen and mark it as served.
7. Log in as `caja_demo` or use an admin account.
8. Pay the comanda.
9. Verify the system redirects to the simple receipt.
10. Open `/admin/restaurant/cash` and verify paid orders have a **Recibo** button.
11. Open `/admin/restaurant/kitchen` and verify each active order has a **Ticket** button.

## Expected result

- Kitchen ticket shows order code, table/reference, service type and product quantities.
- Bill shows customer-facing order lines and total.
- Receipt shows payment method, paid date and total.
- Print buttons call the browser print dialog.
- No database changes are required.
