# Matrix26 Feature Flags Phase 4C - Route Protection Test Guide

## Scope

Phase 4C applies module flags to direct runtime URLs. Phase 4B already hides navigation entries. This phase blocks direct route access with HTTP 403 when a module is inactive.

This phase does not install modules, uninstall modules, delete data, restart runtimes, execute purge, or touch Matrix26 lifecycle operations.

## Main URL

Client runtime diagnostic page:

```text
/admin/system-modules/visibility
```

The page now shows:

- Effective module flags.
- Number of protected route rules.
- Number of blocked route rules.
- Full route-to-module diagnostics.

## Expected behavior

When a module is active, its route continues to work as before.

When a module is inactive, the direct URL returns HTTP 403 and shows a clear module-disabled message.

Examples:

```text
/admin/restaurant/cash-sessions      -> restaurant_cash
/admin/restaurant/qr-orders          -> restaurant_qr
/admin/restaurant/ingredients        -> restaurant_recipes
/admin/restaurant/reservations       -> restaurant_reservations
/warehouse/products-stock            -> warehouse
/production                          -> production
/accounting                          -> accounting
/marketing/admin                     -> marketing
/admin/academy                       -> academy
```

## Smoke test

1. Open Matrix26 Control Center.
2. Go to:

```text
/control-center/modules/activation
```

3. Disable a non-critical module in a client instance, for example `restaurant_cash` on the restaurant runtime.
4. Regenerate or restart the client runtime if your workflow requires runtime property refresh.
5. Open:

```text
/admin/system-modules/visibility
```

6. Confirm the route diagnostics show the related route as `Bloqueada 403`.
7. Open the direct URL, for example:

```text
/admin/restaurant/cash-sessions
```

8. Confirm the app shows a 403 module-disabled message instead of the feature page.
9. Re-enable the module and confirm the URL works again.

## Matrix26 Control Center safety check

When running the Matrix26 Control Center runtime, this filter is bypassed intentionally. These routes must keep working:

```text
/control-center/operations/dashboard
/control-center/modules/activation
/control-center/operations/acceptance
/control-center/security
```

## Regression checklist

- Login still works.
- Static assets still load.
- Public routes still work when their modules are active.
- Sidebar visibility from Phase 4B still works.
- `/admin/system-modules/visibility` shows flags and route diagnostics.
- Disabled direct URLs return HTTP 403.
- Enabled direct URLs still follow normal role permissions.
- Matrix26 Control Center is not blocked by client module flags.
