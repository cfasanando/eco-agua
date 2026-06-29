# Matrix26 Purge Manager - Phase 3H.4 Test Guide

## Scope

Phase 3H.4 adds the controlled Historical Archive Destruction Executor. It can destroy the physical final archive package and final backup directory only after the 3H.3 planner is `READY_FOR_REVIEW`, the execution gate is enabled, and three explicit confirmations are provided.

Central Matrix26 metadata is preserved:

- `matrix26_archive_record`
- `matrix26_archive_event`
- `matrix26_backup_*`
- `matrix26_purge_*`
- `matrix26_decommission_*`
- `matrix26_archive_restore_link`

## Protected instances

Execution remains restricted to:

- `matrix26-appearance-lab`

These remain protected:

- `eco-agua`
- `productos-selva-belen`
- `restaurante-buen-sabor`
- `matrix26-control-center`
- `matrix26-archived-restore-test`

## Apply configuration

Run:

```bash
bash scripts/configure-matrix26-purge-phase3h4.sh
```

Expected default gate:

```properties
matrix26.control-center.purge.archive-destruction-execution-enabled=false
```

This is intentional. First verify that approval is blocked while the gate is disabled.

## Static verification

Run:

```bash
bash scripts/check-matrix26-purge-phase3h4.sh
```

Expected result:

```text
Matrix26 Purge Manager Phase 3H.4 static checks passed.
```

## Functional verification - disabled gate

1. Start Matrix26 Control Center.
2. Open a `READY_FOR_REVIEW` archive destruction plan:

```text
http://localhost:8091/control-center/purge/archive-destruction
```

3. Try to approve with:

```text
APPROVE ARCHIVE DESTRUCTION matrix26-appearance-lab
```

Expected result:

```text
Archive destruction execution is disabled.
```

No files are removed.

## Functional verification - controlled execution window

Only after the disabled-gate test succeeds, temporarily set:

```properties
matrix26.control-center.purge.archive-destruction-execution-enabled=true
```

Restart Matrix26 Control Center and open the same `READY_FOR_REVIEW` plan.

### Step 1: approve

Type exactly:

```text
APPROVE ARCHIVE DESTRUCTION matrix26-appearance-lab
```

Expected status:

```text
APPROVED_FOR_DESTRUCTION
```

No files are removed yet.

### Step 2: execute

Type exactly:

```text
DESTROY ARCHIVE PACKAGE matrix26-appearance-lab
```

and:

```text
I UNDERSTAND THIS ARCHIVE CANNOT BE RESTORED
```

Expected status:

```text
DESTROYED
```

The final archive record should be marked:

```text
PACKAGE_DESTROYED
```

## Safety gates

Execution must be blocked when any condition is true:

- Plan is not `READY_FOR_REVIEW` or `APPROVED_FOR_DESTRUCTION`.
- Retention has not expired.
- Clone restore links still reference the archive.
- Active backup or restore operations exist.
- Final backup metadata is not encrypted, `FINAL`, protected and `VERIFIED`.
- Path is outside `matrix26.control-center.purge.backup-root-directory`.
- Path does not include the instance code.
- Archive directory contains unexpected file names.

## Must not happen

Phase 3H.4 must not:

- Drop any database.
- Delete runtime directories.
- Delete runtime-data directories.
- Delete Matrix26 central metadata.
- Delete clone instance `matrix26-archived-restore-test`.
- Delete protected production clients.

## Restore the safe default

After the test, set execution back to false:

```properties
matrix26.control-center.purge.archive-destruction-execution-enabled=false
```

Restart Matrix26 Control Center.
