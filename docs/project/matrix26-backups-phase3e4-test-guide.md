# Matrix26 Backups Phase 3E.4 Test Guide

## Scope

Phase 3E.4 closes the Matrix26 Backup Manager with automatic encrypted schedules, finite retries, missed-run recovery, calendar visibility, execution history, and operational alerts.

The runtime boundary remains restricted to `matrix26-appearance-lab`.

## Safety boundaries

- Scheduled backups are always full instance backups.
- AES-256-GCM encryption is mandatory.
- The master key remains in `MATRIX26_BACKUP_MASTER_KEY`.
- No command text can be supplied by the browser.
- No external cron or Windows Task Scheduler task is created.
- Only one backup operation can be active per instance.
- Duplicate schedule windows are rejected by a database unique key.
- Protected production instances remain unavailable through the backup allowlist.
- No database, runtime, upload, or active appearance resource is deleted.

## Apply

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

unzip "$HOME/Downloads/matrix26-backups-phase3e4-final.zip"   -d "$HOME/Downloads"

SRC="$HOME/Downloads/matrix26-backups-phase3e4-final"

cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/

chmod +x scripts/check-matrix26-backups-phase3e4.sh
chmod +x scripts/configure-matrix26-backup-scheduler-phase3e4.sh
```

## Runtime configuration

```bash
bash scripts/configure-matrix26-backup-scheduler-phase3e4.sh
```

The script preserves the previous runtime configuration under:

```text
runtime-data/matrix26-control/maintenance/application.properties.phase3e4.bak
```

Expected properties:

```properties
matrix26.control-center.backups.scheduling-enabled=true
matrix26.control-center.backups.scheduler-poll-milliseconds=60000
matrix26.control-center.backups.scheduler-initial-delay-milliseconds=15000
matrix26.control-center.backups.scheduler-grace-minutes=2
matrix26.control-center.backups.schedule-timezone=America/Lima
```

## Static verification

```bash
bash scripts/check-matrix26-backups-phase3e4.sh
```

Expected final line:

```text
Matrix26 Backups Phase 3E.4 static checks passed.
```

## Build

Stop Matrix26 and laboratory runtimes before compiling.

```bash
rm -rf target
mvn clean -DskipTests package
```

Expected result:

```text
BUILD SUCCESS
```

## Start

Open a new Git Bash session so the persisted user-level master key is available.

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/backups/schedules
```

The initializer creates only in `matrix26_platform_control`:

```text
matrix26_backup_schedule
matrix26_backup_schedule_execution
matrix26_backup_alert
```

## First schedule test

Create a schedule with:

```text
Instance: Matrix26 Appearance Laboratory
Name: Five-minute encrypted test
Frequency: Daily
Hour: current America/Lima hour
Minute: five minutes after the current time
Timezone: America/Lima
Encryption: mandatory
Retention class: Daily
Maximum attempts: 2
Retry delay: 2 minutes
Missed execution: Run latest missed backup
Enabled: yes
```

Expected sequence:

```text
SCHEDULED
QUEUED
RUNNING
COMPLETED
```

Expected backup:

```text
Backup type: SCHEDULED_FULL
Encrypted package: package.m26backup
Verification: VERIFIED
Retention class: DAILY
```

## Calendar and execution history

Open:

```text
http://localhost:8091/control-center/backups/calendar
http://localhost:8091/control-center/backups/executions
```

Confirm:

- The planned schedule is visible before execution.
- The completed execution replaces the planned window.
- The execution links to the encrypted backup.
- No duplicate execution exists for the same schedule and planned timestamp.

## Manual schedule run

Open the schedule detail and type exactly:

```text
RUN BACKUP matrix26-appearance-lab
```

The run must use the same encryption, lock, retry, and audit boundaries.

## Missed execution recovery

1. Configure a schedule for two minutes in the future.
2. Stop Matrix26 before the planned time.
3. Keep Matrix26 stopped for at least five minutes.
4. Start Matrix26.

Expected:

- The latest missed window runs once because the policy is `RUN_ON_STARTUP`.
- Older missed windows are recorded as `MISSED`.
- Matrix26 does not create a backlog of multiple backups.

## Retry and alert test

Temporarily close Matrix26, then open a PowerShell session and preserve the current user key before testing. Do not replace the stored key.

Start Matrix26 in a shell where the key is deliberately unavailable only for the test. The schedule execution should become:

```text
RETRY_WAITING
```

An alert should show:

```text
MASTER_KEY_UNAVAILABLE
```

Restore the normal environment and restart Matrix26. The next retry should complete and the recoverable alert should be resolved.

Do not generate a new master key.

## Operation conflict test

Start a manual full backup and trigger a scheduled run for the same instance while it is active.

Expected:

```text
SKIPPED
Another backup operation is already active for this instance.
```

No duplicate database dump process should start.

## Protected-instance test

The schedule form must expose only allowed candidates. It must not allow schedules for:

```text
eco_agua
productos_selva_belen
restaurante_buen_sabor
matrix26_platform_control
```

## Alerts

Open:

```text
http://localhost:8091/control-center/backups/alerts
```

Validate warnings for:

- Missing master key.
- Missing dump tool.
- Unavailable backup storage.
- Low disk space.
- No successful backup during the last 24 hours.
- Failed scheduled backup.
- Missed execution.

## Acceptance checklist

- [ ] Static checks pass.
- [ ] Maven build succeeds.
- [ ] Scheduler starts after the configured initial delay.
- [ ] Daily schedule executes at the expected America/Lima time.
- [ ] Full backup is encrypted and verified.
- [ ] Calendar shows planned and completed events.
- [ ] No duplicate execution is created.
- [ ] Retry count is finite.
- [ ] Missed-run recovery executes only the latest window.
- [ ] Alerts are visible and resolvable.
- [ ] Manual and scheduled backups cannot overlap.
- [ ] Protected instances remain unavailable.
- [ ] Existing retention and manual backup flows still work.
- [ ] No secret is written to source, runtime properties, database, or logs.

## Suggested commit

```bash
git commit -m "Add Matrix26 scheduled backup automation"
```
