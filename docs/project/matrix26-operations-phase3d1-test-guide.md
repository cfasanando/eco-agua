# Matrix26 Operations Phase 3D.1 Test Guide

## Scope

Phase 3D.1 adds a read-only operations inventory to Matrix26 Control Center.
It detects runtime folders, configuration files, launchers, Java processes,
expected listening ports, local HTTP availability, log files, and storage usage.

This phase does **not** start, stop, restart, delete, or modify runtime processes.
It also does not modify operational databases or write PID files.

## Prerequisites

- Matrix26 Control Center is available on port `8091`.
- The project builds successfully with Java 17.
- At least one laboratory runtime exists under `runtime-clients/`.
- Runtime processes may be online or offline; both states are valid test cases.

## Build

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

bash scripts/check-matrix26-operations-phase3d1.sh

rm -rf target
mvn clean -DskipTests package
```

Expected result:

```text
BUILD SUCCESS
Matrix26 Operations Phase 3D.1 static checks passed.
```

## Start Matrix26

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/operations
```

## Routes

```text
/control-center/operations
/control-center/operations/runtimes
/control-center/operations/runtimes/{runtimeKey}
/control-center/operations/ports
/control-center/operations/logs
```

## Test 1: Control Center inventory

Open:

```text
http://localhost:8091/control-center/operations/runtimes/control
```

Expected:

- Runtime profile: `matrix26_control`.
- Expected port: `8091`.
- Runtime folder: `runtime-clients/matrix26_control`.
- `application.properties` detected.
- Launcher detected.
- Local HTTP probe returns an online result.
- A Java PID is displayed when the operating system exposes it.
- No start, stop, or restart controls are available.

## Test 2: Online laboratory runtime

Start the appearance laboratory in a second terminal:

```bash
bash runtime-clients/matrix26_appearance_lab/run.sh
```

Refresh:

```text
http://localhost:8091/control-center/operations/runtimes?refresh=true
```

Expected for `matrix26-appearance-lab`:

- Status: `Online`.
- Port `8094` is listening.
- HTTP response is detected.
- Runtime folder and configuration are detected.
- Database, runtime profile, and port are consistent.
- PID and uptime are shown when available.
- Runtime and asset storage are shown separately.

## Test 3: Offline laboratory runtime

Stop only the laboratory process with `Ctrl+C`.
Do not stop Matrix26.

Refresh the runtime inventory.

Expected:

- Status changes to `Offline`.
- HTTP probe reports no response.
- Port `8094` is no longer listening.
- Runtime folder and configuration remain detected.
- No destructive action is offered.

## Test 4: Protected instances

Review ports `8081`, `8082`, and `8084`.

Expected:

- Each protected instance displays a `Protected` badge.
- Matrix26 only observes the process and port.
- No runtime action is available.
- Existing operational data is not queried.

## Test 5: Configuration mismatch

Use only a disposable laboratory runtime.
Temporarily change the copied test runtime configuration so that the registered
port differs from `server.port`.

Refresh the inventory.

Expected:

- Configuration status changes to `Review`.
- A warning explains the registered and configured port mismatch.
- Matrix26 does not rewrite the configuration.

Restore the original file immediately after the test.

## Test 6: Port ownership

If another process occupies a laboratory port while the portal does not respond,
refresh the Ports page:

```text
http://localhost:8091/control-center/operations/ports?refresh=true
```

Expected:

- Port ownership is displayed when Windows exposes the PID.
- A mismatched owner is reported as `Port occupied`.
- Matrix26 does not terminate the process.

## Test 7: Logs

Open:

```text
http://localhost:8091/control-center/operations/logs
```

Expected:

- Only `.log`, `.out`, and `.err` files inside approved project directories are listed.
- Arbitrary filesystem paths are not accepted.
- The runtime detail shows a sanitized tail when a related log exists.
- Values matching password, secret, token, API key, or private key assignments are masked.

A runtime started directly in a terminal may show `No log` because its standard
output is not redirected to a file. That is valid in Phase 3D.1.

## Test 8: Cache and refresh

Open the Operations dashboard repeatedly without `refresh=true`.
Then use the `Refresh inventory` button.

Expected:

- Normal navigation reuses the short inventory cache.
- Explicit refresh captures a new process and port snapshot.
- No runtime process is restarted.

## Acceptance checklist

- [ ] Maven build succeeds.
- [ ] Operations section appears in the Matrix26 sidebar.
- [ ] Matrix26 Control Center is included in the inventory.
- [ ] Registered instances are listed.
- [ ] Process, PID, port, HTTP, configuration, storage, and log states are visible.
- [ ] Sensitive log and command values are masked.
- [ ] Protected instances remain read-only.
- [ ] No POST runtime operation exists.
- [ ] No PID file is written.
- [ ] No operational database is modified.
- [ ] No runtime is started, stopped, restarted, or deleted.
