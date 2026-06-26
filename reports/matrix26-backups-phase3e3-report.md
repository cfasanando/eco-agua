# Matrix26 Backups — Phase 3E.3 implementation report

## Delivered

- Optional AES-256-GCM encryption for full instance backups.
- Authenticated Matrix26 package format `M26BKP01`, version 1.
- 96-bit random nonce per package.
- 128-bit GCM authentication tag.
- Master key loaded only from the configured environment variable.
- Stable, non-secret key identifier stored with the backup metadata.
- Public manifest with minimal non-sensitive metadata.
- Public SHA-256 checksums for the encrypted package and public manifest.
- Internal ZIP with the complete Phase 3E.2 recovery payload.
- Independent decryption and internal checksum reverification.
- Automatic temporary-directory cleanup.
- Removal of plaintext recovery artifacts only after successful encrypted-package verification.
- Encryption state and last-verification status in backup detail.
- Manual **Verify again** action.
- Retention classes: Daily, Weekly, Monthly, and Final archive.
- Per-instance retention policy with count limits.
- Dry-run retention preview.
- Exact confirmation phrase before cleanup.
- Newest verified backup protection.
- Final archive protection.
- Protection of corrupted and unverified backups from automatic cleanup.
- Retention request and deletion audit events.
- User-environment key generation helper for Windows development.
- Runtime configuration helper that writes no secret value.

## Public encrypted backup layout

```text
backup-.../
├── package.m26backup
├── public-manifest.json
├── checksums.sha256
└── backup-report.txt
```

The sensitive recovery payload remains encrypted inside `package.m26backup`.

## Central metadata

Created only in `matrix26_platform_control`:

```text
matrix26_backup_encryption
matrix26_backup_policy
matrix26_backup_retention_event
```

No binary data or master key is stored in the database.

## Safety boundaries

- Encryption and retention remain allowlisted to `matrix26-appearance-lab`.
- Existing unencrypted backups are not migrated or deleted automatically.
- A failed encryption attempt preserves the original plaintext backup.
- The encrypted package is authenticated and decrypted before plaintext artifacts are removed.
- A valid encrypted package is preserved if a later metadata operation fails.
- Public metadata does not reveal the database name or runtime configuration.
- The master key is absent from process arguments, properties, manifests, reports, database rows, and Git.
- Temporary decrypted data is created only under the configured backup root and removed after verification.
- ZIP entries are checked for absolute paths and traversal.
- Retention cleanup requires a fresh preview and exact instance-specific confirmation.
- The newest verified backup and all final archives remain protected.
- No operational database, runtime, upload directory, or Appearance Studio resource is modified by retention.

## Validation performed

- Java 17 isolated compilation of all new and modified Phase 3E.3 classes: passed.
- Service-level encrypted package round trip using the production package code: passed.
- Tampered encrypted package rejection: passed.
- AES-GCM standalone authentication test: passed.
- HTML parser validation: passed.
- CSS brace validation: passed.
- Static secret-persistence inspection: passed.
- Retention exact-confirmation and protection-rule inspection: passed.
- Runtime property helper inspection: passed.
- Package application test on a clean copy: passed.
- Final ZIP integrity and runtime-configuration exclusion: passed.

## Deferred to Phase 3E.4

- Scheduled backup calendar.
- Automatic execution and retry policy.
- Missed-run recovery.
- Notifications and backup alerts.
- Off-machine storage destinations.
- Key rotation workflow with package re-encryption.
