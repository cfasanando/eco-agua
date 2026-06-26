# Matrix26 Restore Manager — Phase 3F.1 test guide

## Scope

Phase 3F.1 restores one verified encrypted full backup of `matrix26-appearance-lab` as an isolated clone:

- instance code: `matrix26-restore-test`
- database: `matrix26_restore_test`
- runtime profile: `matrix26_restore_test`
- port: `8095`
- URL: `http://localhost:8095`

The source instance, database, runtime, and files are never overwritten.

## Preconditions

1. Matrix26 Control Center is stopped before compiling.
2. At least one full encrypted backup is `COMPLETED` and `VERIFIED`.
3. `MATRIX26_BACKUP_MASTER_KEY` is available to the Matrix26 process.
4. `mariadb.exe` or `mysql.exe` is installed.
5. Database `matrix26_restore_test` does not exist.
6. Port `8095` is free.
7. These paths do not exist:
   - `runtime-clients/matrix26_restore_test`
   - `runtime-data/matrix26-restore-test`
8. No Matrix26 instance is registered with the target code, database, runtime, or port.

## Apply

```bash
unzip "$HOME/Downloads/matrix26-restore-phase3f1-final.zip" -d "$HOME/Downloads"
SRC="$HOME/Downloads/matrix26-restore-phase3f1-final"
cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/
chmod +x scripts/check-matrix26-restore-phase3f1.sh
chmod +x scripts/configure-matrix26-restore-phase3f1.sh
bash scripts/configure-matrix26-restore-phase3f1.sh
bash scripts/check-matrix26-restore-phase3f1.sh
mvn clean -DskipTests package
```

## Main test

1. Start Matrix26.
2. Open `/control-center/restores`.
3. Select an eligible encrypted full backup.
4. Keep **Start the restored clone** enabled.
5. Type exactly `RESTORE matrix26-restore-test`.
6. Execute the restore once.

Expected steps:

1. Validate encrypted backup and destination — `COMPLETED`
2. Decrypt and verify recovery package — `COMPLETED`
3. Create isolated database — `COMPLETED`
4. Import restored database — `COMPLETED`
5. Restore instance files — `COMPLETED`
6. Generate clone runtime — `COMPLETED`
7. Register clone in Matrix26 — `COMPLETED`
8. Start clone runtime — `COMPLETED`
9. Verify clone health — `COMPLETED`

Expected final state: `COMPLETED`.

## Functional checks

Open:

- `http://localhost:8095`
- `http://localhost:8095/login`
- `http://localhost:8095/admin/restaurant/dashboard`
- `http://localhost:8095/restaurant/menu`

Confirm:

- the source portal on `8094` remains online and unchanged;
- the restored portal uses database `matrix26_restore_test`;
- restored business data exists;
- branding, logo, favicon, login image, theme, and layouts match the backup;
- runtime files exist only under `runtime-clients/matrix26_restore_test`;
- assets exist only under `runtime-data/matrix26-restore-test`;
- Runtime Control can stop and start the clone;
- the clone is protected from deletion.

## Negative tests

The restore must be blocked if any target resource already exists. Test only after preserving the completed clone or in a disposable environment:

- target database exists;
- port `8095` is occupied;
- target runtime directory exists;
- target runtime-data directory exists;
- target instance code is registered;
- wrong confirmation phrase;
- backup is not encrypted or not verified;
- master key is unavailable or incorrect.

## Failure behavior

If failure occurs before creating target resources, status is `FAILED`.

If failure occurs after creating a database, directory, or registered instance, status is `CLEANUP_REQUIRED`. Matrix26 does not automatically drop a database or delete partially restored resources in Phase 3F.1.
