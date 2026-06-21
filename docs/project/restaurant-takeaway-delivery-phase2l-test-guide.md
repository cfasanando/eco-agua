# Restaurant Phase 2L test guide

## Scope

This phase adds a dedicated operational workflow for takeaway and delivery orders without assigning a restaurant table.

Main capabilities:

- Separate `TAKEAWAY` and `DELIVERY` orders.
- Customer name and phone.
- Delivery address and reference.
- Estimated pickup or delivery time.
- Optional delivery fee.
- Operational sequence: pending, confirmed, in kitchen, ready, out for delivery, delivered, or cancelled.
- Payment from the ready stage using cash, Yape, Plin, card, transfer, or other.
- Kitchen ticket, customer bill, and payment receipt.
- Stock reservation when the order is created and stock restoration when it is cancelled before dispatch.
- External-order indicators on the restaurant dashboard.
- Restaurant module installer updated with the required columns and indexes.

## Apply the current demo database SQL

Run only against the current restaurant demo database:

```bash
mysql -u root -p < manual_sql/restaurant-takeaway-delivery-phase2l-current-client.sql
```

The SQL file explicitly selects `restaurante_buen_sabor`. It does not modify the protected Eco Agua or Productos de la Selva databases.

## Build and run

```bash
mvn clean -DskipTests package

JAR="$(find target -maxdepth 1 -type f -name "*.jar" ! -name "*sources*" | head -n 1)"

java -jar "$JAR" \
  --spring.config.additional-location="file:runtime-clients/demo_restaurante_buen_sabor/application.properties"
```

## Main routes

```text
GET  /admin/restaurant/external-orders
GET  /admin/restaurant/external-orders/new
POST /admin/restaurant/external-orders
GET  /admin/restaurant/external-orders/{id}
POST /admin/restaurant/external-orders/{id}/status
POST /admin/restaurant/external-orders/{id}/cancel
```

Existing printable and payment routes are reused:

```text
GET  /admin/restaurant/orders/{id}/kitchen-ticket
GET  /admin/restaurant/orders/{id}/bill
GET  /admin/restaurant/orders/{id}/receipt
POST /admin/restaurant/orders/{id}/pay
```

## Takeaway functional test

1. Sign in as `admin_demo` or `mozo_demo`.
2. Open `/admin/restaurant/external-orders`.
3. Select **Nuevo para llevar**.
4. Enter a customer name and phone.
5. Select at least two products and save.
6. Confirm that the order is created as **Pendiente** and does not yet appear in kitchen.
7. Confirm the order and then send it to kitchen.
8. Open `/admin/restaurant/kitchen` and confirm that it appears there.
9. Mark it ready.
10. Register payment using one of the available payment methods.
11. Print the receipt.
12. Mark it delivered to the customer.
13. Confirm that the paid and delivered order leaves the active external-order queue.

## Delivery functional test

1. Select **Nuevo delivery**.
2. Enter customer name, phone, address, reference, estimated delivery time, and delivery fee.
3. Select products and save.
4. Confirm that the total equals product subtotal plus delivery fee.
5. Follow the sequence:
   - Pending.
   - Confirmed.
   - In preparation.
   - Ready.
   - Out for delivery.
   - Delivered.
6. Register payment at the ready, out-for-delivery, or delivered stage.
7. Confirm that the dashboard, bill, receipt, and cash report include the delivery fee.
8. Confirm that the kitchen ticket includes customer, schedule, and delivery information.

## Stock and cancellation test

1. Record the stock of a product.
2. Create an external order using two units of that product.
3. Confirm that the stock is reserved immediately.
4. Cancel the order while it is pending, confirmed, in kitchen, or ready.
5. Confirm that the stock is restored.
6. Move a delivery order to **En reparto** and confirm that cancellation is no longer available.
7. Confirm that a paid order cannot be cancelled.

## Role test

- `admin_demo`: full access.
- `mozo_demo`: create, confirm, send, dispatch, deliver, and cancel valid external orders.
- `cocina_demo`: view external orders and advance kitchen stages allowed by the interface.
- `caja_demo`: view external orders and register payment, but cannot create or cancel them.

## Database validation

```sql
USE restaurante_buen_sabor;

SHOW COLUMNS FROM restaurant_order;
SHOW INDEX FROM restaurant_order;

SELECT id,
       order_code,
       service_type,
       customer_name,
       customer_phone,
       delivery_address,
       delivery_reference,
       scheduled_at,
       subtotal,
       delivery_fee,
       status,
       payment_method,
       paid_at
FROM restaurant_order
WHERE service_type IN ('TAKEAWAY', 'DELIVERY')
ORDER BY id DESC;
```

## Regression checks

Confirm that these existing flows still work:

- Dine-in orders and table release.
- Reservations converted into dine-in orders.
- QR order approval.
- Kitchen status flow.
- Stock deduction and restoration.
- Daily cash screen and daily report.
- Kitchen ticket, bill, and receipt.
- Public QR menu and table requests.
