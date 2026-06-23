# Platform modular foundation Phase 3A test guide

## Purpose

Validate explicit, versioned and safe module installation without modifying protected instances.

## Runtime policy

Schema-changing module actions are blocked unless the runtime contains:

```properties
ecoagua.modules.installation-allowed=true
```

Generated protected runtimes receive `false`. Managed demo runtimes receive `true`.

## Apply to the restaurant demo

1. Enable module installation only for the managed restaurant runtime:

```bash
bash scripts/allow-module-installation-for-runtime.sh demo_restaurante_buen_sabor
```

2. Build and start the restaurant instance.
3. Open:

```text
http://localhost:8084/admin/system-modules/installations
```

4. Confirm the page displays database `restaurante_buen_sabor`.
5. If Restaurant is shown as `Installed without registry`, use **Synchronize**.
6. Confirm `platform_module_installation` contains one Restaurant row with version `2026.06.22.1`.
7. Disable Restaurant and confirm:
   - Restaurant routes return 404.
   - Restaurant tables remain present.
   - Existing data remains present.
8. Activate Restaurant again and confirm the installer runs idempotently.

The optional manual synchronization script is:

```bash
mysql -u root -p < manual_sql/platform-modular-foundation-phase3a-current-client.sql
```

It explicitly selects only `restaurante_buen_sabor`.

## Protected instances

Start Eco Agua and Productos de la Selva without adding the installation property.

Open:

```text
/admin/system-modules/installations
```

Expected result:

- The page is read-only.
- A warning indicates schema operations are locked.
- No Install, Synchronize, Activate or Disable button is displayed.
- No module table is created by opening the page.

## Disposable laboratory on port 8085

The laboratory is generated from the existing restaurant structure without changing the source database. It copies only authentication/configuration data and removes all `restaurant_*` tables from the target.

```bash
bash scripts/setup-modular-lab.sh restaurante_buen_sabor eco_agua_modular_lab modular_lab 8085
mvn clean -DskipTests package
bash scripts/run-modular-lab.sh modular_lab
```

If the laboratory already exists and can be discarded:

```bash
RESET_LAB=1 bash scripts/setup-modular-lab.sh restaurante_buen_sabor eco_agua_modular_lab modular_lab 8085
```

Open:

```text
http://localhost:8085/admin/system-modules/installations
```

Expected initial state:

- Database: `eco_agua_modular_lab`
- Restaurant schema: Missing
- Registry: Not registered
- Runtime installation: Allowed

Install Restaurant without demo data.

Expected final state:

- Restaurant schema: Installed
- Registry: Registered
- Installed version: `2026.06.22.1`
- Status: Active
- `module.restaurant.enabled=true`

Disable Restaurant and activate it again. The same ordered installation steps must remain idempotent and must not duplicate tables, indexes, settings or demo data.

## Database verification

```sql
USE eco_agua_modular_lab;

SELECT *
FROM platform_module_installation
ORDER BY module_key;

SELECT variable, value
FROM platform_setting
WHERE variable = 'module.restaurant.enabled';

SELECT COUNT(*) AS restaurant_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name LIKE 'restaurant\\_%';
```

## Acceptance criteria

- Protected runtimes cannot execute schema operations.
- Opening a business route never installs a module.
- Installation state is stored per database.
- A failed installation is recorded as `FAILED` and the module remains disabled.
- Disabling a module preserves schema and data.
- Reinstalling or updating Restaurant is idempotent.
- Existing Restaurant remains fully operational after synchronization.
