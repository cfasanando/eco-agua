# Matrix26 Lifecycle Manager Phase 3G.3 Test Guide

## Scope

Phase 3G.3 registers protected final archives for decommissioned laboratories and allows recovery only as a new isolated clone. It does not reactivate the original instance and does not delete any database, runtime, runtime-data directory, module assignment, appearance record, or backup.

## Laboratory target

- Original instance: `matrix26-appearance-lab`
- Expected state before test: `DECOMMISSIONED`
- Final backup: encrypted, verified, protected, retention class `FINAL`
- Clone target: `matrix26-archived-restore-test`
- Clone database: `matrix26_archived_restore_test`
- Clone runtime: `matrix26_archived_restore_test`
- Clone port: `8096`

## Functional checks

1. Open `/control-center/lifecycle/archive`.
2. Refresh the archive inventory.
3. Open the archive record for `matrix26-appearance-lab`.
4. Verify the final archive again.
5. Confirm the archive status is `READY`.
6. Restore as clone with confirmation `RESTORE ARCHIVE matrix26-archived-restore-test`.
7. Confirm the restore job completes.
8. Open `http://localhost:8096`.
9. Confirm the original `matrix26-appearance-lab` remains `DECOMMISSIONED`.
10. Confirm port `8094` remains stopped unless a later phase explicitly permits a separate operation.

## Safety boundaries

Phase 3G.3 must not contain or trigger:

- `DROP DATABASE`
- runtime deletion
- runtime-data deletion
- final archive deletion
- purge
- direct reactivation of the original decommissioned instance

