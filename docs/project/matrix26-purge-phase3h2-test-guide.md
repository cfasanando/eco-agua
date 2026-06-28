# Matrix26 Purge Manager Phase 3H.2 Test Guide

## Preconditions

- `matrix26-appearance-lab` is `DECOMMISSIONED`.
- A final archive is `READY`.
- A purge dry run is `DRY_RUN_READY`.
- Port `8094` is free.
- Clone `matrix26-archived-restore-test` on `8096`, if present, remains outside the purge scope.

## Test flow

1. Open `/control-center/purge/{id}` for a `DRY_RUN_READY` plan.
2. Type `PREPARE PURGE EXECUTION matrix26-appearance-lab`.
3. Confirm the plan moves to `READY_TO_PURGE` and no files or schemas were deleted.
4. Type `PURGE INSTANCE matrix26-appearance-lab`.
5. Type `DROP ARCHIVED DATABASE matrix26_appearance_lab`.
6. Execute the operational purge.
7. Confirm the plan becomes `PURGED`.
8. Confirm `matrix26_appearance_lab` no longer exists.
9. Confirm `runtime-clients/matrix26_appearance_lab/` no longer exists.
10. Confirm `runtime-data/matrix26-appearance-lab/` no longer exists.
11. Confirm final archive and backups still exist.
12. Confirm `matrix26-archived-restore-test` still works on `8096`.

## Expected result

- Original archived resources are removed.
- Final archive and audit records are preserved.
- Protected instances and archive clones are untouched.
