# Matrix26 Restore Manager — Phase 3F.1 implementation report

## Delivered

- Restore dashboard, candidate list, creation form, job detail, steps, artifacts, and verifications.
- Restore metadata tables created idempotently in `matrix26_platform_control`.
- Public extraction API for verified AES-256-GCM backup packages.
- Fixed no-overwrite clone target on port 8095.
- MariaDB/MySQL import using credentials through `MYSQL_PWD`, never command arguments.
- Safe ZIP extraction with traversal checks and namespace remapping.
- Source-to-clone appearance identity remapping.
- Runtime generation from the real local source runtime while preserving credentials locally.
- Clone registration and module declaration copy.
- Optional start and HTTP health verification through Runtime Control Center.
- Runtime Control allowlist extension for `matrix26-restore-test`.
- Audit events for completed and failed clone restores.
- Persistent status, steps, artifacts, and verification trace.
- `FAILED` versus `CLEANUP_REQUIRED` separation.

## Safety boundaries

- No `DROP DATABASE` operation.
- No reuse or overwrite of existing database, port, instance, runtime profile, runtime directory, or runtime-data directory.
- Only verified encrypted full backups from `matrix26-appearance-lab` are eligible.
- The source instance and backup files are read-only.
- Temporary decrypted content is removed after execution.
- No master key or database password is stored in restore tables, logs, manifests, or process arguments.
- The restored clone is registered as a protected instance.

## Validation performed in the generation environment

- Static security checker passed.
- Restore package compiled against controlled Java 17 stubs, validating syntax and project API usage.
- HTML structure and form balance checks passed.
- Clean package application test prepared.
- Full Maven build must be confirmed in the project environment because Maven distribution download is unavailable in the generation environment.
