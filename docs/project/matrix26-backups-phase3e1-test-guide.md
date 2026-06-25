# Matrix26 Backups — Phase 3E.1 test guide

## Scope

This phase creates manual, verified database backups only for:

- Instance code: `matrix26-appearance-lab`
- Runtime profile: `matrix26_appearance_lab`
- Database: `matrix26_appearance_lab`

It does not back up runtime files, uploads, branding assets, or operational instances. Those resources belong to Phase 3E.2.

## Prerequisites

1. Matrix26 Control Center compiles successfully.
2. `matrix26-appearance-lab` is registered in Matrix26.
3. The target runtime has `runtime-clients/matrix26_appearance_lab/application.properties`.
4. MySQL or MariaDB client tools are installed.
5. The Matrix26 process can resolve the database credentials declared by the target runtime.

## Find the database dump executable

From PowerShell:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/find-matrix26-database-dump-tool.ps1
```

If a tool is found, export its full path before starting Matrix26:

```bash
export MATRIX26_MYSQLDUMP_PATH='C:/Program Files/MariaDB 10.6/bin/mariadb-dump.exe'
```

Use the path printed by the script. Do not copy the sample path blindly.

## Optional backup root

The default root is:

```text
%USERPROFILE%/Matrix26/backups
```

To use another directory outside the Git repository:

```bash
export MATRIX26_BACKUP_ROOT='D:/Matrix26/backups'
```

## Start Matrix26

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/backups
```

The page must show:

- Backup root.
- Dump executable status.
- Tool version when available.
- No client database data.

## Create the first backup

1. Open **Backups**.
2. Select **New database backup**.
3. Select **Matrix26 Appearance Laboratory**.
4. Confirm the backup request.
5. Submit once.

Expected result:

- Status: `COMPLETED`.
- Table count greater than zero.
- Verification checks all pass.
- Four artifacts are registered.

Expected files:

```text
<backup-root>/matrix26-appearance-lab/YYYY/MM/backup-.../
├── database.sql.gz
├── manifest.json
├── checksums.sha256
└── backup-report.txt
```

## Required verification checks

- Target database exists and contains tables.
- SQL export is non-empty.
- GZIP is readable.
- Database ownership marker is present.
- At least one `CREATE TABLE` statement exists.
- SHA-256 matches after a second calculation.

## Failure tests

### Tool unavailable

Start Matrix26 without `MATRIX26_MYSQLDUMP_PATH` when the tool is not installed in a detectable location.

Expected result:

- The Backup Manager loads normally.
- It shows **Configuration required**.
- The create button remains disabled.
- No backup job is created.

### Protected instance

Open the backup form and inspect Eco Agua, Productos de la Selva, and Restaurante El Buen Sabor.

Expected result:

- They remain disabled.
- Their database names cannot be submitted through the form.
- A crafted POST is rejected by the service allowlist.

### Control database

The database `matrix26_platform_control` must never be accepted as a client backup target.

### Duplicate request

Submit two requests for the same instance while one is active.

Expected result:

- The second request is rejected.
- Only one database dump process runs.

## Security checks

- Database password does not appear in the process command line.
- Database password does not appear in Matrix26 audit entries.
- Database password does not appear in the manifest or report.
- The backup root is outside the project.
- No files are created under `src`, `target`, `runtime-data`, or Git-tracked directories.

## Database tables created in Matrix26 only

```text
matrix26_backup_job
matrix26_backup_artifact
matrix26_backup_verification
```

No manual SQL is required.
