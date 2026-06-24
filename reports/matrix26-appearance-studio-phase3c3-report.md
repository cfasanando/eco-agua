# Matrix26 Appearance Studio — Phase 3C.3 implementation report

## Delivered

- Real publication of an appearance draft to an authorized managed instance.
- Explicit instance-code confirmation and acknowledgement.
- Local appearance configuration stored in the target database.
- Local runtime configuration service with a short refresh cache.
- `ThemeResolver` and `LayoutResolver`.
- Runtime model attributes for public, administrative and login views.
- Dynamic theme/layout CSS loading.
- Safe CSS variable overrides.
- Real Classic, Nature and Warm runtime styling.
- Real public layout behavior.
- Classic and Compact Workspace administrative layouts.
- Split login layout.
- Central/local synchronization indicator.
- Published-version history.
- Rollback as a new immutable published version.
- Audit actions:
  - `APPEARANCE_PUBLISHED`
  - `APPEARANCE_ROLLED_BACK`

## Isolation

The target runtime reads only its own local table. It does not call Matrix26 during
normal page rendering. Matrix26 may therefore be offline without affecting the
published portal appearance.

## Protected resources

The publisher enforces both an explicit allowlist and reserved database/port
checks. During Phase 3C.3, only `matrix26-restaurant-lab` can receive real
appearance publications.

## Database impact

Matrix26 control database:

- existing appearance and audit records are updated or appended;
- no new central table is required.

Authorized target database:

```sql
matrix26_instance_appearance_config
```

The table is created idempotently during the first publication. No manual SQL is
included in source resources.

## Compatibility

When a business instance does not contain the local configuration table, the
new runtime appearance bridge stays disabled. Existing protected instances keep
their previous CSS and layout behavior.
