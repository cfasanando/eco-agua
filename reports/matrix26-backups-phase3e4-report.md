# Matrix26 Backups Phase 3E.4 Report

## Delivered

Phase 3E.4 adds an internal Matrix26 scheduler for encrypted recovery packages.

### Scheduling

- Daily, weekly, and monthly schedules.
- IANA timezone support with `America/Lima` as the default.
- Persistent `next_run_at` calculation.
- One-minute evaluation loop.
- Unique schedule-window protection.
- Manual run through the same schedule pipeline.

### Execution lifecycle

- `SCHEDULED`
- `QUEUED`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `RETRY_WAITING`
- `MISSED`
- `SKIPPED`
- `CANCELLED`

### Reliability

- Finite retry attempts and configurable delay.
- Startup recovery for missed windows.
- Latest-missed-only anti-backlog rule.
- Persistent execution records.
- Per-instance active-operation lock.
- Existing backup job lock remains authoritative.

### Security

- Scheduled backups are full-instance packages.
- AES-256-GCM encryption is mandatory.
- `MATRIX26_BACKUP_MASTER_KEY` remains outside the database and runtime properties.
- Scheduled backup support was added to the existing encrypted package service.
- Browser requests cannot supply commands, executable paths, database names, or filesystem paths.
- Candidate validation delegates to the established backup allowlist.
- Existing protected instances remain excluded.

### Monitoring

- Schedule dashboard.
- Schedule detail and execution history.
- 31/90-day calendar view.
- Operational alerts.
- Health checks for key, dump tool, storage, disk space, and recent successful backup.
- Manual alert resolution.

### Persistence

The initializer creates only in `matrix26_platform_control`:

- `matrix26_backup_schedule`
- `matrix26_backup_schedule_execution`
- `matrix26_backup_alert`

No manual SQL is required.

## Validation performed

- Java 17 isolated compilation of all new schedule classes.
- Isolated compilation of modified backup and encryption services through dependency stubs.
- Executable tests for daily, weekly, and monthly next-run calculation.
- HTML parser validation for all backup templates.
- CSS brace validation.
- Static enforcement of mandatory encryption.
- Static verification of the duplicate-window unique key.
- Static verification of no arbitrary process execution.
- Static verification of protected-instance boundary delegation.
- Package application test against a clean context copy.

## Important implementation note

Generated database keys use Spring `GeneratedKeyHolder`. The scheduler does not rely on `SELECT LAST_INSERT_ID()` across pooled connections.

## Not included

- External cron or Windows Task Scheduler integration.
- Email or SMS notification delivery.
- Multi-node scheduler coordination.
- Automatic retention cleanup.
- Restore execution.
- Production-instance scheduling.

These remain outside Phase 3E.4.
