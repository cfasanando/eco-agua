# Matrix26 Operations Phase 3I.4 - Final Acceptance Matrix Test Guide

## Scope

Phase 3I.4 adds a read-only final acceptance matrix for Matrix26 Operations & Lifecycle.

Route:

- `/control-center/operations/acceptance`

The page consolidates evidence from existing Matrix26 modules:

- Operations Dashboard
- Alert Center
- Runtime inventory
- Backup and schedule visibility
- Restore and final archive visibility
- Purge and archive destruction safety
- Roles and permissions
- Protected instance and release hygiene checks

## Safety rules

This phase must not:

- start, stop or restart runtimes;
- create backups;
- run restores;
- suspend/reactivate/decommission instances;
- execute purge;
- destroy archive packages;
- change roles or permissions.

The acceptance route is GET-only.

## Configuration

Run:

```bash
bash scripts/configure-matrix26-operations-phase3i4.sh
```

Expected safe configuration:

```properties
matrix26.control-center.enabled=true
matrix26.control-center.purge.archive-destruction-execution-enabled=false
```

## Static verification

Run:

```bash
bash scripts/check-matrix26-operations-phase3i4.sh
```

Expected output:

```text
Matrix26 Operations Phase 3I.4 static checks passed.
```

## Compile

Run:

```bash
mvn clean -DskipTests package
```

Expected result:

```text
BUILD SUCCESS
```

## Manual test

Start Matrix26 Control Center:

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/operations/acceptance
```

Expected result:

- Page loads without errors.
- Overall status is shown as Passed, Warning, Failed or Not tested.
- Metric cards appear.
- Checklist groups are visible.
- Known risks are visible.
- Links navigate to Dashboard, Alert Center, Security, Archive Destruction and Final Archives.
- Refresh button works.

Also open:

```text
http://localhost:8091/control-center/operations/dashboard
```

Expected result:

- The dashboard still loads.
- An Acceptance Matrix button is available.

## Acceptance criteria

The phase is accepted when:

- the page loads;
- the static checker passes;
- Maven compile succeeds;
- no destructive operation is introduced;
- archive destruction execution is disabled after testing;
- protected instances remain visible and documented;
- roles and permissions are visible from `/control-center/security`.
