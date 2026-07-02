# GastoClaro Personal - Phase 5A Test Guide

## Scope

This phase adds the first internal version of GastoClaro as a private per-user module inside the current Eco Agua / Matrix26 application.

Implemented in 5A:

- `/gasto-claro/dashboard`
- `/gasto-claro/debts`
- `/gasto-claro/fixed-expenses`
- `/gasto-claro/income-sources`
- `/gasto-claro/income-events`
- Runtime module flag `personal_finance`
- Sidebar section `Personal > GastoClaro`
- Per-user data isolation with `user_id`
- Base schema initializer for the four MVP structural tables

Not implemented yet:

- Payment obligations
- Payment records
- Real due-date calendar
- Monthly snapshots
- Payment prioritization scenarios

## Apply

```bash
bash scripts/configure-gastoclaro-personal-phase5a.sh
bash scripts/check-gastoclaro-personal-phase5a.sh
mvn clean -DskipTests package
```

## Manual verification

1. Start a client runtime, for example Agua Eco or Restaurante.
2. Login with `admin_demo` or another active user.
3. Open `/gasto-claro/dashboard`.
4. Create one debt.
5. Create one fixed expense.
6. Create one income source.
7. Create one income event for the current month.
8. Return to dashboard and verify the summary cards update.
9. Login with a different user and verify that the previous user's records are not shown.
10. Disable `personal_finance` from system modules and verify `/gasto-claro/**` returns 403.

## Expected result

The module provides a usable personal base for tracking debts, fixed expenses and planned income without mixing the data with business finance/accounting modules.
