# Platform module sidebar links hotfix

## Root cause

The module-management links were placed inside a sidebar block that required:

- `!restaurantRuntime`
- `moduleSystemSectionEnabled`
- `modulePlatformSettingsEnabled`

A restaurant runtime could therefore open the URLs directly but could never display the links.

## Correction

A dedicated **Plataforma / Administración técnica** group is now visible to:

- `ROLE_SUPER_ADMIN`
- `ROLE_OWNER`
- `ADMIN_PRINC`
- `ADMIN`
- `admin_config`

It is independent from business-module feature flags.

The group includes:

- Módulos del sistema
- Instalaciones de módulos

The group opens automatically when either page is active.

## Test

1. Sign in as `admin_demo`.
2. Open `/admin/system-modules/installations`.
3. Confirm that the **Plataforma** section is visible in the sidebar.
4. Confirm that **Administración técnica** is expanded.
5. Confirm that **Instalaciones de módulos** is selected.
6. Open **Módulos del sistema** from the sidebar.
7. Confirm there are no duplicate links.
8. Sign in with waiter, kitchen and cashier roles and confirm the technical section is hidden.

No SQL or Java changes are required.
