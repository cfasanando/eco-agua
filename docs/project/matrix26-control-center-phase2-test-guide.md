# Matrix26 Control Center Phase 2 test guide

## Scope

Phase 2 adds visual administration of instance metadata inside the dedicated Matrix26 runtime.

It does not create, drop, copy, migrate, start, stop, or restart any operational portal or database.
All writes remain inside `matrix26_platform_control`.

## Runtime

- URL: `http://localhost:8091`
- Profile: `matrix26_control`
- Database: `matrix26_platform_control`

## Build and startup

```bash
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

Login:

```text
matrix_admin / Matrix26Demo123!
```

## Test 1: existing instances remain intact

Open:

```text
http://localhost:8091/control-center/instances
```

Confirm that these instances remain registered:

- Eco Agua del Amazonas — 8081
- Productos de la Selva Belén — 8082
- Restaurante El Buen Sabor — 8084

Edit only a harmless Matrix26 metadata field such as notes, save, restart Matrix26, and confirm the change remains. The initializer must not overwrite existing records on startup.

## Test 2: register a new metadata-only instance

Open:

```text
http://localhost:8091/control-center/instances/new
```

Example:

```text
Code: test-instance
Name: Test Instance
Database: test_instance
Runtime: test_instance
Port: 8092
URL: http://localhost:8092
Monitoring: enabled
Protected: disabled
```

Save and confirm:

- the instance appears in the list;
- no database named `test_instance` is created;
- no runtime is started;
- the audit page records the user and action.

## Test 3: duplicate validation

Attempt to register another instance using any existing:

- code;
- port;
- database name;
- runtime profile.

The form must display a clear validation error and must not save a duplicate record.

## Test 4: instance detail and health history

Open an instance detail page and use **Check now**.

Confirm:

- online/offline status is refreshed;
- HTTP status and response time are stored;
- the history table displays the new result;
- the operational database remains untouched.

Pause monitoring and return to the instance list. The status must display **Monitoring paused** rather than offline.

## Test 5: administrative protection

From instance detail, toggle protection after confirming the browser dialog.

Confirm:

- only `protected_instance` and `management_mode` change in Matrix26;
- the portal keeps running normally;
- the audit log records the actor and timestamp.

Restore protection for the three production/demo reference instances after testing.

## Test 6: module declarations

Open:

```text
http://localhost:8091/control-center/modules
```

Change the declarations for a test instance and save.

Confirm:

- the selection remains after reload;
- an audit record is created;
- no module installer runs;
- no schema change occurs in the operational database.

## Test 7: audit

Open:

```text
http://localhost:8091/control-center/audit
```

Confirm that create, edit, monitoring, protection, manual health check, and module declaration actions show:

- date and time;
- authenticated username;
- instance;
- action code;
- summary;
- before/after snapshots when applicable.

## Expected database impact

New Matrix26-only table:

```text
matrix26_instance_audit_log
```

Existing Matrix26 tables used:

```text
platform_business_client
platform_client_module
platform_module_catalog
matrix26_instance_health_check
```

No SQL is executed against:

```text
eco_agua
productos_selva_belen
restaurante_buen_sabor
```
