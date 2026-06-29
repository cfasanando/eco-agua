# Matrix26 Feature Flags - Phase 4A Test Guide

## Scope

Phase 4A adds a central module activation page for Matrix26 instances.

Route:

```text
/control-center/modules/activation
```

This phase is metadata-only. It updates module declarations stored in Matrix26 control metadata and writes audit events to:

```text
matrix26_instance_module_activation_event
```

It does not install code, execute SQL in client databases, restart runtimes, hide menus, or change operational data.

## Pre-checks

Run:

```bash
bash scripts/check-matrix26-feature-flags-phase4a.sh
bash scripts/configure-matrix26-feature-flags-phase4a.sh
mvn clean -DskipTests package
```

Expected static check:

```text
Matrix26 Feature Flags Phase 4A static checks passed.
```

## Functional test

1. Start the control runtime:

```bash
bash scripts/run-matrix26-control.sh
```

2. Open:

```text
http://localhost:8091/control-center/modules/activation
```

3. Confirm the page shows:

- total instances;
- protected instances;
- catalog modules;
- enabled declarations;
- module groups;
- activation forms per instance;
- recent activation events.

4. Pick a lab/non-production instance and toggle one harmless declaration.

5. Save and confirm:

- the page reloads successfully;
- the module selection remains saved;
- a new activation event appears in the recent event list;
- the instance detail page still loads;
- the runtime was not restarted;
- no operational client database was modified.

## Safety expectations

The following must remain true:

- `/control-center/modules` still works.
- `/control-center/instances/{id}` still works.
- Protected instances remain protected.
- Matrix26 archive destruction execution remains disabled unless explicitly enabled.
- No `DROP DATABASE`, file deletion or runtime process execution was added in Phase 4A code.

## Next phase

Phase 4B should apply these declarations to runtime navigation and/or generated runtime properties. Phase 4A intentionally stops at visual control and persistence.
