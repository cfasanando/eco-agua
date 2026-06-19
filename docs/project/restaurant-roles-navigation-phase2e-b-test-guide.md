# Restaurant roles navigation Phase 2E-B - Test guide

## Goal

Validate that every restaurant role only sees links it can use, and every visible restaurant link opens without a 403.

## Users

- `admin_demo` / `Demo12345`: owner/admin, full restaurant access.
- `mozo_demo` / `Demo12345`: restaurant dashboard, tables, new comanda, order detail and read-only kitchen status.
- `cocina_demo` / `Demo12345`: kitchen screen and order detail, can update kitchen statuses.
- `caja_demo` / `Demo12345`: cash screen, daily report and order detail, can charge orders.

## Smoke test

1. Build and start the restaurant runtime on port 8084.
2. Log in with each user separately in a private/incognito window.
3. Confirm the post-login landing page:
   - Admin: `/admin/restaurant/dashboard`
   - Waiter: `/admin/restaurant/dashboard`
   - Kitchen: `/admin/restaurant/kitchen`
   - Cashier: `/admin/restaurant/cash`
4. Click every visible sidebar item.
5. Click every visible action button in the current page.
6. Confirm no visible action returns 403.

## Expected navigation

### Admin

Can see and use the full restaurant menu.

### Waiter

Can see:

- Inicio
- Panel restaurante
- Mesas y salón
- Nueva comanda
- Cocina
- Carta pública

Waiter can open Cocina to check status, but cannot use kitchen-only status update buttons.

### Kitchen

Can see:

- Inicio
- Cocina
- Carta pública

Kitchen can open order detail and change kitchen statuses, but cannot charge orders.

### Cashier

Can see:

- Inicio
- Caja diaria
- Carta pública

Cashier can open order detail and charge orders, but cannot create/edit comandas or update kitchen statuses.
