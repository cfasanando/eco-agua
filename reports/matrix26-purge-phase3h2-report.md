# Matrix26 Purge Manager - Phase 3H.2

## Scope

Adds controlled operational purge for the allowlisted archived laboratory `matrix26-appearance-lab`.

## Safety boundaries

- Requires a previous `DRY_RUN_READY` plan.
- Requires `PREPARE PURGE EXECUTION matrix26-appearance-lab` before arming purge.
- Requires `PURGE INSTANCE matrix26-appearance-lab` before execution.
- Requires `DROP ARCHIVED DATABASE matrix26_appearance_lab` before the database is dropped.
- Re-verifies final archive metadata before execution.
- Refuses protected instances and archive clone `matrix26-archived-restore-test`.
- Preserves final archive, protected backups, decommission records, archive records, purge records, and clone restore links.

## Resources removed during execution

- `matrix26_appearance_lab`
- `runtime-clients/matrix26_appearance_lab/`
- `runtime-data/matrix26-appearance-lab/`

## Resources preserved

- `C:/Users/PC/Matrix26/backups/`
- final backup package and metadata
- `matrix26_decommission_*`
- `matrix26_archive_*`
- `matrix26_purge_*`
- `matrix26-archived-restore-test` clone runtime and database
