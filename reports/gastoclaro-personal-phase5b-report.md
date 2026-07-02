# GastoClaro Personal - Phase 5B Report

## Summary

Phase 5B turns GastoClaro from a CRUD-only personal module into a monthly operational planning tool.

The main new screen is:

```text
/gasto-claro/monthly-plan
```

It mirrors the user's spreadsheet structure:

- Ingresos
- Costo de vida básico
- Deudas x pagar
- Egresos
- Pagado
- Pendiente
- Saldo / déficit

## Implemented backend changes

### New domain objects

- `PersonalFinancePaymentObligation`
- `PersonalFinancePaymentObligationRepository`
- `PersonalFinanceMonthlyPlan`
- `PersonalFinanceMonthlyPlanItem`
- `PersonalFinanceObligationGroup`
- `PersonalFinanceObligationSourceType`
- `PersonalFinanceObligationStatus`
- `PersonalFinancePriority`
- `PersonalFinanceDebtHolderType`

### Debt model improvements

The debt model now supports real personal cases:

- debts in the user's own name
- debts in a third party's name
- contact/intermediary name
- private lenders
- bank debt through third party
- stopped-payment status
- overdue status
- negotiation status
- priority
- high monthly interest

### Schema changes

New table:

```text
personal_finance_payment_obligation
```

New columns for existing table:

```text
personal_finance_debt.holder_type
personal_finance_debt.contact_name
personal_finance_debt.priority
```

The initializer includes a safe upgrade step using `information_schema` checks before altering existing tables.

## Implemented frontend changes

### Personal topbar menu

The user/avatar in the topbar now opens a personal dropdown menu with:

- GastoClaro
- Plan mensual
- Mis deudas
- logout

The business sidebar no longer shows the full GastoClaro tree.

### Monthly plan UI

The monthly plan page includes:

- month/year selector
- projected balance hero
- summary cards
- income table
- basic living cost table
- debts table
- other commitments section
- manual obligation form
- quick guidance panel

## Static checks

The static check passed:

```text
GastoClaro Personal Phase 5B static checks passed.
```

## Compile note

The sandbox could not run Maven because the wrapper attempted to download Maven from the internet and network access is unavailable.

Run locally:

```bash
mvn clean -DskipTests package
```

## Safety

No destructive SQL was added.

This phase does not:

- delete personal finance data
- truncate tables
- purge instances
- touch backups
- touch restore flows
- affect Matrix26 Control Center routes
