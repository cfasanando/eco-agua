# Matrix26 Control Center Phase 3A report

## Delivered scope

Phase 3A adds a provisioning planning workflow inside Matrix26 Control Center:

- dedicated provisioning navigation;
- visual eight-stage assistant;
- duplicate validation for instance code, database, runtime and port;
- Control Center resource protection;
- URL and runtime port consistency validation;
- module installer capability validation;
- auditable Dry Run history;
- generated provisioning steps;
- module installer/version snapshots;
- plan revalidation;
- audit events for creation and revalidation.

## Module readiness

The common core is represented as a built-in mandatory component. The Restaurant
module is detected through `PlatformModuleInstaller` and can produce a ready plan.
Catalog modules without a registered installer remain visible as pending and cannot
be selected for an executable future plan.

## Safety boundary

All Phase 3A writes are restricted to:

```text
matrix26_platform_control
```

No operational database connection is opened by the provisioning service. No
runtime file is generated, no administrator password is collected or stored, and
no installer step is executed.

## New control tables

```text
matrix26_provisioning_job
matrix26_provisioning_step
matrix26_provisioning_module
```

They are created idempotently by `Matrix26ControlCenterInitializer`; no manual SQL
is required.

## Deferred to Phase 3B

- explicit execution confirmation;
- secure target database credentials;
- real base creation;
- common core installation;
- initial administrator credential delivery;
- module installer execution against the target datasource;
- runtime file generation;
- rollback/resume behavior;
- final instance registration and health verification.
