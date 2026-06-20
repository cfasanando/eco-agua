# Restaurant stock UI polish - Phase 2G-B

## Objective

Make the stock actions in the restaurant menu item list clearer for daily operators.

## What changed

- The standalone replenishment input is now labeled as `Cantidad a reponer`.
- The replenish action explains that it adds the entered quantity to current stock.
- The availability toggle now uses clearer text: `Agotar` / `Marcar disponible`.
- Actions are grouped into quick item actions and stock replenishment actions.

## Test

1. Open `/admin/restaurant/menu-items`.
2. Verify the action column no longer shows an unexplained `5` field.
3. Pick a product with stock 2.
4. Leave `Cantidad a reponer` as 5 and click `Reponer stock`.
5. Confirm stock becomes 7.
6. Click `Agotar` and confirm the product is marked unavailable.
7. Click `Marcar disponible` and confirm the product returns to available state.

## Expected result

Restaurant operators should understand immediately that the quantity field is used only for stock replenishment.
