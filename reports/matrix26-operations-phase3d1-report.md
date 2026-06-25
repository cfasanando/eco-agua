# Matrix26 Operations Phase 3D.1 Report

## Objective

Provide a trustworthy, read-only runtime inventory before enabling process
control in Phase 3D.2.

## Delivered

### Operations navigation

- Operations Center dashboard.
- Runtime inventory.
- Runtime technical detail.
- Expected port inventory.
- Log inventory.
- Direct runtime link from the instance detail page.

### Runtime inspection

- Dedicated Matrix26 Control Center runtime is included as a synthetic protected target.
- Registered business instances are loaded from `matrix26_platform_control`.
- Runtime directories are resolved from approved relative paths.
- `application.properties` is parsed without exposing credentials.
- Registered port, runtime profile, and database are compared with local configuration.
- Compatible launchers are detected: `run.sh`, `run.ps1`, `run.cmd`, and `run.bat`.

### Process and port inspection

- Java `ProcessHandle` provides process metadata without requiring shell input.
- Windows port ownership uses a fixed PowerShell command.
- Windows `netstat` is used as a fallback.
- Unix-like systems use `ss` when available.
- A non-destructive socket binding check detects occupancy when PID ownership is unavailable.
- No user-supplied command is executed.

### Availability

- Only local URLs are probed by the operations inventory.
- Remote URLs are not contacted from this module.
- HTTP status and response time are displayed.
- Runtime status consolidates process, port, HTTP, and configuration evidence.

### Logs and storage

- Log discovery is restricted to approved project roots.
- Supported extensions: `.log`, `.out`, and `.err`.
- Log tails are sanitized before rendering.
- Passwords, secrets, tokens, API keys, and private keys are masked.
- Runtime and instance asset storage are calculated separately.

### Safety

Phase 3D.1 contains no runtime write actions.

It does not:

- start processes;
- stop processes;
- restart processes;
- execute registered runtime commands;
- write PID files;
- edit launchers;
- delete logs;
- modify runtime configuration;
- query operational business tables;
- modify operational databases.

## Status model

- `ONLINE`
- `OFFLINE`
- `PORT_OCCUPIED`
- `PROCESS_FOUND`
- `PROCESS_NOT_FOUND`
- `CONFIGURATION_MISSING`
- `RUNTIME_MISSING`
- `LOG_MISSING`
- `DEGRADED`
- `UNKNOWN`

## Configuration defaults

The following properties have safe defaults and do not require changes to the
existing Matrix26 runtime configuration:

```properties
matrix26.control-center.operations-runtime-directory=runtime-clients
matrix26.control-center.operations-data-directory=runtime-data
matrix26.control-center.operations-log-directory=logs
matrix26.control-center.operations-cache-seconds=5
matrix26.control-center.operations-connect-timeout-ms=700
matrix26.control-center.operations-read-timeout-ms=700
matrix26.control-center.operations-log-tail-lines=120
```

## Validation performed

- Java 17 isolated compilation with framework stubs.
- Runtime probe smoke test.
- Log sanitization smoke test.
- HTML structure checks.
- CSS brace validation.
- Static route and safety checks.
- Package application test over the supplied context.

A full Maven build must be confirmed in the project environment because Maven is
not installed in the artifact execution container.

## Next phase

Phase 3D.2 will add controlled start, stop, and restart operations only for
explicitly authorized laboratory instance codes. Protected instances will remain
blocked.
