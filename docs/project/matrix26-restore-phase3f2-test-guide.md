# Matrix26 Restore Manager — Phase 3F.2 test guide

## Scope

Phase 3F.2 adds automated post-restore verification, a downloadable verification report, safe restoration resumption, and a non-destructive cleanup preview for the isolated clone target:

- Source instance: `matrix26-appearance-lab`
- Source database: `matrix26_appearance_lab`
- Clone instance: `matrix26-restore-test`
- Clone database: `matrix26_restore_test`
- Clone port: `8095`

The phase does not delete databases, runtime directories, runtime-data directories, or instance registrations.

## 1. Apply the package

Stop Matrix26 Control Center on port `8091`, copy the package files into the project, and make the scripts executable.

```bash
chmod +x scripts/check-matrix26-restore-phase3f2.sh
chmod +x scripts/configure-matrix26-restore-phase3f2.sh
```

## 2. Configure Phase 3F.2

```bash
bash scripts/configure-matrix26-restore-phase3f2.sh
```

The script preserves the previous runtime configuration at:

```text
runtime-data/matrix26-control/maintenance/application.properties.phase3f2.bak
```

It does not write or modify database passwords, tokens, or `MATRIX26_BACKUP_MASTER_KEY`.

## 3. Static verification

```bash
bash scripts/check-matrix26-restore-phase3f2.sh
```

Expected final line:

```text
Matrix26 Restore Manager Phase 3F.2 static checks passed.
```

## 4. Build

```bash
rm -rf target
mvn clean -DskipTests package
```

Expected result:

```text
BUILD SUCCESS
```

## 5. Start required runtimes

Start Matrix26 Control Center and the restored clone. Keeping the source laboratory online is useful for visual comparison but is not required for database verification.

```text
8091 — Matrix26 Control Center
8094 — Matrix26 Appearance Laboratory, optional for visual comparison
8095 — Matrix26 Restore Test
```

## 6. Run automated verification

Open:

```text
http://localhost:8091/control-center/restores
```

Open the completed restore job and type the exact confirmation shown on screen:

```text
VERIFY <restore-public-id>
```

Select **Run automated verification**.

The verification reads the encrypted backup again and compares the clone against the backup snapshot, not against a possibly changed live source database.

Expected checks include:

- Encrypted backup authentication and internal checksums.
- Source registration identity.
- Restored database table set.
- Normalized `CREATE TABLE` signatures.
- Exact table row counts parsed from `database.sql.gz`.
- Assigned modules from `modules.json`.
- Published appearance metadata from `appearance.json`.
- SHA-256 hashes of restored instance-owned files.
- Runtime profile, database, port, URL, and ownership markers.
- HTTP routes configured for the restored clone.

Expected overall result when the clone matches and is online:

```text
RESTORE VERIFIED
```

A stopped runtime can produce `VERIFIED WITH WARNINGS` for HTTP checks while database and file checks remain valid.

## 7. Download the report

From the restore detail page, select **Download verification report**.

The text report contains the source and target summaries, each check status, and differences. It does not contain database passwords, the backup master key, decrypted SQL, or record contents.

## 8. Test mismatch detection safely

Use only a disposable clone. A safe test is to stop port `8095` and run verification again.

Expected result:

- Database, module, appearance, and resource checks remain `MATCH`.
- HTTP checks become `WARNING` or `MISMATCH`, depending on the route result.
- The report identifies the failed route without exposing credentials.

Restart `8095` and rerun verification to return to a verified state.

## 9. Test safe resumption

Resumption is available only for jobs in `FAILED` or `CLEANUP_REQUIRED`.

Open the failed job and review **Resume plan**. Matrix26 validates each completed step before skipping it.

Type:

```text
RESUME <restore-public-id>
```

Then select **Resume restore**.

Safe behavior:

- An already imported database is adopted only when its completed step is valid.
- An empty target database may be imported again.
- A partially imported database is blocked and marked for cleanup review.
- A completed file restore is adopted only when its target directory exists.
- A partially populated unowned file directory is blocked.
- A matching instance registration may be adopted.
- A conflicting registration, runtime marker, database, or port is blocked.
- Start and health-check steps may be retried.

## 10. Review cleanup preview

The restore detail page contains a dry-run inventory for:

- Target database.
- Runtime directory.
- Runtime-data directory.
- Instance registration.
- Module declarations.
- Temporary extraction directory.

Phase 3F.2 performs no deletion. Items are labeled by presence, ownership, and cleanup eligibility for later review.

## 11. Acceptance criteria

Phase 3F.2 is complete when:

1. The project builds successfully.
2. Static checks pass.
3. A completed clone can be verified from the encrypted backup.
4. Schema and row counts match.
5. Module and appearance metadata match.
6. Restored resource hashes match.
7. Runtime isolation checks pass.
8. HTTP checks reflect the real runtime state.
9. A report downloads successfully.
10. A failed job displays a safe resume plan.
11. Cleanup remains dry-run only.
12. `matrix26_appearance_lab` and port `8094` remain untouched.
