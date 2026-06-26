# Matrix26 Restore Manager — Phase 3F.2 implementation report

## Delivered capabilities

- Automated verification runs persisted independently from restore execution.
- Verification items with `MATCH`, `WARNING`, `MISMATCH`, `NOT_APPLICABLE`, and `FAILED` states.
- Database comparison against the encrypted backup snapshot:
  - table set;
  - normalized schema signatures;
  - exact row counts parsed from MariaDB extended `INSERT` statements.
- Module comparison using `modules.json`.
- Appearance comparison using `appearance.json` and the restored database.
- Restored resource verification using SHA-256 values from `instance-files.zip`.
- Runtime isolation verification for instance code, database, runtime profile, port, URL, and ownership markers.
- HTTP route checks with configurable paths and timeout.
- Downloadable plain-text verification report without secrets or record contents.
- Safe resume planning and execution for `FAILED` and `CLEANUP_REQUIRED` jobs.
- Persistent resume audit events.
- Non-destructive cleanup dry run.

## Safety boundaries

- No `DROP DATABASE` was added.
- No target runtime or runtime-data deletion was added.
- No instance registration deletion was added.
- Completed steps are revalidated before they are skipped.
- Partially imported databases are not replayed automatically.
- Partially restored unowned file trees are not overwritten.
- Ownership markers bind runtime and runtime-data directories to the restore public ID.
- Verification decrypts into a temporary directory and removes only that temporary workspace.
- The original source instance is never modified by verification or resumption.

## New control tables

Created idempotently in `matrix26_platform_control`:

- `matrix26_restore_validation_run`
- `matrix26_restore_validation_item`
- `matrix26_restore_resume_event`

## Validation performed in the packaging environment

- Phase 3F.2 static checker: passed.
- MariaDB dump parser executable test: passed.
- Extended `INSERT` row-count test: passed.
- Template form-balance validation: passed.
- Controlled Java 17 compilation with dependency stubs for modified service, repository, initializer, controller, and verification classes: passed during implementation.
- Configuration preservation test with an existing datasource password: passed.
- Destructive cleanup audit: passed.
- Package application test on a clean copy of the provided context: passed.

## Environment limitation

A complete Maven build could not be executed in the packaging environment because Maven was unavailable and the Maven wrapper could not download its distribution. The source context supplied by the user contained a successful Maven compile report before Phase 3F.2. A complete `mvn clean -DskipTests package` remains an acceptance step on the user's development machine.
