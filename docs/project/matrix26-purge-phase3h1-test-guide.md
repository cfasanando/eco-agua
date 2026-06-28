# Matrix26 Purge Manager - Phase 3H.1 Test Guide

## Scope

Phase 3H.1 is a dry-run-only purge planner for archived, decommissioned laboratories. It does not remove databases, runtime directories, runtime-data directories, backup folders, schedules, metadata, modules, or appearance assets.

## Allowed instance

- `matrix26-appearance-lab`

## Protected instances

- Eco Agua / 8081
- Productos de la Selva Belén / 8082
- Restaurante El Buen Sabor / 8084
- Matrix26 Control Center / 8091

## Expected flow

1. Open `/control-center/purge`.
2. Confirm that the decommissioned archive appears as an eligible candidate.
3. Open `/control-center/purge/new`.
4. Select the final archive for `matrix26-appearance-lab`.
5. Enter a reason.
6. Type `PREPARE PURGE DRY RUN matrix26-appearance-lab`.
7. Submit the dry run.
8. Open the plan detail.
9. Confirm that `Deleted resources: 0` is displayed.
10. Review the resource classification table.
11. Download the report.

## Expected classifications

- Database: `WOULD_DELETE` when the archived schema exists.
- Runtime directory: `WOULD_DELETE` when the archived runtime directory exists.
- Runtime-data directory: `WOULD_DELETE` when archived runtime data exists.
- Final backup: `PROTECTED` when it is encrypted, FINAL, deletion-protected and VERIFIED.
- Backup root and metadata: `WOULD_KEEP`.
- Decommission and archive records: `WOULD_KEEP`.
- Archive clone links: `REQUIRES_REVIEW` when clone restores exist.

## Expected result

- Plan status is `DRY_RUN_READY` when no blockers exist.
- Plan status is `BLOCKED` when a safety gate fails.
- No resource is deleted in either case.
