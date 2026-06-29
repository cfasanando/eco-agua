# Matrix26 Purge Manager - Phase 3H.3 Test Guide

## Scope

Phase 3H.3 adds a read-only Historical Archive Destruction Planner. It inventories final archive packages and backup metadata after operational purge, but it does not remove files, databases, backup packages, archive records, purge records, or clone restore links.

## Protected instances

The planner remains restricted to `matrix26-appearance-lab`. These instances remain protected:

- `eco-agua`
- `productos-selva-belen`
- `restaurante-buen-sabor`
- `matrix26-control-center`
- `matrix26-archived-restore-test`

## Configuration

Run:

```bash
bash scripts/configure-matrix26-purge-phase3h3.sh
```

Expected keys:

```properties
matrix26.control-center.purge.enabled=true
matrix26.control-center.purge.archive-destruction-enabled=true
matrix26.control-center.purge.archive-destruction-execution-enabled=false
matrix26.control-center.purge.archive-destruction-require-retention-expired=true
matrix26.control-center.purge.allowed-instance-codes=matrix26-appearance-lab
matrix26.control-center.purge.protected-instance-codes=eco-agua,productos-selva-belen,restaurante-buen-sabor,matrix26-control-center,matrix26-archived-restore-test
```

The `archive-destruction-execution-enabled=false` gate must remain disabled in this phase.

## Static verification

Run:

```bash
bash scripts/check-matrix26-purge-phase3h3.sh
```

Expected result:

```text
Matrix26 Purge Manager Phase 3H.3 static checks passed.
```

## Functional verification

1. Start Matrix26 Control Center.
2. Open:

```text
http://localhost:8091/control-center/purge/archive-destruction
```

3. Create a planner from:

```text
http://localhost:8091/control-center/purge/archive-destruction/new
```

4. Use:

```text
Instance: Matrix26 Appearance Laboratory
Reason: Review historical archive package before any future destruction decision.
Confirmation: PREPARE ARCHIVE DESTRUCTION matrix26-appearance-lab
```

## Expected result

The plan should finish as one of these states:

- `BLOCKED` when retention is active, final backup metadata is incomplete, the package is missing, or clone links still exist.
- `READY_FOR_REVIEW` only when all planner checks pass.

In both cases:

```text
Deleted resources: 0
```

## Expected classifications

Common classifications:

- `FINAL_ARCHIVE_PACKAGE` as `PROTECTED`, `REQUIRES_REVIEW`, `BLOCKED`, or `WOULD_DELETE`.
- `FINAL_BACKUP_DIRECTORY` as `PROTECTED` or `WOULD_DELETE`.
- `PUBLIC_BACKUP_METADATA` as `WOULD_KEEP`.
- `ARCHIVE_RECORDS` as `WOULD_KEEP`.
- `BACKUP_METADATA` as `WOULD_KEEP`.
- `PURGE_RECORDS` as `WOULD_KEEP`.
- `CLONE_DEPENDENCY` as `REQUIRES_REVIEW` when clone restore links exist.

## Must not happen

Phase 3H.3 must not:

- Delete `package.m26backup`.
- Delete `public-manifest.json`.
- Delete `checksums.sha256`.
- Delete `backup-report.txt`.
- Remove backup directories.
- Delete central archive metadata.
- Delete purge metadata.
- Delete clone restore links.
- Drop any database.
