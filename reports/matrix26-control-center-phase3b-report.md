# Matrix26 Control Center Phase 3B Report

## Delivered scope

Phase 3B converts the Phase 3A provisioning Dry Run into an explicit, resumable
execution workflow inside Matrix26 Control Center.

Delivered components:

- execution confirmation form with reference matching and acknowledgement;
- transient initial administrator password handling;
- persistent job and step execution metadata;
- idempotent control-schema column upgrades;
- creation of an isolated target database through Spring JDBC;
- structural core installation from a configurable read-only template database;
- target-aware module installer contract;
- Restaurant module target installation;
- generated runtime configuration and run script;
- central instance registration with protection enabled by default;
- health-check deferral until the generated runtime is started;
- audit events for started, completed, failed, and interrupted executions;
- startup recovery for jobs left in `RUNNING` state;
- retry behavior that skips completed steps and reapplies the administrator credential.

## Safety model

The provisioning target is validated against registered instance codes, database
names, runtime profiles, runtime ports, Matrix26 reserved resources, and existing
runtime folders.

A database that already contains tables is rejected when it is not part of the same
provisioning execution. Runtime folders contain a plan ownership marker to prevent
an unrelated plan from overwriting existing files.

The template database is queried with `SHOW CREATE TABLE`; no operational rows are
copied. Tables with the `matrix26_` prefix are excluded from the new business
instance. Module-specific Restaurant tables are created by `RestaurantModuleInstaller`
instead of being copied from the template.

The initial password is accepted only in the execution request, encoded with the
application password encoder, and written directly to the target database. It is not
stored in Matrix26 provisioning or audit tables.

## Runtime lifecycle boundary

Phase 3B generates the runtime but does not start or stop operating-system processes.
Runtime lifecycle controls remain a later Matrix26 phase. The administrator starts the
generated `run.sh`, then Matrix26 health monitoring confirms availability.

## Database impact

Matrix26 control schema gains execution metadata columns on:

```text
matrix26_provisioning_job
matrix26_provisioning_step
```

A confirmed plan creates only its declared new database. Existing protected business
databases are not altered.

## Validation performed in the package workspace

- Java delimiter and text-block balance checks passed for all modified Java files.
- `Matrix26TargetDatabaseService` compiled against isolated API stubs.
- `Matrix26ProvisioningExecutionService` compiled against isolated API stubs after fixing lambda finality.
- `Matrix26ProvisioningRecoveryRunner` compiled against isolated API stubs.
- `Matrix26ControlCenterController` compiled against isolated MVC and project API stubs.
- `RestaurantModuleInstaller` compiled against isolated Spring JDBC and project API stubs.
- Updated Thymeleaf templates passed HTML parsing checks.
- Maven dependency resolution was not available in the packaging environment; the full `mvn clean -DskipTests package` validation must run in the project environment.
