# Matrix26 Restore Manager — Phase 3F.3 implementation report

## Context reviewed

- The Phase 3F.2 project baseline compiled successfully in the user-provided context.
- Port `8095` was free while the context was collected.
- No `runtime-clients/matrix26_restore_test` or `runtime-data/matrix26-restore-test` directory was present in the context.
- HTTP checks for `8095` returned no connection because the clone was stopped or absent.
- Manual MySQL metadata capture failed with an access-denied response; the implementation therefore uses Matrix26's configured datasource, as the existing provisioning, backup, and restore services do.

## Implemented capabilities

- Persistent cleanup plans, plan items, and audit events.
- SHA-256 resource snapshot fingerprint.
- HMAC-SHA256 plan signature derived from the existing external backup master key.
- Fixed-target and protected-instance boundaries.
- Successful restore-verification protection.
- Source encrypted-backup availability check and permanent keep action.
- Independent exact confirmations for runtime, files, database, and registration.
- Final execution confirmation.
- Runtime stop through Runtime Control rather than an arbitrary process command.
- Expected-process and unexpected-port-owner protection.
- Ownership-marker verification for runtime and runtime-data directories.
- Symbolic-link rejection and normalized path-boundary validation.
- Dynamic cleanup of foreign-key dependents before removing the clone registration.
- Audit-log detachment instead of audit-log destruction.
- Database deletion restricted to `matrix26_restore_test` and performed after runtime, registration, and files.
- Per-item persistent progress and idempotent retry.
- Residual verification after execution.
- Startup recovery of interrupted cleanup plans and items.
- New statuses: `CLEANING`, `PARTIALLY_CLEANED`, and `CLEANED`.
- Updated Restore Manager detail UI.

## Validation performed

- Pure Java cleanup models compiled with Java 17.
- Cleanup repository and cleanup service compiled against controlled Spring/internal API stubs.
- Modified controller and initializer compiled against controlled API stubs.
- Phase 3F.3 static verification script passed.
- HTML form-balance validation passed.
- Configuration preservation test passed, including an existing datasource password and unrelated values.
- Package application is tested separately against a clean copy before delivery.

## Environment limitation

The full Maven wrapper could not download Maven in the execution environment. The user-provided baseline already reports `BUILD SUCCESS`; the full `mvn clean -DskipTests package` must be run in the user's local project after applying the package.

## Destructive boundary

This phase can issue `DROP DATABASE` only after all approvals and only for the exact isolated restore target. It cannot target:

- `matrix26_platform_control`
- `matrix26_appearance_lab`
- any protected production/demo instance
- the encrypted source backup
- arbitrary paths or arbitrary database names
