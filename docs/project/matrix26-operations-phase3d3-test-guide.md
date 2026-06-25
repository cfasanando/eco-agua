# Matrix26 Operations — Phase 3D.3 test guide

## Objective

Close the Runtime Control Center with safe recovery of interrupted operations, stale PID detection, verified orphan-process adoption, persistent operation metadata, controlled GZIP log rotation, concurrency protection, and an isolated force-stop path.

The implementation remains limited to the laboratory allowlist:

- `matrix26-restaurant-lab`
- `matrix26-appearance-lab`

The Control Center and the protected operating portals remain read only.

## 1. Apply and compile

From the project root:

```bash
bash scripts/check-matrix26-operations-phase3d3.sh
rm -rf target
mvn clean -DskipTests package
```

Expected results:

```text
Matrix26 Operations Phase 3D.3 static checks passed.
BUILD SUCCESS
```

No SQL script is required. Existing runtime tables use `VARCHAR` status and action columns, so the new values are compatible with the Phase 3D.2 schema.

## 2. Start Matrix26

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/operations
http://localhost:8091/control-center/operations/runtimes
```

Confirm that `8081`, `8082`, `8084`, and `8091` remain read only.

## 3. Regression test: normal runtime operations

Use `matrix26-appearance-lab` on port `8094`.

1. Ensure no external terminal is already running `8094`.
2. Start the runtime from Matrix26.
3. Confirm HTTP availability and the detected PID.
4. Restart it and confirm that the PID changes.
5. Stop it and confirm that port `8094` becomes free.

Expected operation statuses:

```text
START   → COMPLETED
RESTART → COMPLETED
STOP    → COMPLETED
```

Expected local files:

```text
runtime-data/matrix26-appearance-lab/operations/
├── application.log
├── application-error.log
├── last-operation.json
└── runtime.pid     # Only while the managed runtime is active
```

## 4. Interrupted-operation recovery

This test validates recovery after Matrix26 itself stops during an operation.

1. Keep the laboratory stopped.
2. Start `8094` from Matrix26.
3. While the operation is still shown as `STARTING`, stop only Matrix26 with `Ctrl+C`.
4. Do not stop the laboratory process.
5. Start Matrix26 again.
6. Open the runtime detail and operation history.

Matrix26 must inspect the process, command line, port, and HTTP state. If the start really completed, the old operation must become:

```text
RECOVERED
```

If completion cannot be proven, it must become:

```text
INTERRUPTED
```

It must never remain permanently in `REQUESTED` or `RUNNING`.

## 5. Stale PID test

Stop `8094` first. Then create a harmless PID file that references a nonexistent process:

```bash
mkdir -p runtime-data/matrix26-appearance-lab/operations

cat > runtime-data/matrix26-appearance-lab/operations/runtime.pid <<'PID'
pid=999999999
instance=matrix26-appearance-lab
port=8094
startedAt=2026-06-24T00:00:00
PID
```

Refresh the runtime detail.

Expected result:

```text
Stale PID
```

`Start runtime` must remain unavailable until the stale metadata is resolved.

Use **Clean stale PID** and type:

```text
CLEAN PID matrix26-appearance-lab
```

Expected result:

- Only `runtime.pid` is removed.
- No process is terminated.
- No database, configuration, asset, or log is removed.
- Starting the runtime becomes available again.

## 6. Orphan-process adoption

1. Stop `8094` through Matrix26.
2. Stop Matrix26 `8091`.
3. Start the laboratory manually in another terminal:

```bash
bash runtime-clients/matrix26_appearance_lab/run.sh
```

4. Start Matrix26 again.
5. Open the `8094` runtime detail.

Expected result:

```text
Orphan process
```

Matrix26 must not offer normal stop/restart actions until the process is adopted.

Use **Adopt process** and type:

```text
ADOPT matrix26-appearance-lab
```

Expected result:

- The existing process is not restarted.
- Its verified PID is recorded.
- `runtime.pid` is generated.
- Normal stop and restart actions become available.

## 7. Port-conflict protection

With `8094` stopped, run a non-Matrix26 process on the same port. One optional local test is:

```bash
python -m http.server 8094
```

Refresh the inventory.

Expected result:

```text
Port conflict
```

Matrix26 may display the PID and executable, but it must not offer adoption, stop, restart, or force stop for that process.

Stop the temporary server with `Ctrl+C` after the test.

## 8. Manual log rotation

Keep `8094` stopped and add harmless test content:

```bash
mkdir -p runtime-data/matrix26-appearance-lab/operations
printf 'Matrix26 log rotation test\n' >> runtime-data/matrix26-appearance-lab/operations/application.log
```

Refresh the runtime detail and choose **Rotate logs**. Type:

```text
ROTATE LOGS matrix26-appearance-lab
```

Expected files:

```text
application.log             # Empty and ready for the next start
application.log.1.gz        # Contains the previous log
```

Repeating the test must shift archives:

```text
application.log.1.gz
application.log.2.gz
...
application.log.5.gz
```

The default automatic policy is:

- Maximum current file size: 10 MB.
- Retained compressed copies: 5 per log.
- Compression: GZIP.
- Automatic rotation: before start or after stop when the threshold is reached.
- Manual rotation: only while the runtime is stopped.

## 9. Force-stop boundary

Force stop must not be displayed during normal operation. It is enabled only when all conditions are true:

1. The instance belongs to the laboratory allowlist.
2. A graceful stop ended in `STOP_TIMEOUT`.
3. The process is still alive.
4. The Java command line matches the expected runtime profile and `application.properties`.
5. The administrator enters the exact confirmation:

```text
FORCE STOP matrix26-appearance-lab
```

Do not intentionally create a hung production process only to test this action. Static verification is sufficient unless a genuine laboratory timeout occurs.

## 10. Concurrency test

Double-clicking an action or sending two requests must not create two processes. Matrix26 uses:

- An in-memory lock per runtime.
- A persisted active-operation check for `REQUESTED` and `RUNNING`.
- Port availability validation before start.

Expected error for a concurrent request:

```text
Another runtime operation is already in progress.
```

## 11. Acceptance checklist

- [ ] START, STOP, and RESTART still work.
- [ ] Interrupted operations are resolved as RECOVERED or INTERRUPTED.
- [ ] A stale PID blocks start until safely cleaned.
- [ ] An orphan process must be adopted before normal control.
- [ ] Port conflicts never expose process-control actions.
- [ ] Manual and automatic log rotation preserve GZIP archives.
- [ ] Force stop is isolated behind timeout, ownership, allowlist, and confirmation.
- [ ] Runtime metadata is written atomically to `last-operation.json`.
- [ ] Simultaneous operations are blocked.
- [ ] `8081`, `8082`, `8084`, and `8091` remain read only.
- [ ] No operational database or client asset is modified by maintenance actions.

After completing this checklist, Runtime Control Center Phase 3D is considered closed and the project can advance to Phase 3E.1: verified manual database backups.
