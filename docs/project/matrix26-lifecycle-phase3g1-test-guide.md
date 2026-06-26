# Matrix26 Lifecycle Manager — Phase 3G.1 Test Guide

## Scope

Phase 3G.1 adds controlled suspension and reactivation for the allowlisted laboratory instance only:

- Instance code: `matrix26-appearance-lab`
- Runtime profile: `matrix26_appearance_lab`
- Database: `matrix26_appearance_lab`
- Port: `8094`

Protected production/demo instances and Matrix26 Control Center remain read only.

## Preconditions

1. Matrix26 Control Center is stopped before applying the package.
2. Phase 3E Backup Manager and Phase 3F Restore Manager are already installed.
3. `matrix26-appearance-lab` has a full encrypted backup with verification state `VERIFIED` from the last 72 hours.
4. No backup, runtime, clone restore, in-place restore, or lifecycle job is active.
5. Port `8094` is owned by the expected laboratory runtime or is free.

## Apply

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

unzip "$HOME/Downloads/matrix26-lifecycle-phase3g1-final.zip" \
  -d "$HOME/Downloads"

SRC="$HOME/Downloads/matrix26-lifecycle-phase3g1-final"

cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/

chmod +x scripts/check-matrix26-lifecycle-phase3g1.sh
chmod +x scripts/configure-matrix26-lifecycle-phase3g1.sh
```

## Configure

```bash
bash scripts/configure-matrix26-lifecycle-phase3g1.sh
```

The script stores the previous runtime configuration under:

```text
runtime-data/matrix26-control/maintenance/application.properties.phase3g1.bak
```

It does not write or change passwords, tokens, MySQL credentials, or the backup master key.

## Static verification

```bash
bash scripts/check-matrix26-lifecycle-phase3g1.sh
```

Expected final line:

```text
Matrix26 Lifecycle Manager Phase 3G.1 static checks passed.
```

## Build and start

```bash
rm -rf target
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/lifecycle
```

## Suspension test

1. Confirm that `8094` is online.
2. Confirm that Lifecycle Manager shows a recent verified encrypted backup.
3. Enter an operational reason of at least 10 characters.
4. Enter exactly:

```text
SUSPEND matrix26-appearance-lab
```

5. Submit the suspension.

Expected results:

- Lifecycle job ends as `SUSPENDED`.
- Instance registry status becomes `SUSPENDED`.
- Runtime status becomes `STOPPED`.
- Port `8094` is free.
- Previously enabled backup schedules are disabled and snapshotted.
- Database, runtime directory, runtime-data, modules, appearance, users, and backups still exist.
- Ports `8091`, `8081`, `8082`, and `8084` are not modified.

## Reactivation test

1. From Lifecycle Manager, enter a reason.
2. Enter exactly:

```text
REACTIVATE matrix26-appearance-lab
```

3. Submit the reactivation.

Expected results:

- Runtime Control starts `8094`.
- HTTP health check succeeds.
- Instance status returns to `ACTIVE`.
- Runtime status returns to `ONLINE`.
- Only schedules captured during suspension are re-enabled.
- The lifecycle job ends as `ACTIVE`.

## Conflict tests

Suspension/reactivation must be blocked when any of these are active:

- Backup job.
- Scheduled backup execution.
- Runtime operation.
- Clone restore.
- In-place restore awaiting completion, confirmation, or rollback.
- Another lifecycle job.
- Unexpected process on port `8094`.

## Git boundary

Do not commit:

```text
runtime-data/
runtime-clients/matrix26_appearance_lab/
C:/Users/PC/Matrix26/backups/
```
