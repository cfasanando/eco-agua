# Matrix26 Lifecycle Manager Phase 3G.2

## Purpose

Phase 3G.2 performs a non-destructive operational decommission for explicitly allowlisted laboratory instances.

The first allowed target is:

- Instance code: `matrix26-appearance-lab`
- Database: `matrix26_appearance_lab`
- Runtime profile: `matrix26_appearance_lab`
- Port: `8094`

Protected business instances and Matrix26 Control Center remain read only.

## Preconditions

1. Matrix26 Control Center is running on `8091`.
2. The target instance is already `SUSPENDED` through Lifecycle Manager 3G.1.
3. Port `8094` is free and no runtime process is active.
4. `MATRIX26_BACKUP_MASTER_KEY` is available to the Matrix26 process.
5. The MariaDB dump tool and backup root are correctly configured.
6. No backup, restore, cleanup, runtime, lifecycle, or decommission job is active for the target.

## Apply

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

unzip "$HOME/Downloads/matrix26-lifecycle-phase3g2-final.zip" \
  -d "$HOME/Downloads"

SRC="$HOME/Downloads/matrix26-lifecycle-phase3g2-final"

cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/

chmod +x scripts/check-matrix26-lifecycle-phase3g2.sh
chmod +x scripts/configure-matrix26-lifecycle-phase3g2.sh
```

## Configure

```bash
bash scripts/configure-matrix26-lifecycle-phase3g2.sh
```

The script preserves the previous runtime configuration at:

```text
runtime-data/matrix26-control/maintenance/application.properties.phase3g2.bak
```

It does not write or change database passwords, tokens, API keys, or the backup master key.

## Static verification

```bash
bash scripts/check-matrix26-lifecycle-phase3g2.sh
```

Expected final line:

```text
Matrix26 Lifecycle Manager Phase 3G.2 static checks passed.
```

## Build

```bash
rm -rf target
mvn clean -DskipTests package
```

Expected result:

```text
BUILD SUCCESS
```

## Start

```bash
bash scripts/run-matrix26-control.sh
```

No manual SQL is required. Matrix26 creates the following central tables:

```text
matrix26_decommission_job
matrix26_decommission_check
matrix26_decommission_event
matrix26_decommission_schedule_state
```

## Functional test

Open:

```text
http://localhost:8091/control-center/lifecycle/decommission
```

The target must appear as eligible only when it is `SUSPENDED` and its runtime is stopped.

Open:

```text
http://localhost:8091/control-center/lifecycle/decommission/new
```

Use:

```text
Reason: Laboratory retirement validation for Matrix26 Lifecycle Manager.
Retention: 30 days
Confirmation: PREPARE DECOMMISSION matrix26-appearance-lab
```

Preparation must:

1. Validate the laboratory allowlist and protected-instance boundary.
2. Confirm the suspended state and free port.
3. Create a new full instance backup.
4. Encrypt it with AES-256-GCM.
5. Assign retention class `FINAL`.
6. Mark it deletion-protected.
7. Verify the encrypted package again.
8. Disable any remaining schedules.
9. Finish as `READY_TO_DECOMMISSION`.

Then enter:

```text
DECOMMISSION matrix26-appearance-lab
```

The execution must:

1. Reverify the final archive.
2. Confirm the runtime remains stopped.
3. Confirm no schedules remain enabled.
4. Change the instance status to `DECOMMISSIONED`.
5. Keep the database, runtime, resources, modules, appearance, and all backups.
6. Prevent Runtime Control from starting the decommissioned instance.

Expected final state:

```text
Decommission job: DECOMMISSIONED
Instance status: DECOMMISSIONED
Runtime status: STOPPED
Port 8094: FREE
```

## Manual preservation checks

The following must continue to exist:

```text
matrix26_appearance_lab
runtime-clients/matrix26_appearance_lab/
runtime-data/matrix26-appearance-lab/
```

The final backup directory must contain:

```text
package.m26backup
public-manifest.json
checksums.sha256
backup-report.txt
```

The final archive metadata must indicate:

```text
Retention class: FINAL
Verification: VERIFIED
Protected: Yes
```

## Security boundaries

Phase 3G.2 does not contain:

- `DROP DATABASE`
- runtime directory deletion
- runtime-data deletion
- backup deletion
- module deletion
- appearance deletion
- automatic purge

The next phase, 3G.3, will manage final archive inventory and future recovery as a clone. It will not purge operational data.
