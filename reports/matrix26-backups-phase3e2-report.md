# Matrix26 Backups — Phase 3E.2 implementation report

## Delivered

- Full instance backup option alongside the database-only option.
- Database export inherited from Phase 3E.1.
- ZIP archive of instance-owned runtime data and approved launcher files.
- Published Appearance Studio resources and history.
- Upload, document, attachment, media, and public resource roots when present.
- Sanitized runtime configuration stored outside the ZIP.
- Instance metadata export.
- Module assignment export.
- Appearance assignment export.
- Before-and-after file inventory.
- Online consistency warning when files change during the backup.
- Sanitized diagnostic log tail instead of raw operation logs.
- ZIP readability and path traversal verification.
- Symbolic-link exclusion.
- Per-file and total archive size limits.
- Cross-artifact SHA-256 verification.
- Full manifest format version 2.
- Human-readable full backup report.
- Full-package audit actions.
- Safe configuration helper that patches only the correctly named Spring backup properties and preserves runtime secrets.
- Existing database-only backup flow retained.

## Recovery artifacts

```text
database.sql.gz
instance-files.zip
runtime-config.properties
instance.json
modules.json
appearance.json
files-inventory.json
runtime-log-tail.txt
manifest.json
checksums.sha256
backup-report.txt
```

## Safety boundaries

- Only `matrix26-appearance-lab` remains allowlisted.
- No user-provided database name or filesystem path is executed.
- Database passwords remain in the dump process environment only.
- Raw runtime configuration is never placed in the backup package.
- Raw operation logs are excluded.
- Runtime JAR files are excluded.
- Symbolic links are skipped.
- ZIP entries are checked for absolute paths and traversal segments.
- Backup storage remains outside the Git repository.
- Protected operational instances remain unavailable.
- No restoration or deletion operation is included in this phase.

## Important correction

The submitted runtime contained legacy keys:

```text
matrix26.backup.dump-tool
matrix26.backup.root
```

The included configuration helper replaces those legacy keys with the actual `@ConfigurationProperties` prefix:

```text
matrix26.control-center.backups.dump-executable
matrix26.control-center.backups.root-directory
```

The helper edits only backup settings, preserves the original runtime file under ignored `runtime-data/matrix26-control/maintenance/`, and does not overwrite database or administrator credentials.

## Validation performed

- Existing Phase 3E.1 backup manifest reviewed: passed.
- Runtime-data inventory reviewed: published appearance resources detected.
- Java 17 isolated compilation of the new full-backup assembler: passed.
- Java 17 isolated compilation of the modified backup service: passed.
- Synthetic ZIP creation and readback test: passed.
- Synthetic runtime configuration redaction test: passed.
- HTML parser validation: passed.
- CSS brace validation: passed.
- Static security verifier: passed.
- Clean-context package apply test: required before delivery.

## Deferred to Phase 3E.3

- Backup encryption.
- Configurable retention policies.
- Scheduled backups.
- Backup expiration and pruning.
- External/off-machine backup destinations.
