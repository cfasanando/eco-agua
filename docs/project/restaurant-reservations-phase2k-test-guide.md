# Restaurant Phase 2K test guide

## Scope

This phase adds restaurant reservations and table scheduling without changing the protected Eco Agua or Productos de la Selva databases.

Main capabilities:

- Daily reservation agenda.
- Pending, confirmed, attended, cancelled, and no-show statuses.
- Customer name, phone, date/time, duration, party size, assigned table, and notes.
- Same-table overlap validation for pending and confirmed reservations.
- Table-capacity validation.
- Upcoming reservations on the restaurant dashboard.
- Next reservation displayed on table cards.
- Reservation conversion into a normal restaurant order.
- Automatic table status refresh between free, reserved, and occupied.
- Restaurant module installer updated with the reservation schema.

## Apply the current demo database SQL

Run only against the current restaurant demo database:

```bash
mysql -u root -p < manual_sql/restaurant-reservations-phase2k-current-client.sql
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
GET  /admin/restaurant/reservations
GET  /admin/restaurant/reservations/new
GET  /admin/restaurant/reservations/{id}/edit
POST /admin/restaurant/reservations
POST /admin/restaurant/reservations/{id}
POST /admin/restaurant/reservations/{id}/status
GET  /admin/restaurant/reservations/{id}/open-order
```

## Functional test

1. Sign in as `admin_demo` or `mozo_demo`.
2. Open `/admin/restaurant/reservations`.
3. Create a confirmed reservation for today or tomorrow using a table whose capacity is greater than or equal to the party size.
4. Confirm that the reservation appears in the selected day's agenda.
5. Try to create another pending or confirmed reservation for the same table with an overlapping time. The system must reject it.
6. Create another reservation at the same time using a different table. It must be accepted.
7. Create a reservation with more people than the selected table capacity. The system must reject it.
8. Open the restaurant dashboard and confirm that upcoming reservations are displayed.
9. For a reservation close to its scheduled time, confirm that the table is shown as reserved when it has no active order.
10. Use the action to convert the reservation into an order.
11. Select at least one available dish and submit the order.
12. Confirm that:
    - A normal restaurant order is created.
    - Customer, phone, table, and notes are preserved.
    - The reservation changes to `ATTENDED`.
    - The reservation links to the created order.
    - The table changes to occupied.
13. Pay or cancel the order and confirm that the table is released, or returns to reserved when another confirmed reservation is close.
14. Test the reservation status actions: confirm, cancel, and no-show.
15. Sign in with `cocina_demo` and `caja_demo` and confirm they do not receive reservation-management access unless their existing restaurant table permissions allow it.

## Database validation

```sql
USE restaurante_buen_sabor;

SHOW TABLES LIKE 'restaurant_reservation';
SHOW COLUMNS FROM restaurant_reservation;
SHOW INDEX FROM restaurant_reservation;

SELECT id,
       reservation_code,
       table_id,
       customer_name,
       customer_phone,
       reservation_at,
       duration_minutes,
       party_size,
       status,
       order_id
FROM restaurant_reservation
ORDER BY reservation_at DESC, id DESC;
```

## Regression checks

Confirm that these existing flows still work:

- Restaurant dashboard and table cards.
- New manual order.
- QR order approval.
- Kitchen status flow.
- Stock deduction and restoration.
- Daily cash screen.
- Kitchen ticket, bill, and receipt.
- Public QR menu and table requests.
