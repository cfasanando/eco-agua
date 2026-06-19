# Restaurant white page template hotfix test guide

## Scope

This hotfix targets the authenticated restaurant pages that opened as a blank page after login.

## Root cause

The restaurant admin templates referenced the fragment `fragments/scripts_admin :: scripts`, but the shared fragment file defines `adminScripts`. This can break Thymeleaf rendering at the end of the page.

## Expected result

The following routes should render with the admin layout:

- `/admin/restaurant/dashboard`
- `/admin/restaurant/tables`
- `/admin/restaurant/orders/new`
- `/admin/restaurant/kitchen`

The public route should keep working:

- `/restaurant/menu`

## Runtime

Start the restaurant runtime with:

```bash
bash scripts/run-restaurant-demo.sh
```

Then open:

```text
http://localhost:8084/login
```

Use:

```text
admin_demo / Demo12345
```
