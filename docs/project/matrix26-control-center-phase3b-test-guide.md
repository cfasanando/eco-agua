# Matrix26 Control Center Phase 3B Test Guide

## Goal

Validate controlled provisioning from the Matrix26 UI. A READY plan can create a
new isolated database, install the structural core, create the initial administrator,
execute target-compatible module installers, generate a runtime folder, and register
the new instance in Matrix26.

The existing protected instances must remain unchanged:

- `eco_agua` / port 8081;
- `productos_selva_belen` / port 8082;
- `restaurante_buen_sabor` / port 8084.

The source template database is read only. Phase 3B copies table definitions, not
business rows.

## Apply and build

Stop Matrix26 with `Ctrl+C`, copy the Phase 3B package over the repository, and run:

```bash
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/provisioning
```

## Safe provisioning plan

Create a plan with unused resources:

```text
Business name: Matrix26 Restaurant Laboratory
Business type: restaurant
Instance code: matrix26-restaurant-lab
Database: matrix26_restaurant_lab
Runtime: matrix26_restaurant_lab
Port: 8092
URL: http://localhost:8092
Administrator: admin_demo
Module: Restaurant
Demo data: enabled for the first laboratory test
```

The plan must finish as `READY`.

## Explicit execution

Open the plan detail page and verify:

- the database, runtime, port, URL, administrator, and selected modules;
- all steps are prepared;
- protected databases are not listed as targets;
- the execution panel is visible only for `READY` or `FAILED` plans.

Enter the exact plan reference, provide an initial password of at least ten
characters, confirm the password, acknowledge the warning, and execute.

Expected result:

```text
Job status: COMPLETED
Database step: COMPLETED
Core step: COMPLETED
Administrator step: COMPLETED
Restaurant installer step: COMPLETED
Runtime generation step: COMPLETED
Matrix26 registration step: COMPLETED
Health check: SKIPPED until the runtime is started
```

The new instance must be protected by default and have runtime status `GENERATED`.

## Start the generated runtime

Phase 3B intentionally does not start operating-system processes. From the repository
root, run:

```bash
bash runtime-clients/matrix26_restaurant_lab/run.sh
```

Open:

```text
http://localhost:8092
```

Login with the administrator and password supplied during execution. Then return to
Matrix26, open the instance detail, and use **Check now**. The instance should become
online.

## Isolation checks

Confirm in Matrix26 that the original instances still point to their original
resources:

```text
Eco Agua del Amazonas: 8081 / eco_agua
Productos de la Selva Belén: 8082 / productos_selva_belen
Restaurante El Buen Sabor: 8084 / restaurante_buen_sabor
```

The provisioning audit must contain:

```text
PROVISIONING_EXECUTION_STARTED
PROVISIONING_EXECUTION_COMPLETED
```

No plaintext administrator password may appear in audit, logs, job tables, runtime
files, or the generated README.

## Failure and retry behavior

When a step fails, the job becomes `FAILED`. Completed steps remain completed. The
same plan can be retried after fixing the cause. During retry, the administrator
credential is applied again using the new password entered in the confirmation form.

If Matrix26 is restarted while a job is `RUNNING`, the recovery runner converts the
interrupted job and active step to `FAILED` so the plan can be reviewed and retried.

## Acceptance criteria

- Matrix26 starts on 8091.
- Existing Phase 3A plans remain readable.
- Only target-compatible modules can produce a READY plan.
- A READY plan requires reference, password confirmation, and acknowledgement.
- The new database contains structure and optional demo data only for the new instance.
- Existing protected databases remain unchanged.
- The runtime folder contains `application.properties`, `run.sh`, `README.txt`, and the Matrix26 ownership marker.
- The generated runtime is not started automatically.
- The new instance is registered and protected by default.
