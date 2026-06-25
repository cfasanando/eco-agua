# Matrix26 Operations Phase 3D.2 Report

## Delivered

- explicit runtime-control allowlist for the two laboratory instances;
- Java `ProcessBuilder` startup without user-provided shell commands;
- PID persistence under each instance runtime-data directory;
- persistent standard and error logs;
- process ownership verification before stop or restart;
- normal `ProcessHandle.destroy()` stop request;
- no automatic force stop;
- start health-check timeout and stop timeout handling;
- restart as verified stop followed by verified start;
- runtime state and operation history in the Matrix26 control database;
- authenticated actor, PID, duration, status, and sanitized error details;
- confirmation strings for stop and restart;
- runtime operation controls in inventory and detail pages;
- log discovery extended to `runtime-data`;
- instance audit integration.

## Control database tables

- `matrix26_runtime_state`;
- `matrix26_runtime_operation`.

These tables are created idempotently at Matrix26 startup. No SQL is added to application source resources and no operational client database is changed.

## Safety decisions

- Matrix26 cannot control its own process.
- Production instances are not in the allowlist.
- A generic protected flag does not accidentally block the explicitly authorized laboratories, but it does not grant control to any other protected instance.
- Runtime ownership must match the expected profile and `application.properties` command line.
- A foreign process occupying the registered port is never stopped.
- Stop timeout leaves the process active for manual review.
- Runtime commands stored in the database are displayed as references only and are not executed.

## Validation performed in the generation environment

- all Phase 3D.2 Java sources compiled with Java 17 against isolated framework stubs;
- templates parsed successfully as HTML;
- Java brace and structure checks passed;
- no force-stop call exists in `Matrix26RuntimeControlService`;
- no production runtime is present in the default allowlist;
- final package applied successfully over a clean context copy;
- ZIP integrity verified.

A full Maven build and live Windows process test must be completed in the target workstation.
