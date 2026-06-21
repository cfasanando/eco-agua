# Restaurant phase 2M-B test guide

## Scope

This phase connects restaurant recipes to real ingredient inventory while preserving the existing finished-product stock workflow.

Each dish has one explicit stock control mode:

- `PRODUCT`: deducts the finished dish stock from `product.stock`.
- `RECIPE`: deducts ingredient quantities defined in the dish recipe.
- `NONE`: does not modify inventory.

The selected mode is copied into each order item. This protects open orders if the dish configuration changes later.

## Prerequisites

1. Apply `manual_sql/restaurant-ingredient-stock-phase2m-b-current-client.sql` only to `restaurante_buen_sabor`.
2. Compile and restart the restaurant runtime on port `8084`.
3. Sign in as `admin_demo / Demo12345`.
4. Confirm that the Tacacho con cecina recipe contains:
   - Plátano verde: 2 units
   - Cecina: 180 g
   - Aceite: 30 ml
   - Ensalada: 1 portion

## Test 1: configure recipe stock control

1. Open `/admin/restaurant/menu-items`.
2. Edit **Tacacho con cecina**.
3. Set **Control de stock** to **Por ingredientes de la receta**.
4. Keep the dish visible and available.
5. Save.
6. Return to the dish list.

Expected:

- The dish shows `Por receta`.
- The list shows available portions and the limiting ingredient.
- Finished-product replenishment is not offered for this dish.

## Test 2: create a dine-in order

1. Open `/admin/restaurant/ingredients` and write down the current stocks.
2. Create a new dine-in order with **Tacacho con cecina x2**.
3. Return to the ingredient list.

Expected deductions:

- Plátano verde: `-4`
- Cecina: `-360`
- Aceite: `-60`
- Ensalada: `-2`

Open each ingredient movement history and confirm a `Consumo por pedido` movement with the order reference.

## Test 3: reduce quantity

1. Open the created order.
2. Change Tacacho con cecina from quantity `2` to `1`.
3. Recheck ingredient stocks and movement history.

Expected returns:

- Plátano verde: `+2`
- Cecina: `+180`
- Aceite: `+30`
- Ensalada: `+1`

The movement history must show `Devolución` entries.

## Test 4: cancel the order

1. Cancel the remaining order.
2. Recheck ingredient stocks.

Expected:

- The remaining ingredients are restored.
- The final stock equals the stock recorded before Test 2.
- Repeating the cancellation must not restore stock a second time.

## Test 5: QR order approval

1. Open `/restaurant/menu?tableId=1` in a private browser window.
2. Submit Tacacho con cecina from the QR menu.
3. Check ingredient stock before approval.
4. Sign in as admin or waiter and open `/admin/restaurant/qr-orders`.
5. Approve the QR order.

Expected:

- Submitting the QR request does not deduct ingredients.
- Approving it creates or updates the table order and deducts ingredients once.
- The same QR request cannot be approved twice.

## Test 6: takeaway and delivery

1. Create a takeaway order containing a recipe-controlled dish.
2. Confirm the ingredient deduction.
3. Cancel it before delivery and confirm ingredient restoration.
4. Repeat with a delivery order.

Expected:

- Both workflows use the same ingredient allocation rules.
- A paid, delivered, or out-for-delivery order cannot be cancelled through the protected cancellation flow.

## Test 7: limiting ingredient and public availability

1. Reduce one ingredient stock so it cannot prepare one complete portion.
2. Open `/admin/restaurant/menu-items`.
3. Open `/restaurant/menu?tableId=1`.
4. Open a new order form.

Expected:

- The dish shows zero available portions and the limiting ingredient.
- The dish is excluded from the public menu and new-order selectors.
- Existing order history remains unchanged.

## Test 8: replenish ingredient stock

1. Open `/admin/restaurant/ingredients`.
2. Enter a replenishment quantity for the limiting ingredient.
3. Click **Reponer**.
4. Open its movement history.
5. Recheck the dish and public menu.

Expected:

- Ingredient stock increases by the entered quantity.
- A `Reposición` movement is recorded.
- The available portion count is recalculated.
- The dish becomes saleable again if it is active, visible, and not manually paused.

## Test 9: other stock modes

### Finished-product stock

1. Configure a dish as `Por plato terminado`.
2. Create an order and confirm only `product.stock` decreases.
3. Cancel and confirm finished-product stock returns.

### No stock control

1. Configure a dish as `Sin control de stock`.
2. Create, edit, and cancel an order.

Expected:

- No product or ingredient stock changes.

## Database verification

```sql
USE restaurante_buen_sabor;

SELECT id, name, restaurant_stock_control, stock
FROM product
ORDER BY id;

SELECT id, name, unit_code, stock, minimum_stock
FROM restaurant_ingredient
ORDER BY id;

SELECT id, order_id, product_id, product_name, quantity, stock_control_mode
FROM restaurant_order_item
ORDER BY id DESC
LIMIT 30;

SELECT order_item_id, ingredient_id, ingredient_name,
       quantity_per_unit, quantity_reserved
FROM restaurant_order_item_ingredient
ORDER BY id DESC
LIMIT 100;

SELECT ingredient_id, movement_type, quantity_change, balance_after,
       order_id, order_item_id, notes, created_at
FROM restaurant_ingredient_movement
ORDER BY id DESC
LIMIT 100;
```
