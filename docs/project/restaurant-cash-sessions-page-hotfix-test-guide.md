# Restaurant cash sessions page hotfix test guide

## Scope

This hotfix only changes the cash-session presentation layer. It does not require SQL.

## Main verification

1. Start the restaurant runtime on port `8084`.
2. Log in as `caja_demo` or an administrator.
3. Open `/admin/restaurant/cash-sessions`.
4. Confirm that the full admin layout is displayed.
5. Confirm that an empty history shows the empty-state message.
6. Open today's cash session when one exists.
7. Close a session and open the printable close summary.

## Browser verification

The Network panel must show a complete `200` response. It must not show:

- `ERR_INCOMPLETE_CHUNKED_ENCODING`
- `Failed to fetch`
- an empty document body

## Regression verification

Confirm that these pages still work:

- `/admin/restaurant/cash`
- `/admin/restaurant/reports`
- `/admin/restaurant/dashboard`
