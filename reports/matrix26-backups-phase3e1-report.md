# Matrix26 Backups — Phase 3E.1 implementation report

## Delivered

- Backup Manager navigation and dashboard.
- Manual database backup form.
- Explicit allowlist limited to `matrix26-appearance-lab`.
- Runtime configuration validation.
- MySQL JDBC target parsing.
- Automatic `mysqldump` or `mariadb-dump` detection.
- Explicit `MATRIX26_MYSQLDUMP_PATH` override.
- Backup root outside the repository.
- Free-space validation.
- Transaction-consistent database export.
- Password delivery through `MYSQL_PWD`, never through process arguments.
- Java-native GZIP compression.
- Streaming GZIP structural verification.
- Database ownership marker.
- `CREATE TABLE` verification.
- SHA-256 calculation and recheck.
- Manifest, checksums, and human-readable report.
- Central metadata tables in `matrix26_platform_control`.
- Matrix26 audit events for success and failure.
- Concurrent backup blocking per instance.
- Tool discovery helper for Windows.
- Static security verifier.

## Safety boundaries

- No backups of Eco Agua, Productos de la Selva, Restaurante El Buen Sabor, or Matrix26 Control Center.
- No user-supplied database names.
- No arbitrary shell commands.
- No command-line passwords.
- No database restoration.
- No file or resource backup yet.
- No deletion or retention policy yet.

## Environment finding

The submitted context confirmed that `mysql` and `mysqldump` were not available in the Git Bash PATH. The implementation therefore supports Windows executable discovery and the `MATRIX26_MYSQLDUMP_PATH` environment variable.

## Validation performed

- Java 17 isolated compilation with interface stubs: passed.
- HTML parser validation: passed.
- Java brace and structure checks: passed.
- CSS balance check: passed.
- Static backup security checks: passed.
- Package apply test on a clean context copy: required before delivery.

## Deferred to Phase 3E.2

- Runtime configuration archive.
- Branding and appearance assets.
- Uploads and documents.
- Complete instance package.
- Cross-artifact manifest.
- Complete-instance size accounting.
