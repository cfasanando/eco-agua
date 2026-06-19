# Restaurant Operational Dashboard - Phase 2A

## Goal

Improve the restaurant home/dashboard so it behaves like an operational control panel instead of a generic summary page.

## What changed

- Adds an operational table board to the restaurant dashboard.
- Shows each active table as a card with current status.
- Shows the active order attached to the table when it exists.
- Shows elapsed waiting time for occupied tables.
- Shows current order total and order status.
- Adds quick actions to create an order, open kitchen, or mark an order as paid.
- Adds a kitchen queue widget on the dashboard.
- Keeps the existing active orders table as a detailed fallback.
- Allows opening the new order screen with a preselected table using `?tableId=`.

## Files changed

- `src/main/java/com/ecoamazonas/eco_agua/restaurant/RestaurantController.java`
- `src/main/java/com/ecoamazonas/eco_agua/restaurant/RestaurantService.java`
- `src/main/java/com/ecoamazonas/eco_agua/restaurant/RestaurantTableBoardRow.java`
- `src/main/resources/templates/admin/restaurant/dashboard.html`
- `src/main/resources/templates/admin/restaurant/order_form.html`

## Test steps

1. Rebuild the app.

```bash
mvn clean -DskipTests package
```

2. Start the restaurant runtime.

```bash
bash scripts/run-restaurant-demo.sh
```

If the helper script is not available, start it directly:

```bash
JAR="$(find target -maxdepth 1 -type f -name "*.jar" ! -name "*sources*" | head -n 1)"

java -jar "$JAR" \
  --spring.config.additional-location="file:runtime-clients/demo_restaurante_buen_sabor/application.properties"
```

3. Open the restaurant dashboard.

```text
http://localhost:8084/admin/restaurant/dashboard
```

4. Validate these behaviors:

- Free tables show the `Crear comanda` action.
- Occupied tables show order code, elapsed time, total, status, and payment action.
- The kitchen queue shows pending kitchen orders.
- `Nueva comanda` from a table opens the order form with that table selected.
- Marking an active order as paid returns to the dashboard and frees the table.

## Notes

This phase does not change database structure. It only reads existing restaurant tables, active orders, and order items.
