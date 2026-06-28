# Matrix26 Lifecycle Manager Phase 3G.3 Report

## Summary

Implemented final archive inventory and historical recovery as an isolated clone for decommissioned Matrix26 laboratories.

## Added

- Archive Manager controller, service, repository, properties, initializer, records, events, and restore links.
- Archive inventory pages.
- Archive detail page with final archive reverification.
- Archive restore history page.
- Restore-as-clone integration using the existing Restore Manager pipeline.
- Clone target for archive recovery: `matrix26-archived-restore-test` on port `8096`.
- Configuration and static checker scripts.

## Changed

- Restore Manager can now receive an explicit clone target for archive recovery while preserving the existing 3F.1 clone target.
- Decommission repository schedule snapshot SQL was corrected to avoid a duplicated `WHERE instance_id = ?` clause.
- Sidebar includes Final archives and Archive restores.

## Safety

The implementation remains non-destructive. The archive code contains no database drops, runtime deletion, runtime-data deletion, backup deletion, or purge operation.

## Verification

`bash scripts/check-matrix26-lifecycle-phase3g3.sh` passed in the packaged context.

Maven compilation must be confirmed in the user's local environment because this sandbox cannot download Maven wrapper dependencies.
