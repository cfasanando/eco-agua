# Matrix26 Control Center Phase 2 report

## Objective

Move instance administration into the dedicated Matrix26 Control Center while preserving strict data isolation between managed businesses.

## Delivered

- Visual instance registration.
- Instance metadata editing.
- Unique validation for code, port, database, and runtime profile.
- Monitoring enable/disable control.
- Manual health check per instance.
- Per-instance health history.
- Administrative protection toggle.
- Visual module declarations per instance.
- Central audit trail with authenticated actor and before/after snapshots.
- Dedicated instance detail screen.
- Updated dashboard, sidebar, settings, and modules screens.

## Safety model

Phase 2 only writes to `matrix26_platform_control`.

The following operations remain unavailable:

- database creation or deletion;
- schema installation;
- operational module installation or removal;
- runtime start, stop, or restart;
- backup and restore;
- reads from business sales, customer, order, product, inventory, restaurant, or accounting tables.

## Important initializer correction

Phase 1 reseeded the three protected instance records on every startup. Phase 2 changes the seed behavior so existing instance metadata and module declarations are preserved. Seed data is inserted only when an instance code does not yet exist.

## Schema

Added idempotently through Spring JDBC:

```text
matrix26_instance_audit_log
```

No manual SQL file is included.

## Validation status

- ZIP context inspected.
- Java source syntax checked with `javac` parsing; external framework symbols were unavailable in the artifact environment.
- Thymeleaf files parsed as HTML.
- Full Maven build must be executed in the project environment because the artifact environment could not download the Maven distribution.
