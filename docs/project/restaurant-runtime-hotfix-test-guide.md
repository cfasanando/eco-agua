# Restaurant runtime hotfix test guide

## Goal

Fix the restaurant demo runtime so the sidebar links work in `http://localhost:8084` without touching the protected Eco Agua and Productos de la Selva instances.

## What this fixes

- Restaurant clients now force the main `restaurant` module on during provisioning.
- Restaurant clients now generate the restaurant schema in the bootstrap SQL.
- Restaurant demo data now includes tables and a sample kitchen order.
- Runtime properties now expose `ecoagua.features.restaurant=true` for restaurant profiles.
- The current `restaurante_buen_sabor` database can be repaired with one idempotent SQL script.

## Apply

1. Copy the files from the hotfix ZIP into the project.
2. Run `manual_sql/restaurant-runtime-current-client-repair.sql` against MySQL.
3. Compile and restart the restaurant runtime.

## Expected checks

Open these URLs after login as `admin_demo / Demo12345`:

- `/admin/restaurant/dashboard`
- `/admin/restaurant/tables`
- `/admin/restaurant/orders/new`
- `/admin/restaurant/kitchen`
- `/restaurant/menu`

The dashboard should show restaurant summary cards, tables should list demo tables, new order should show demo plates, kitchen should show the sample order, and the public menu should load without login.
