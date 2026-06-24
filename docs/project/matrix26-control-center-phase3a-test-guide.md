# Matrix26 Control Center Phase 3A test guide

## Objective

Validate the visual provisioning assistant in Dry Run mode. This phase must only
write plans, steps and module snapshots to `matrix26_platform_control`.

It must not:

- create or alter an operational database;
- create a runtime profile directory;
- create a user in another database;
- start or stop an application;
- install a module in an operational portal.

## Build and run

```bash
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/provisioning
```

## Valid Restaurant Dry Run

Create a plan with:

```text
Business name: Laboratorio Modular Restaurante
Instance code: laboratorio-restaurante
Database: laboratorio_restaurante
Runtime: laboratorio_restaurante
Port: 8092
URL: http://localhost:8092
Administrator: admin_demo
Module: Restaurante
```

Expected result:

- status `READY`;
- core module marked as built-in;
- Restaurant installer version displayed;
- all generated steps marked ready;
- no execution button;
- audit event `PROVISIONING_DRY_RUN_CREATED`.

## Conflict validation

Create another plan using one of the protected values:

```text
Database: restaurante_buen_sabor
Port: 8084
Runtime: demo_restaurante_buen_sabor
```

Expected result:

- plan saved as `BLOCKED`;
- duplicate reasons displayed;
- dependent steps marked blocked;
- no change to the protected restaurant instance.

## Revalidation

Open a saved plan and press **Revalidar plan**.

Expected result:

- validation timestamp refreshed;
- steps and module snapshots regenerated idempotently;
- audit event `PROVISIONING_DRY_RUN_REVALIDATED`;
- no external operation executed.

## Database verification

The following tables must exist only in `matrix26_platform_control`:

```text
matrix26_provisioning_job
matrix26_provisioning_step
matrix26_provisioning_module
```

The existing operational databases must retain their data unchanged.
