# Matrix26 Backups — Phase 3E.2 test guide

## Scope

Phase 3E.2 adds a complete instance recovery package for:

- Instance code: `matrix26-appearance-lab`
- Runtime profile: `matrix26_appearance_lab`
- Database: `matrix26_appearance_lab`
- Runtime port: `8094`

The previous database-only backup remains available.

## Full package contents

A successful full backup creates:

```text
<backup-root>/matrix26-appearance-lab/YYYY/MM/backup-.../
├── database.sql.gz
├── instance-files.zip
├── runtime-config.properties
├── instance.json
├── modules.json
├── appearance.json
├── files-inventory.json
├── runtime-log-tail.txt
├── manifest.json
├── checksums.sha256
└── backup-report.txt
```

`instance-files.zip` may include only instance-owned recovery files:

```text
runtime-data/matrix26-appearance-lab/appearance/
runtime-data/matrix26-appearance-lab/uploads/
runtime-data/matrix26-appearance-lab/documents/
runtime-data/matrix26-appearance-lab/attachments/
runtime-data/matrix26-appearance-lab/media/
runtime-data/matrix26-appearance-lab/public/
runtime-clients/matrix26_appearance_lab/<approved launchers and metadata>
```

It excludes:

- Raw `application.properties`.
- JAR files.
- Raw operation logs.
- PID files.
- Temporary files.
- Backups from other instances.
- Symbolic links.
- `.git`, `target`, or source code.

A sanitized tail of current runtime logs is stored separately as `runtime-log-tail.txt`.

## Apply and validate

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

bash scripts/check-matrix26-backups-phase3e2.sh

rm -rf target
mvn clean -DskipTests package
```

Expected results:

```text
Matrix26 Backups Phase 3E.2 static checks passed.
BUILD SUCCESS
```

## Persistent configuration

Apply the configuration helper once. It updates only Matrix26 backup keys and preserves all existing database credentials and runtime settings:

```bash
bash scripts/configure-matrix26-backup-runtime-phase3e2.sh
```

The defaults match the detected Wamp installation and current backup root:

```text
C:/wamp64/bin/mariadb/mariadb10.10.2/bin/mariadb-dump.exe
C:/Users/PC/Matrix26/backups
```

To use other paths:

```bash
bash scripts/configure-matrix26-backup-runtime-phase3e2.sh \
  'D:/Tools/MariaDB/bin/mariadb-dump.exe' \
  'D:/Matrix26/backups'
```

The helper creates:

```text
runtime-data/matrix26-control/maintenance/application.properties.phase3e2.bak
```

It never reads or changes the MySQL password value. It replaces the legacy backup keys with the actual Spring `@ConfigurationProperties` keys.

## Start Matrix26

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/backups/new
```

## Create a full instance backup

1. Select **Full instance backup**.
2. Select **Matrix26 Appearance Laboratory**.
3. Mark the confirmation.
4. Submit once.

Expected result:

- Backup type: `MANUAL_FULL`.
- Status: `COMPLETED`.
- The database checks from Phase 3E.1 pass.
- The full-package checks pass.
- The artifact table lists all recovery files.

## Required full-package checks

Expected `PASSED` results:

- `FULL_ARCHIVE_READABLE`
- `RUNTIME_CONFIG_REDACTED`
- `INSTANCE_OWNERSHIP`
- `FULL_ARTIFACT_SHA256`

Expected `PASSED` or `WARNING`:

- `FILESET_STABILITY`

A warning is allowed when files change while the online backup runs. The backup remains readable, but restore testing is recommended.

## Verify the archive manually

From Git Bash, locate the newest backup:

```bash
BACKUP_ROOT="/c/Users/PC/Matrix26/backups/matrix26-appearance-lab"
LATEST_BACKUP="$(find "$BACKUP_ROOT" -name manifest.json -printf '%T@ %h\n' | sort -nr | head -1 | cut -d' ' -f2-)"

echo "$LATEST_BACKUP"
unzip -t "$LATEST_BACKUP/instance-files.zip"
unzip -l "$LATEST_BACKUP/instance-files.zip"
```

The listing must not contain:

```text
application.properties
runtime.pid
application.log
application-error.log
../
C:/
```

It should contain the published appearance resources and approved launcher files.

## Verify runtime redaction

```bash
grep -niE 'password|secret|token|api[-_.]?key|private[-_.]?key' \
  "$LATEST_BACKUP/runtime-config.properties"
```

Sensitive keys must show:

```text
***REDACTED***
```

## Verify checksums

```bash
cd "$LATEST_BACKUP"
sha256sum -c checksums.sha256
```

All listed files must return `OK`.

## Database-only regression

Create another backup with **Database only**.

Expected result:

- Backup type: `MANUAL_DATABASE`.
- Status: `COMPLETED`.
- It creates the original four database-backup artifacts.
- No `instance-files.zip` is created.

## Protected boundaries

The following remain disabled:

- Eco Agua.
- Productos de la Selva Belén.
- Restaurante El Buen Sabor.
- Matrix26 Control Center database.

A crafted request using another instance ID must be rejected by the service allowlist.

## No manual SQL

Phase 3E.2 reuses the Phase 3E.1 metadata tables and does not require SQL scripts.
