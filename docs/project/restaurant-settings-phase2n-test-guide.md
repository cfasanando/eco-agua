# Restaurant Phase 2N test guide

## Scope

This phase centralizes restaurant identity, operating rules, public QR options, external order options, printable data, currency display, included IGV breakdown, and the optional dine-in service charge.

The current manual SQL script targets only `restaurante_buen_sabor`. The module installer remains the official idempotent installation path for future restaurant instances.

## Preparation

1. Apply the package files.
2. Run `manual_sql/restaurant-settings-phase2n-current-client.sql` against the local MySQL server.
3. Build the application with Java 17.
4. Start the restaurant runtime on port 8084.
5. Sign in with an administrator account.

## Configuration screen

Open:

```text
/admin/restaurant/settings
```

Verify the following sections are present:

- Restaurant identity
- Operations and opening hours
- QR orders and table requests
- Takeaway and delivery
- Taxes and charges
- Tickets, bills, and receipts

Save these sample values:

```text
Trade name: Restaurante El Buen Sabor
Legal name: Restaurante El Buen Sabor E.I.R.L.
RUC: 20123456789
Address: Av. Prueba 123, Iquitos
Phone: 065 123456
WhatsApp: 51999999999
Currency: S/
Order prefix: RSV
Preparation time: 35
Opening: 09:00
Closing: 23:00
Default delivery fee: 5.00
QR maximum distinct items: 12
QR maximum quantity per item: 6
Receipt footer: Gracias por tu preferencia. Vuelve pronto.
```

Reload the page and confirm every value persists.

## Currency formatting

Review restaurant dashboard, cash, daily report, external orders, order detail, QR orders, public menu, bill, and receipt.

Expected examples:

```text
S/ 18.00
S/ 0.50
S/ 0.025
```

Monetary amounts must use a point as the decimal separator. Ingredient unit costs may preserve up to four meaningful decimals.

## QR feature switches

1. Disable **Allow QR orders**.
2. Open `/restaurant/menu?tableId=1` in a private window.
3. Confirm the menu remains visible but the QR order form is hidden.
4. Try to POST an old QR form or reuse an old browser page. The server must reject the request.
5. Re-enable QR orders.
6. Set the maximum quantity per item to `2` and confirm a quantity greater than `2` is rejected server-side.

## Table-request switch

1. Disable **Allow table requests**.
2. Open the public menu with `tableId=1`.
3. Confirm waiter, attention, bill, and note request controls are hidden.
4. Confirm a direct POST to the request endpoint is rejected.
5. Re-enable the option.

## Takeaway and delivery switches

1. Disable delivery and leave takeaway enabled.
2. Open `/admin/restaurant/external-orders`.
3. Confirm only **New takeaway** is shown.
4. Open the new-order route with `serviceType=DELIVERY`; it must fall back to takeaway.
5. Enable delivery, disable takeaway, and repeat the inverse test.
6. Disable both. Historical orders must remain visible, but no creation button should be shown.

## Default delivery fee and preparation time

1. Set the default fee to `5.00` and preparation time to `35` minutes.
2. Create a new delivery order.
3. Confirm the fee starts at `5.00` and the scheduled time is approximately 35 minutes ahead.
4. Confirm the kitchen ticket displays the configured preparation estimate.

## Order prefix

1. Set the prefix to `RSV`.
2. Create a new dine-in or external order.
3. Confirm the generated code starts with `RSV-` and remains unique.

## Included IGV breakdown

1. Enable **Show included IGV** and set `18.00`.
2. Create an order with total `118.00`.
3. Open its bill and receipt.
4. Confirm the total remains `118.00`, while the informational breakdown shows approximately:

```text
Taxable base: S/ 100.00
Included IGV: S/ 18.00
```

IGV in this phase is informational and does not increase the selling price.

## Service charge

1. Enable service charge at `10.00%`.
2. Create a new dine-in order with products totaling `100.00`.
3. Confirm the order detail, bill, receipt, cash screen, and reports show a final total of `110.00`.
4. Add or remove an item and confirm the charge is recalculated once.
5. Create a takeaway and a delivery order. They must not receive the dine-in service charge.
6. Disable the charge. Existing orders keep their stored amount; new dine-in orders use zero charge.

## Printable identity

Open the kitchen ticket, bill, and receipt and verify:

- configured trade name;
- legal name when different;
- RUC;
- address;
- phone;
- optional logo;
- configured receipt footer;
- selected currency symbol.

Disable **Show logo when printing** and confirm the logo disappears without affecting the rest of the identity.

## Roles

- `admin_demo`: may open and save restaurant settings.
- `mozo_demo`, `cocina_demo`, and `caja_demo`: must not see the settings link and must receive access denied if they open the URL directly.

## Regression checklist

Verify these existing flows still work:

- table order creation and kitchen flow;
- QR order approval;
- table requests;
- reservations converted to orders;
- takeaway and delivery;
- ingredient stock consumption and restoration;
- cash payment;
- kitchen ticket, bill, and receipt;
- daily report;
- public menu.
