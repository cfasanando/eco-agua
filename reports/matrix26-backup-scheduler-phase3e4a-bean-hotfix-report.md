# Matrix26 Backup Scheduler Phase 3E.4a Bean Hotfix

## Problem

`Matrix26BackupScheduler` was created when `matrix26.control-center.backups.scheduling-enabled` was missing because `matchIfMissing=true`, while `Matrix26BackupScheduleService` required `matrix26.control-center.enabled=true`. This could produce an application startup failure due to a missing service bean.

## Fix

- Restored the known-good `Matrix26BackupScheduleService` source with `@Service`.
- Required both `matrix26.control-center.enabled=true` and `matrix26.control-center.backups.scheduling-enabled=true` before creating the scheduler or enabling scheduling.
- Removed the unsafe `matchIfMissing=true` behavior.

No database, backup artifact, runtime data, or credential is modified.
