# Restaurant final acceptance checklist

## Installation and isolation

- [ ] Restaurant installer is idempotent.
- [ ] Cash tables are created only when Restaurant is installed.
- [ ] Eco Agua 8081 remains unchanged while Restaurant is disabled.
- [ ] Productos de la Selva 8082 remains unchanged while Restaurant is disabled.
- [ ] A direct public Restaurant URL cannot activate a disabled module.

## Navigation and permissions

- [ ] No blank Restaurant pages.
- [ ] No duplicate menu items in one sidebar runtime block.
- [ ] Every visible link is authorized for the current role.
- [ ] Login redirects each Restaurant role to a usable screen.

## Orders

- [ ] Dine-in order.
- [ ] QR order approval and rejection.
- [ ] Takeaway order.
- [ ] Delivery order.
- [ ] Kitchen status flow.
- [ ] Cancellation restores stock exactly once.
- [ ] Payment cannot be repeated.

## Inventory and costing

- [ ] Product stock mode.
- [ ] Recipe stock mode.
- [ ] No-stock-control mode.
- [ ] Ingredient consumption.
- [ ] Ingredient return.
- [ ] Low-stock and unavailable behavior.
- [ ] Cost and margin format uses decimal point.

## Cash and reports

- [ ] Open cash session.
- [ ] Register income.
- [ ] Register expense.
- [ ] Expected cash calculation.
- [ ] Counted cash and difference.
- [ ] Close cash session.
- [ ] Reject payment after daily close.
- [ ] Printable close receipt.
- [ ] Sales and payment reports.
- [ ] Product and category reports.
- [ ] User and cashier traceability.
- [ ] Kitchen time report.
- [ ] Ingredient report.
- [ ] CSV export opens correctly in Excel.
