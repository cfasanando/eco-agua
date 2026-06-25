# Matrix26 Operations — Phase 3D.3 implementation report

## Scope delivered

Phase 3D.3 stabilizes the Runtime Control Center introduced in Phases 3D.1 and 3D.2. It adds recovery and maintenance capabilities without expanding runtime control beyond the two explicit laboratory instances.

## Implemented capabilities

### Interrupted operation recovery

A startup runner reconciles persisted operations left in `REQUESTED` or `RUNNING`. It inspects the live runtime and marks each operation as:

- `RECOVERED` when completion can be proven.
- `INTERRUPTED` when the result cannot be proven safely.

The recovery process also reconciles the managed runtime state and writes updated operation metadata.

### Persistent concurrency boundary

Runtime operations are protected by:

- A `ReentrantLock` per runtime in the active Matrix26 process.
- A database check that rejects a new request while another operation remains `REQUESTED` or `RUNNING` for the same instance.

### PID integrity

The runtime detail now distinguishes:

- Valid managed PID.
- Stale PID file.
- Verified orphan process.
- Unrelated process occupying the registered port.

Starting is blocked while stale PID metadata exists. Stop and restart are blocked for orphan processes until explicit adoption.

### Safe orphan adoption

Adoption requires matching:

- Java executable.
- Runtime profile.
- `application.properties` path.
- Registered port.

Adoption records the existing PID and does not restart or terminate the process.

### Stale PID cleanup

Cleanup removes only the stale `runtime.pid` file after proving that it does not belong to a live expected runtime. No process or client data is removed.

### Controlled force stop

`ProcessHandle.destroyForcibly()` is available only as a separate action after a recorded graceful-stop timeout. It also requires:

- Laboratory allowlist membership.
- Verified process ownership.
- Exact typed confirmation.
- A second timeout and port-release check.

Matrix26 never force-stops the Control Center, a protected production portal, or an unrelated process.

### Log rotation

Current operation logs support:

- 10 MB default threshold.
- Five retained copies per log.
- GZIP compression.
- Automatic rotation before start and after stop when necessary.
- Manual rotation only while the runtime is stopped.

### Persistent operation metadata

Each operation updates an atomic, sanitized file:

```text
runtime-data/{instance-code}/operations/last-operation.json
```

The file contains operation identity, action, status, PIDs, port, timestamps, and a sanitized result message.

## New operation actions

- `FORCE_STOP`
- `ADOPT`
- `CLEAN_STALE_PID`
- `ROTATE_LOGS`

## New operation statuses

- `RECOVERED`
- `INTERRUPTED`

## New managed runtime states

- `ORPHAN_PROCESS`
- `PID_STALE`
- `PORT_CONFLICT`

## Security boundaries retained

Runtime control remains enabled only for:

- `matrix26-restaurant-lab`
- `matrix26-appearance-lab`

The following remain read only:

- Matrix26 Control Center (`8091`).
- Eco Agua (`8081`).
- Productos de la Selva Belén (`8082`).
- Restaurante El Buen Sabor (`8084`).

No arbitrary command input, shell execution, `taskkill`, `kill -9`, or PowerShell process termination was introduced.

## Validation performed in the generation environment

- Java 17 isolated compilation of the complete Operations package using dependency stubs.
- HTML structural validation for all Operations templates.
- CSS brace validation.
- GZIP rotation and archive-shifting execution harness.
- Stale PID detection execution harness.
- Static safety and endpoint verification script.
- Package application and ZIP integrity verification.

The complete Maven build must still be confirmed in the user's project environment.

## Next phase

Phase 3E.1 will introduce manual, verified database backups for `matrix26_appearance_lab`, without yet backing up files or scheduling recurring jobs.
