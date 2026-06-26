# Matrix26 Backups — Phase 3E.3 test guide

## Scope

Phase 3E.3 adds encrypted full-instance backups, independent reverification, count-based retention policies, and manual cleanup preview for:

- Instance code: `matrix26-appearance-lab`
- Runtime profile: `matrix26_appearance_lab`
- Database: `matrix26_appearance_lab`
- Runtime port: `8094`

Database-only and unencrypted full backups from previous phases remain supported.

## Security model

Encrypted packages use:

```text
AES-256-GCM
Authenticated package format: M26BKP01 / version 1
Master key environment variable: MATRIX26_BACKUP_MASTER_KEY
```

The master key is never stored in:

- `matrix26_platform_control`
- `application.properties`
- Git
- public manifests
- backup reports
- `package.m26backup`

Only a non-secret key identifier is persisted.

## Apply the phase

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

bash scripts/configure-matrix26-backup-security-phase3e3.sh
bash scripts/check-matrix26-backups-phase3e3.sh

rm -rf target
mvn clean -DskipTests package
```

Expected results:

```text
Matrix26 Backups Phase 3E.3 static checks passed.
BUILD SUCCESS
```

## Generate the development master key

From Git Bash:

```bash
powershell.exe -ExecutionPolicy Bypass \
  -File scripts/configure-matrix26-backup-master-key-phase3e3.ps1
```

The script:

- generates 32 cryptographically random bytes;
- stores the Base64 value in the current Windows user environment;
- does not write the key into the project;
- prints the recovery value once so it can be stored in a password manager.

Close the current Git Bash terminal and open a new one before starting Matrix26.

Verify that the variable is available without printing its value:

```bash
powershell.exe -NoProfile -Command '
$value = [Environment]::GetEnvironmentVariable("MATRIX26_BACKUP_MASTER_KEY", "User")
if ([string]::IsNullOrWhiteSpace($value)) {
  Write-Host "KEY_NOT_CONFIGURED"
} else {
  Write-Host "KEY_CONFIGURED"
  Write-Host "Length=$($value.Length)"
}
'
```

Expected result:

```text
KEY_CONFIGURED
```

## Start Matrix26

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/backups
```

The encryption card must show:

```text
AES-256-GCM ready
```

It must display a key identifier, never the key itself.

## Create the first encrypted backup

Open:

```text
http://localhost:8091/control-center/backups/new
```

Select:

```text
Backup type: Full instance backup
Instance: Matrix26 Appearance Laboratory
Encryption: Enabled
Retention class: Daily
```

Mark the confirmation and create the package.

Expected job result:

```text
Backup type: MANUAL_FULL_ENCRYPTED
Status: COMPLETED
Verification status: VERIFIED
```

Expected public files:

```text
package.m26backup
public-manifest.json
checksums.sha256
backup-report.txt
```

The following plaintext recovery artifacts must no longer remain beside the encrypted package:

```text
database.sql.gz
instance-files.zip
runtime-config.properties
instance.json
modules.json
appearance.json
files-inventory.json
manifest.json
```

They are contained inside `package.m26backup`.

## Required verification results

The backup detail must include `PASSED` results for:

```text
ENCRYPTED_PACKAGE
INTERNAL_CHECKSUMS
```

The original Phase 3E.2 verifications remain visible as historical checks.

## Verify public checksums manually

```bash
BACKUP_ROOT="/c/Users/PC/Matrix26/backups/matrix26-appearance-lab"
LATEST_BACKUP="$(find "$BACKUP_ROOT" -name package.m26backup -printf '%T@ %h\n' | sort -nr | head -1 | cut -d' ' -f2-)"

cd "$LATEST_BACKUP"
sha256sum -c checksums.sha256
```

Expected result:

```text
package.m26backup: OK
public-manifest.json: OK
```

## Reverify without creating another backup

From the encrypted backup detail, click **Verify again**.

Matrix26 must:

1. verify the public package SHA-256;
2. confirm that the available key identifier matches;
3. decrypt into an isolated temporary directory;
4. authenticate AES-GCM;
5. inspect the internal ZIP;
6. verify every internal checksum;
7. remove the temporary directory;
8. update the verification timestamp.

Expected result:

```text
Verification status: VERIFIED
```

The temporary directory under the backup root must not retain decrypted files after the operation.

## Wrong-key test

Do not destroy the real key. For a temporary negative test, stop Matrix26 and start it from a terminal with a different process-only value:

```bash
export MATRIX26_BACKUP_MASTER_KEY="$(openssl rand -base64 32)"
bash scripts/run-matrix26-control.sh
```

Open the same backup and click **Verify again**.

Expected result:

```text
KEY_UNAVAILABLE
```

The package must not decrypt. Stop Matrix26, close that terminal, and restart it from a new terminal that inherits the persistent correct key.

## Tamper test

Copy the package before testing:

```bash
cp "$LATEST_BACKUP/package.m26backup" "$LATEST_BACKUP/package.m26backup.test-copy"
printf 'X' >> "$LATEST_BACKUP/package.m26backup"
```

Click **Verify again**.

Expected result:

```text
CORRUPTED
```

Restore the original immediately:

```bash
mv -f "$LATEST_BACKUP/package.m26backup.test-copy" "$LATEST_BACKUP/package.m26backup"
```

Click **Verify again** once more. The status must return to `VERIFIED`.

## Configure retention policy

Open:

```text
http://localhost:8091/control-center/backups/policies
```

For the laboratory use:

```text
Daily: 2
Weekly: 1
Monthly: 1
Final archives: keep indefinitely
Retention cleanup: enabled
```

The low values above are for testing only. The recommended operational defaults remain 7 / 4 / 6.

## Create retention test points

Create at least three encrypted backups with retention class `DAILY`.

Then open:

```text
http://localhost:8091/control-center/backups/retention?instanceId=<laboratory-id>
```

Expected preview:

- the two newest daily backups are `KEEP`;
- the older daily backup is `DELETE`;
- the newest verified backup is always protected;
- final archives are always protected;
- corrupted or unverified backups are not deleted automatically.

## Execute manual cleanup

The cleanup form requires the exact phrase:

```text
CLEAN BACKUPS matrix26-appearance-lab
```

Only rows marked `DELETE` in the freshly recalculated preview may be removed.

Expected result:

- eligible backup directories are removed;
- their central metadata is removed;
- protected packages remain;
- a retention audit event is written;
- reclaimed storage is reported.

## Protected boundaries

Phase 3E.3 must reject encryption and retention operations for:

- Eco Agua;
- Productos de la Selva Belén;
- Restaurante El Buen Sabor;
- Matrix26 Control Center;
- any database name supplied outside the registered instance.

## No manual SQL

The following tables are created idempotently inside `matrix26_platform_control`:

```text
matrix26_backup_encryption
matrix26_backup_policy
matrix26_backup_retention_event
```

No SQL script must be executed manually.
