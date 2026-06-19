# Restaurant Phase 2D - Daily cash and sales report

## Scope

This phase adds a daily cash screen and printable restaurant sales report.

## URLs

- `/admin/restaurant/cash`
- `/admin/restaurant/cash?date=YYYY-MM-DD`
- `/admin/restaurant/reports/daily?date=YYYY-MM-DD`

## Test flow

1. Start the restaurant runtime on port `8084`.
2. Log in as `admin_demo`.
3. Create a comanda and pay it using different payment methods.
4. Open `/admin/restaurant/cash`.
5. Confirm paid orders, open orders, payment totals and average ticket.
6. Open the printable daily report.

## Notes

The manual SQL only touches `restaurante_buen_sabor` and is safe to rerun.
