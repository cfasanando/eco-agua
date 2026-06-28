# Matrix26 Lifecycle Manager Phase 3G.2 Report

## Delivered scope

- Controlled decommission preparation for `matrix26-appearance-lab`.
- Mandatory new full backup dedicated to the final lifecycle.
- AES-256-GCM encryption and independent verification.
- `FINAL` retention classification and deletion protection.
- Separate preparation and execution confirmations.
- Persistent jobs, checks, events, schedule snapshots, and retention evidence.
- Permanent schedule disablement after decommission.
- Historical decommissioned inventory.
- Runtime Control lockout for `DECOMMISSIONED` instances.
- Protection of business instances and Matrix26 Control Center.

## Safety characteristics

- The target must already be `SUSPENDED`.
- The runtime process must be absent and the registered port must be free.
- Backup, runtime, restore, cleanup, lifecycle, and decommission conflicts are blocked.
- The final backup is generated even when another recent backup exists.
- The final archive is reverified before the status transition.
- No database, runtime, runtime-data, module, appearance, or backup deletion is implemented.
- Failure during execution returns the instance to `SUSPENDED` and preserves all evidence for review.

## New central tables

- `matrix26_decommission_job`
- `matrix26_decommission_check`
- `matrix26_decommission_event`
- `matrix26_decommission_schedule_state`

## Routes

- `/control-center/lifecycle/decommission`
- `/control-center/lifecycle/decommission/new`
- `/control-center/lifecycle/decommission/{jobId}`
- `/control-center/lifecycle/decommission/{jobId}/execute`
- `/control-center/lifecycle/decommission/decommissioned`

Dynamic job routes accept numeric identifiers only.

## Validation performed

- Controlled Java 17 compilation of all new 3G.2 classes using isolated dependency stubs.
- Static validation of final archive creation, encryption, `FINAL` retention, and independent verification.
- Static validation of allowlist and protected-instance boundaries.
- Static audit for destructive database and filesystem operations.
- HTML structure validation for all new Thymeleaf templates.
- Configuration script audit to confirm that credentials and master keys are not written.
- Overlay application test against a clean copy of the supplied context.

## Environment limitation

The full Maven build could not be executed in the packaging environment because Maven was unavailable and the wrapper could not download its distribution. The supplied context reported `BUILD SUCCESS` before the 3G.2 changes. A full `mvn clean -DskipTests package` must be run in the user's project after application.
