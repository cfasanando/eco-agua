# Restaurant decimal format test guide — Phase 2M-A-B

## Goal

Use a dot as the decimal separator and avoid unnecessary trailing zeroes while preserving precision for very small ingredient costs.

## Expected monetary format

- `28` → `S/ 28.00`
- `12.45` → `S/ 12.45`
- `1.5000` → `S/ 1.50`
- `0.0400` → `S/ 0.04`
- `0.0250` → `S/ 0.025`
- `0.0023` → `S/ 0.0023`

## Expected quantity format

- `30.0000` → `30`
- `1.5000` → `1.5`
- `0.0250` → `0.025`

## Manual verification

1. Open `/admin/restaurant/ingredients`.
2. Verify unit costs use a dot and no unnecessary zeroes.
3. Open the Tacacho con cecina recipe.
4. Verify price, total cost and margin use exactly two decimals.
5. Verify ingredient unit costs retain up to four decimals only when needed.
6. Verify recipe quantities do not display `.0000`.
7. Edit an ingredient with cost `0.0023` and confirm the full precision remains visible.
8. Save the ingredient and verify the value is preserved.
