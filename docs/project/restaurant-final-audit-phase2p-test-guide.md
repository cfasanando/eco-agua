# Restaurant Phase 2P - Final audit and acceptance guide

## Scope

This package closes the Restaurant module after integrating the missing Phase 2O functionality:

- daily cash sessions;
- manual cash income and expense movements;
- expected versus counted cash;
- printable cash close;
- operational reports and CSV export;
- order creator and cashier traceability;
- kitchen timestamps;
- protection against payments after the daily cash close;
- safe module access without activating Restaurant on protected instances.

## Apply the package

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

unzip "$HOME/Downloads/eco-agua-restaurant-final-audit-phase2p-final.zip" \
  -d "$HOME/Downloads"

SRC="$HOME/Downloads/eco-agua-restaurant-final-audit-phase2p-final"

cp -a "$SRC/src/main/java/com/ecoamazonas/eco_agua/restaurant/"* \
      src/main/java/com/ecoamazonas/eco_agua/restaurant/

cp -a "$SRC/src/main/java/com/ecoamazonas/eco_agua/security/SecurityConfig.java" \
      src/main/java/com/ecoamazonas/eco_agua/security/SecurityConfig.java

cp -a "$SRC/src/main/resources/templates/admin/restaurant/"* \
      src/main/resources/templates/admin/restaurant/

cp -a "$SRC/src/main/resources/templates/fragments/sidebar.html" \
      src/main/resources/templates/fragments/sidebar.html

mkdir -p manual_sql docs/project reports scripts
cp -a "$SRC/manual_sql/restaurant-final-audit-phase2p-current-client.sql" manual_sql/
cp -a "$SRC/docs/project/"*.md docs/project/
cp -a "$SRC/reports/"* reports/
cp -a "$SRC/scripts/check-restaurant-final-phase2p.sh" scripts/
```

## Update the current restaurant database

```bash
mysql -u root -p < manual_sql/restaurant-final-audit-phase2p-current-client.sql
```

The recovery SQL contains `USE restaurante_buen_sabor;`. It does not modify the protected Eco Agua or Productos de la Selva databases.

The official installation path for future Restaurant instances remains `RestaurantModuleInstaller`.

## Build and run

```bash
mvn clean -DskipTests package

JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)"

java -jar "$JAR" \
  --spring.config.additional-location="file:runtime-clients/demo_restaurante_buen_sabor/application.properties"
```

## Cash close acceptance flow

Sign in with `caja_demo / Demo12345`.

1. Open `/admin/restaurant/cash-sessions`.
2. Open today's cash with `S/ 100.00`.
3. Register an income of `S/ 20.00`.
4. Register an expense of `S/ 15.00`.
5. Charge one order in cash and another with Yape.
6. Confirm that only the cash payment changes expected cash.
7. Enter counted cash.
8. Close the cash session.
9. Confirm that the difference is calculated correctly.
10. Print the close receipt.
11. Try to charge another order after closing. The application must reject it.

Expected formula:

```text
Expected cash = opening amount + cash sales + manual income - manual expense
Difference    = counted cash - expected cash
```

## Reports acceptance flow

Open `/admin/restaurant/reports`.

Verify:

- date range filters;
- sales totals;
- payment methods;
- dine-in, takeaway and delivery totals;
- products and categories;
- estimated recipe cost and gross profit;
- registered-by and paid-by users;
- QR approved and rejected counts;
- average kitchen time;
- ingredient consumption;
- CSV export with UTF-8 characters and decimal point.

Historical profitability remains estimated because ingredient costs are not snapshotted per sale yet. The report states that it uses registered recipes and current ingredient costs.

## Protected instance safety test

With Eco Agua running on port 8081 and Restaurant disabled:

```bash
curl -I http://localhost:8081/restaurant/menu
```

Expected result: `404` or the platform module-disabled response.

Then verify in the Eco Agua database that no Restaurant tables were created by that request.

With Restaurante El Buen Sabor running on port 8084:

```bash
curl -I http://localhost:8084/restaurant/menu
```

Expected result: `200`.

## Role acceptance matrix

### `admin_demo`

Must access all Restaurant screens, settings, ingredients, reports and cash close.

### `mozo_demo`

Must access dashboard, tables, reservations, QR approval, external orders, new orders, order details and kitchen view. It must not access Restaurant settings, ingredient administration or cash close.

### `cocina_demo`

Must access kitchen, order details, kitchen tickets, table requests and external-order status actions allowed for kitchen. It must not charge orders, edit settings or manage recipes.

### `caja_demo`

Must access cash, cash sessions, reports, receipts, bill requests and paid-order details. It must not create orders, manage recipes or change kitchen status.

## Final regression checklist

- Dine-in order creation and stock reservation.
- QR order approval only once.
- Takeaway and delivery status transitions.
- Recipe ingredient consumption and return.
- Reservation overlap validation.
- Kitchen ticket, bill and receipt.
- Payment method totals.
- Daily cash close.
- CSV export.
- Sidebar contains no duplicate Restaurant entries within the same runtime block.
- Disabled Restaurant module cannot activate itself through a public URL.
