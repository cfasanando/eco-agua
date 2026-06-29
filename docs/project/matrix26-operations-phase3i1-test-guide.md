# Matrix26 Operations Phase 3I.1 - Operations Dashboard

## Goal

Add a consolidated read-only dashboard for Matrix26 operations. The page summarizes runtime health, backups, schedules, restores, lifecycle, decommission, archive records, purge plans, and archive destruction safety without executing any operational action.

## Safety scope

This phase must not:

- start or stop runtimes;
- create backups;
- run restores;
- purge runtime resources;
- destroy archive packages;
- modify protected business instances.

The dashboard only reads existing Matrix26 metadata and the current runtime inventory snapshot.

## Routes

- `/control-center/operations`
- `/control-center/operations/dashboard`

Both render the same consolidated dashboard.

## Expected UI sections

1. Read-only safety banner.
2. KPI grid:
   - Instances;
   - Runtime health;
   - Backups;
   - Backup schedules;
   - Restores;
   - Lifecycle;
   - Final archives;
   - Purge safety;
   - Archive destruction.
3. Attention center with warnings and critical alerts.
4. Instance safety table.
5. Unified operational timeline.
6. Runtime snapshot.

## Validation steps

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

bash scripts/configure-matrix26-operations-phase3i1.sh
bash scripts/check-matrix26-operations-phase3i1.sh

rm -rf target
mvn clean -DskipTests package
```

Expected result:

```text
Matrix26 Operations Phase 3I.1 static checks passed.
BUILD SUCCESS
```

Then start Matrix26:

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/operations/dashboard
```

## Acceptance criteria

- The sidebar shows **Operations dashboard**.
- The dashboard opens without creating or changing any lifecycle, purge, restore, or backup record.
- If archive destruction execution is disabled, the dashboard clearly shows it as disabled.
- If archive destruction execution is enabled, the dashboard raises a critical alert.
- Existing Operations pages still work:
  - `/control-center/operations/runtimes`
  - `/control-center/operations/ports`
  - `/control-center/operations/logs`
- Protected instances remain untouched.
- Runtime Control actions remain in their existing detail pages only.

## Commit suggestion

```bash
git commit -m "Add Matrix26 operations dashboard"
```
