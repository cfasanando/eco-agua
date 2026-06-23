# Matrix26 Control Center - Phase 1 test guide

## Scope

Phase 1 introduces a dedicated control runtime:

- URL: `http://localhost:8091`
- Runtime: `matrix26_control`
- Database: `matrix26_platform_control`
- Mode: read-only for managed business instances

The following operational databases must remain unchanged:

- `eco_agua`
- `productos_selva_belen`
- `restaurante_buen_sabor`

## Build

```bash
mvn clean -DskipTests package
```

## Start

```bash
bash scripts/run-matrix26-control.sh
```

The database is created through the existing Spring JDBC connection. No `mysql.exe`,
`mysqldump`, XAMPP, Laragon, or external database client is required.

## Default local credentials

```text
matrix_admin
Matrix26Demo123!
```

The password can be overridden before the first startup:

```bash
export MATRIX26_ADMIN_PASSWORD='A-strong-local-password'
bash scripts/run-matrix26-control.sh
```

## Acceptance checks

1. Open `http://localhost:8091`.
2. Confirm the login page shows Matrix26 branding.
3. Log in with the Matrix26 administrator.
4. Confirm the dashboard lists:
   - Eco Agua del Amazonas on 8081
   - Productos de la Selva Belén on 8082
   - Restaurante El Buen Sabor on 8084
5. Confirm each instance displays its independent database name.
6. Confirm online/offline status and response time are shown.
7. Confirm the Instances screen has no delete, reinstall, stop, or database-edit actions.
8. Confirm the Modules screen is informational only.
9. Confirm business routes entered on port 8091 redirect to `/control-center`.
10. Confirm ports 8081, 8082 and 8084 continue working with their original data.

## Database isolation check

The Matrix26 database must contain central metadata only. The application-managed
schema includes authentication, platform inventory, module declarations, and health
history tables. It must not copy orders, sales, customers, products, or restaurant data.

## Stop

Use `Ctrl+C` in the terminal running port 8091. Stopping Matrix26 does not stop any
managed business portal.
