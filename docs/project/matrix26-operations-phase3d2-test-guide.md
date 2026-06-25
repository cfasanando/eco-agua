# Matrix26 Operations Phase 3D.2 Test Guide

## Objective

Verify that Matrix26 Control Center can start, gracefully stop, and restart only the allowlisted laboratory runtimes while production instances and Matrix26 itself remain read only.

## Safety boundaries

Runtime control is initially allowed only for:

- `matrix26-restaurant-lab` on port `8093`;
- `matrix26-appearance-lab` on port `8094`.

The following remain blocked:

- Matrix26 Control Center on `8091`;
- Eco Agua on `8081`;
- Productos de la Selva Belén on `8082`;
- Restaurante El Buen Sabor on `8084`;
- any instance not explicitly included in the runtime-control allowlist.

Phase 3D.2 does not use arbitrary commands from forms, does not execute `taskkill`, does not call `destroyForcibly()` for managed runtimes, and does not modify operational databases.

## Apply and validate

```bash
bash scripts/check-matrix26-operations-phase3d2.sh
rm -rf target
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/operations/runtimes
```

## Automatic database objects

Matrix26 creates only in `matrix26_platform_control`:

- `matrix26_runtime_state`;
- `matrix26_runtime_operation`.

No manual SQL is required.

## Test 1 — protected runtime boundaries

Review ports `8081`, `8082`, `8084`, and `8091`.

Expected:

- the runtime remains visible in inventory;
- no Start, Stop, or Restart action is available;
- the detail page explains why runtime control is locked.

## Test 2 — start the appearance laboratory

Ensure port `8094` is stopped and free.

Open the detail page for `matrix26-appearance-lab` and press **Start runtime**.

Expected:

- a `START` operation is created;
- `runtime-data/matrix26-appearance-lab/operations/` is created;
- `application.log`, `application-error.log`, and `runtime.pid` are created;
- a Java process starts with a PID;
- Matrix26 waits for the HTTP probe;
- the operation ends in `COMPLETED`;
- the runtime state becomes `ONLINE`;
- `http://localhost:8094` opens normally.

## Test 3 — restart

From the same detail page press **Restart**.

Type exactly:

```text
RESTART matrix26-appearance-lab
```

Expected:

- the previous PID is recorded;
- the previous process stops;
- port `8094` becomes free before the next process starts;
- a new PID is recorded;
- the health check succeeds;
- the operation ends in `COMPLETED`.

The old and new PID must be different.

## Test 4 — graceful stop

Press **Stop** and type exactly:

```text
STOP matrix26-appearance-lab
```

Expected:

- the operation moves through `RUNNING`;
- Matrix26 sends a normal process termination request;
- the process exits;
- port `8094` becomes free;
- `runtime.pid` is removed;
- the final runtime state is `STOPPED`.

Phase 3D.2 must not force-kill a runtime if the stop timeout is reached.

## Test 5 — persistence after Matrix26 restart

1. Start `8094` from Matrix26.
2. Stop only Matrix26 `8091` with `Ctrl+C`.
3. Confirm `8094` remains online.
4. Start Matrix26 again.
5. Refresh the runtime inventory.
6. Stop or restart `8094` from Matrix26.

Expected:

- Matrix26 redetects the process from the operating system and runtime profile;
- the PID and port are associated with the correct runtime;
- the operation succeeds without reopening a terminal for `8094`.

## Test 6 — wrong confirmation

Try a stop or restart with an incorrect confirmation string.

Expected:

- the request is rejected;
- the runtime remains unchanged;
- no unrelated process is affected.

## Test 7 — occupied port protection

With `8094` stopped, occupy port `8094` using an unrelated process and refresh inventory.

Expected:

- the runtime shows `PORT_OCCUPIED` or an equivalent warning;
- Start is not available;
- Matrix26 does not terminate the unrelated process.

## Test 8 — logs and audit

Review:

```text
http://localhost:8091/control-center/operations
http://localhost:8091/control-center/operations/logs
```

Expected:

- runtime operations show actor, action, result, PID, duration, and timestamp;
- operation logs are available in the normal log inventory;
- secrets remain sanitized;
- the general Matrix26 audit contains `RUNTIME_START`, `RUNTIME_STOP`, or `RUNTIME_RESTART` actions.

## Acceptance checklist

- [ ] Static checker passes.
- [ ] Maven build succeeds.
- [ ] Only 8093 and 8094 are controllable.
- [ ] Start creates PID and persistent logs.
- [ ] Restart changes PID.
- [ ] Stop releases the port.
- [ ] Wrong confirmation is rejected.
- [ ] Occupied unrelated port is protected.
- [ ] Runtime remains alive when Matrix26 is stopped.
- [ ] Matrix26 can rediscover and control the runtime after restart.
- [ ] No force stop is executed.
- [ ] Operations and audit history are recorded.
