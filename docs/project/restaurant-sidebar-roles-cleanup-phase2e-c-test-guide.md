# Restaurant sidebar roles cleanup - Phase 2E-C

## Goal

Keep the restaurant runtime navigation consistent with the active user role. A user should not see menu links or buttons that lead to 403 pages.

## Expected navigation

### admin_demo
- Inicio -> restaurant dashboard
- Restaurante: dashboard, tables, new order, kitchen, cash, daily report, public menu

### mozo_demo
- Inicio -> restaurant dashboard
- Restaurante: dashboard, tables, new order, kitchen read-only, public menu
- No cash/report links

### cocina_demo
- Inicio -> kitchen
- Restaurante: kitchen, public menu
- No dashboard, cash or new order links

### caja_demo
- Inicio -> cash
- Restaurante: cash, daily report, public menu
- No Commercial / CRM empty group
- No dashboard link that causes 403

## Test steps

1. Build the app with `mvn clean -DskipTests package`.
2. Run the restaurant runtime on port 8084.
3. Test each account in a private browser window.
4. Confirm that visible menu links open without 403.
5. Confirm that the generic `Comercial / CRM` group is hidden in the restaurant runtime.
