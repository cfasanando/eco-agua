# Checklist de rutas principales V1

Probar como usuario administrador/dueño después de compilar y levantar la app.

## Inicio y configuración

- `/login`
- `/home`
- `/dashboard/business`
- `/admin/platform-settings`
- `/admin/system-modules`
- `/admin/users`
- `/admin/roles-permissions`

## Ventas / CRM

- `/admin/clients`
- `/admin/clients/portfolio`
- `/admin/clients/follow-up`
- `/orders`
- `/orders/new`
- `/income/sales`
- `/income/credit`
- `/income/sales/channels`

## Finanzas / caja

- `/cashflow`
- `/cashflow/monthly-summary`
- `/income/others`
- `/expenses`
- `/expenses/accounts-payable`
- `/expenses/monthly-summary`

## Contabilidad interna

- `/accounting/control-panel`
- `/accounting/journal-entries`
- `/accounting/draft-review`
- `/accounting/journal-book`
- `/accounting/ledger`
- `/accounting/trial-balance`
- `/accounting/income-statement`
- `/accounting/balance-sheet`
- `/accounting/period-close`

## Productos, costos y rentabilidad

- `/admin/products`
- `/admin/products/profitability`
- `/admin/products/channel-profitability`
- `/admin/products/price-simulator`
- `/admin/products/1/full-cost`

## Inventario / almacén

- `/warehouse/products-stock`
- `/warehouse/supplies-stock`
- `/admin/supplies`
- `/admin/suppliers`
- `/reorder-agenda`

## Delivery

- `/delivery`
- `/admin/delivery-zones`

## Marketing

- `/marketing/admin/tools`
- `/marketing/admin/campaigns`
- `/marketing/admin/ideas`
- `/marketing/admin/publication-plan`
- `/marketing/admin/actions-report`
- `/admin/promotions`
- `/admin/blog`

## RRHH

- `/admin/personnel`
- `/admin/personnel/monthly-payroll`
- `/admin/personnel/obligations`
- `/admin/personnel/attendance`
- `/admin/personnel/payments`
- `/admin/personnel/employees/1`

## Producción

- `/production/overview`
- `/production/new`
- `/production`
- `/production/recipes`
- `/production/reports`
- `/production/quality`
- `/production/traceability`
- `/production/expiry`
- `/production/planning`
- `/production/capacity`
- `/production/material-requirements`
- `/production/material-purchase-request`
- `/production/material-purchase-draft`
- `/production/schedule`

## Portal público

- `/`
- `/portal`
- `/catalogo`
- `/blog`

## Criterio de aprobación

Cada ruta debe cumplir uno de estos resultados:

- Carga pantalla completa.
- Redirige al login si no hay sesión.
- Devuelve acceso denegado si el rol no corresponde.

No debe quedarse en blanco ni lanzar error 500.
