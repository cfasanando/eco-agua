# Matrix26 Phase 3B diagnostic capacity hotfix report

## Root cause

The provisioning module entity and schema declared `detail` as `VARCHAR(500)`.
A database exception produced a diagnostic longer than 500 characters. Persisting
that diagnostic failed and masked the original core-installation error.

## Fix

- Changed module detail persistence to `TEXT`.
- Added an idempotent startup schema-capacity migration.
- Added defensive output limits for module details and audit summaries.
- Added root-cause preservation to provisioning errors.

## Data impact

Only the Matrix26 control database schema is adjusted:

```sql
ALTER TABLE matrix26_provisioning_module MODIFY COLUMN detail TEXT NULL
```

The migration is executed automatically through Spring JDBC. No manual SQL is
required. Existing business databases are not modified by this hotfix.
