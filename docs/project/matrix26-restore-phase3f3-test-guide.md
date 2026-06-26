# Matrix26 Restore Manager — Phase 3F.3 test guide

## Purpose

Phase 3F.3 removes only residual resources owned by an incomplete restore job. It never removes the encrypted source backup or the source instance.

## Safety model

Cleanup requires three distinct stages:

1. Create a signed preview using `PREPARE CLEANUP <restore-public-id>`.
2. Approve four independent groups using exact confirmations for runtime, files, database, and registration.
3. Execute using `EXECUTE CLEANUP <restore-public-id>`.

The preview stores a SHA-256 snapshot fingerprint and an HMAC-SHA256 signature derived from the existing backup master key. Approval is rejected when the current target differs from the signed preview.

Cleanup is allowed only for `FAILED`, `CLEANUP_REQUIRED`, `CLEANING`, or `PARTIALLY_CLEANED` restore jobs targeting the fixed laboratory clone:

- Instance code: `matrix26-restore-test`
- Database: `matrix26_restore_test`
- Runtime profile: `matrix26_restore_test`
- Port: `8095`

A successful `VERIFIED` or `VERIFIED_WITH_WARNINGS` clone is blocked and must later use the decommission workflow.

## Installation

Stop Matrix26 Control Center and apply the package contents:

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

SRC="$HOME/Downloads/matrix26-restore-phase3f3-final"
cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/

chmod +x scripts/check-matrix26-restore-phase3f3.sh
chmod +x scripts/configure-matrix26-restore-phase3f3.sh
```

Configure the non-sensitive flag:

```bash
bash scripts/configure-matrix26-restore-phase3f3.sh
```

Run the static verifier:

```bash
bash scripts/check-matrix26-restore-phase3f3.sh
```

Compile:

```bash
mvn clean -DskipTests package
```

Start Matrix26 from a terminal that has `MATRIX26_BACKUP_MASTER_KEY` available:

```bash
bash scripts/run-matrix26-control.sh
```

## Metadata tables

The following tables are created automatically in `matrix26_platform_control`:

- `matrix26_restore_cleanup_plan`
- `matrix26_restore_cleanup_item`
- `matrix26_restore_cleanup_event`

Do not run manual SQL.

## Safe functional test

Do not test deletion against a completed and verified clone. Use a laboratory restore job intentionally left in `FAILED` or `CLEANUP_REQUIRED`.

Open:

```text
http://localhost:8091/control-center/restores
```

Enter the incomplete restore job and create a preview with:

```text
PREPARE CLEANUP RST-...
```

Expected result:

- A cleanup plan with public ID `CLN-...`.
- Status `PREVIEW_READY` when all ownership checks pass.
- Status `BLOCKED` when a conflict exists.
- A stored fingerprint and HMAC signature.
- A table containing runtime process, modules, registration, runtime directory, runtime-data, database, temporary extraction, and protected source backup.

### Approval

Enter all four confirmations exactly:

```text
STOP RUNTIME matrix26-restore-test
REMOVE FILES matrix26-restore-test
DROP DATABASE matrix26_restore_test
REMOVE REGISTRATION matrix26-restore-test
```

Expected result:

```text
APPROVED
```

Approval must fail when:

- The source backup is missing.
- A successful restore validation exists.
- Port `8095` belongs to an unexpected process.
- The registration does not match the restore job.
- A runtime or runtime-data ownership marker is missing or belongs to another job.
- A symbolic link is found.
- The signed snapshot changed after preview.

### Execution

Enter:

```text
EXECUTE CLEANUP RST-...
```

Expected order:

1. Stop the owned runtime and release port `8095`.
2. Remove module assignments.
3. Remove the matching central registration and dependent operational rows.
4. Remove the owned runtime directory.
5. Remove the owned runtime-data directory.
6. Drop only `matrix26_restore_test`.
7. Remove the job temporary extraction directory.
8. Verify that no target residue remains.

Expected final states:

```text
Cleanup plan: CLEANED
Restore job: CLEANED
```

The encrypted source backup must still exist and pass its public checksums.

## Interrupted cleanup test

Interrupt Matrix26 after at least one cleanup item completes. Start Matrix26 again.

Expected recovery:

- A plan left in `RUNNING` becomes `PARTIALLY_CLEANED`.
- An item left in `RUNNING` becomes `FAILED` with an interruption message.
- A restore job left in `CLEANING` becomes `PARTIALLY_CLEANED`.
- Re-entering `EXECUTE CLEANUP RST-...` skips completed items and resumes from the first unfinished item.

## Final verification

Confirm:

```text
Port 8095: free
runtime-clients/matrix26_restore_test: absent
runtime-data/matrix26-restore-test: absent
matrix26_restore_test: absent
platform_business_client code matrix26-restore-test: absent
source backup package: present
source instance matrix26-appearance-lab: unchanged
port 8094: unchanged
```

## Git exclusions

Do not commit:

```text
runtime-clients/matrix26_restore_test/
runtime-data/matrix26-restore-test/
runtime-data/matrix26-control/
C:/Users/PC/Matrix26/backups/
```
