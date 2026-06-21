# Restaurant Phase 2M-A test guide

## Scope

This phase adds an ingredient catalog, recipe composition per dish, and estimated food cost/margin calculation. Ingredient stock is informational in Phase 2M-A; order confirmation does not deduct ingredient stock yet.

## Access

- Ingredients: `/admin/restaurant/ingredients`
- New ingredient: `/admin/restaurant/ingredients/new`
- Dish recipe: `/admin/restaurant/menu-items/{productId}/recipe`
- Dishes and costing overview: `/admin/restaurant/menu-items`

Use `admin_demo / Demo12345`.

## Suggested test data

Create these ingredients:

| Ingredient | Unit | Unit cost | Stock | Minimum |
|---|---|---:|---:|---:|
| Plátano verde | Unidad | 1.5000 | 30 | 10 |
| Cecina | Gramo | 0.0400 | 5000 | 1000 |
| Aceite | Mililitro | 0.0250 | 3000 | 500 |
| Ensalada | Porción | 1.5000 | 30 | 8 |

Edit `Tacacho con cecina` and set its sale price to `28.00`. Open its recipe and add:

| Ingredient | Quantity |
|---|---:|
| Plátano verde | 2 |
| Cecina | 180 |
| Aceite | 30 |
| Ensalada | 1 |

Expected result:

- Estimated cost: `S/ 12.45`
- Estimated margin: `S/ 15.55`
- Estimated margin percentage: approximately `55.54%`
- Recipe status: `Receta completa`

## Validation cases

1. Create an ingredient and confirm it appears in the list.
2. Attempt to create another ingredient with the same name; it must be rejected.
3. Edit unit cost and confirm dish recipe cost is recalculated.
4. Set an ingredient cost to zero; the recipe must show `Receta incompleta`.
5. Deactivate an ingredient used by a recipe; the recipe must show a warning.
6. Reactivate the ingredient and restore its cost; the recipe must become complete again.
7. Add the same ingredient twice to a dish; the existing quantity must be updated instead of duplicated.
8. Update a recipe quantity and confirm cost and margin change.
9. Remove a recipe item and confirm totals are recalculated.
10. Confirm ingredient stock is not deducted when creating or paying an order in this phase.

## Database validation

```sql
USE restaurante_buen_sabor;

SELECT id, name, unit_code, unit_cost, stock, minimum_stock, active
FROM restaurant_ingredient
ORDER BY name;

SELECT r.id, r.product_id, p.name AS product_name,
       r.ingredient_id, i.name AS ingredient_name,
       r.quantity, i.unit_cost,
       (r.quantity * i.unit_cost) AS line_cost
FROM restaurant_recipe_item r
JOIN restaurant_ingredient i ON i.id = r.ingredient_id
LEFT JOIN product p ON p.id = r.product_id
ORDER BY p.name, i.name;
```
