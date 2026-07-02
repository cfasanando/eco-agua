# GastoClaro Personal - Phase 5B Test Guide

## Goal

Convert the personal finance base module into a usable monthly plan, similar to the spreadsheet currently used by the user.

This phase adds:

- a personal topbar menu for GastoClaro
- `/gasto-claro/monthly-plan`
- monthly income section
- basic cost of living section
- debts to pay section
- manual monthly obligations
- paid and pending amount fields for manual obligations
- projected deficit / surplus summary
- richer debt fields for real personal cases

## Scope

Phase 5B is still not the final payment history workflow. It prepares the operational monthly plan and allows temporary manual paid/pending tracking per obligation.

Payment records and full payment history belong to Phase 5D.

## Main routes

- `/gasto-claro/dashboard`
- `/gasto-claro/monthly-plan`
- `/gasto-claro/debts`
- `/gasto-claro/fixed-expenses`
- `/gasto-claro/income-sources`
- `/gasto-claro/income-events`

## Preconditions

- The user must be authenticated.
- The `personal_finance` module must be enabled for the client runtime.
- Phase 5A must already be applied.

## Test users

Use a client runtime user, for example:

- `admin_demo`
- password: `Demo12345`

## Test 1 - Personal topbar menu

1. Start a client runtime.
2. Log in.
3. Click the user/avatar area in the topbar.
4. Confirm that the dropdown opens.
5. Confirm GastoClaro, Plan mensual and Mis deudas are available.
6. Confirm the left sidebar no longer contains the full GastoClaro tree.

Expected result:

- GastoClaro is accessible from the topbar personal menu.
- Business sidebar remains focused on business modules.

## Test 2 - Monthly plan page

Open:

```text
/gasto-claro/monthly-plan
```

Expected result:

- Header shows selected year and month.
- Summary cards show income, basic living cost, debts, paid, pending and projected balance.
- Sections appear like the spreadsheet: Ingresos, Costo de vida básico, Deudas x pagar.

## Test 3 - Income projection

1. Create an income source.
2. Create an income event for the selected month.
3. Return to Plan mensual.

Expected result:

- Income appears in the Ingresos section.
- Total ingresos increases.
- Projected balance changes.

## Test 4 - Basic cost of living

1. Create fixed expenses:
   - Alquiler
   - Luz
   - Agua
   - Internet
   - Celular
2. Return to Plan mensual.

Expected result:

- Expenses appear under Costo de vida básico.
- Mandatory expenses appear as critical priority.
- Total costo de vida increases.

## Test 5 - Real debt cases

Create debts with the new fields:

- Banco propio
- Banco por tercero with contact name
- Prestamista with 10% or 15% monthly interest
- Deuda a tercero / familiar
- Status: Atrasada or Dejé de pagar

Expected result:

- Debts appear under Deudas x pagar.
- Contact/intermediary appears in notes.
- Monthly interest appears in notes.
- Overdue/stopped debts appear as overdue in the monthly plan.

## Test 6 - Manual monthly obligation

1. Open Plan mensual.
2. Add a manual obligation such as `UTP ciclo`, `Junta`, `Préstamo Nicolas`, or `Comida mínima`.
3. Set group, amount, paid amount, due date, status and priority.
4. Save.

Expected result:

- Manual obligation appears in the correct section.
- Paid and pending columns reflect the values.
- Manual item can be edited or deleted.
- Generated items from fixed expenses/debts cannot be deleted from this page.

## Test 7 - Deficit warning

Create a month where obligations exceed income.

Expected result:

- Saldo proyectado is negative.
- Warning says the month is in red.

## Out of scope for 5B

- Full payment records table
- Real payment history
- Automatic priority engine
- Scenario simulation: “tengo S/ X, qué cubro primero”
- Bank integration
- Notifications

## Next phase

Phase 5C should add obligations, due date pressure, prioritization and the first assisted decision view.
