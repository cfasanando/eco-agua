# Matrix26 Operations — Phase 3I.2 Test Guide

## Scope

Phase 3I.2 adds a persistent Operation Alert Center for Matrix26 Control Center.

The implementation is intentionally non-destructive. It does not start runtimes, stop runtimes, run backups, run restores, purge resources, destroy archive packages or modify protected instances.

## New routes

- `/control-center/operations/alerts`
- `/control-center/operations/alerts/{id}`
- `POST /control-center/operations/alerts/{id}/acknowledge`
- `POST /control-center/operations/alerts/{id}/resolve`
- `POST /control-center/operations/alerts/{id}/ignore`
- `POST /control-center/operations/alerts/{id}/reopen`

## New tables

- `matrix26_operation_alert`
- `matrix26_operation_alert_event`

The tables are created automatically when Matrix26 Control Center starts.

## Expected behavior

1. Open `/control-center/operations/alerts`.
2. The page synchronizes current dashboard alerts into persistent alert records.
3. Open alerts can be acknowledged, resolved or ignored.
4. Closed alerts can be reopened.
5. Alert events are stored for audit.
6. The dashboard includes a link to the Alert Center.
7. The sidebar includes an Alert Center entry.

## Safety checks

Run:

```bash
bash scripts/check-matrix26-operations-phase3i2.sh
```

Expected output:

```text
Matrix26 Operations Phase 3I.2 static checks passed.
```

## Functional acceptance

- `/control-center/operations/alerts` loads without errors.
- First sync creates alert records from current dashboard alerts.
- Refresh sync does not create duplicate alerts for the same signal.
- Acknowledging an alert changes status to `ACKNOWLEDGED`.
- Resolving an alert changes status to `RESOLVED`.
- Ignoring an alert changes status to `IGNORED`.
- Reopening a closed alert changes status to `OPEN`.
- The detail page shows event history.
- No destructive settings are enabled.

## Protected resources

The following must remain untouched:

- `runtime-data/`
- `runtime-clients/matrix26_appearance_lab/`
- `runtime-clients/matrix26_archived_restore_test/`
- `C:/Users/PC/Matrix26/backups/`
- protected instances: Eco Agua, Productos Selva Belén, Restaurante El Buen Sabor and Matrix26 Control Center.
