# Matrix26 Lifecycle Manager — Phase 3G.1 Report

## Delivered

- Controlled suspension and reactivation for `matrix26-appearance-lab`.
- Explicit allowlist and protected-instance rejection.
- Recent encrypted and verified backup prerequisite.
- Persistent lifecycle jobs, events, and schedule snapshots.
- Runtime stop/start through the existing Runtime Control service.
- Pause and restoration of only the schedules enabled before suspension.
- Compensation that restores schedules if suspension fails.
- Backup, schedule execution, runtime, clone restore, and in-place restore conflict gates.
- Exact operator confirmations and operational reasons.
- Instance registry state transitions and central audit events.
- Lifecycle Manager list/detail UI and sidebar navigation.

## Safety boundaries

The implementation does not:

- Delete or modify the instance database.
- Delete runtime files or runtime-data.
- Delete backups.
- Change modules, users, roles, appearance, branding, or documents.
- Operate on protected instances.
- Operate outside `matrix26-appearance-lab` in Phase 3G.1.
- Execute arbitrary commands supplied by the browser.

## Validation completed

- Lifecycle Java sources compiled with Java 17 against controlled API stubs.
- Static security and route checks passed.
- Thymeleaf form balance checks passed.
- Configuration update was tested on a copied runtime configuration.
- The pre-change configuration copy matched byte-for-byte by SHA-256.
- Full Maven compilation must be confirmed in the user's project environment because Maven dependencies are not available in the packaging environment.
