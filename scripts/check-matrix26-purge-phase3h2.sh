#!/usr/bin/env bash
set -euo pipefail

assert_file() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
}

assert_contains() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if ! grep -qE "$pattern" "$file"; then
    echo "Check failed: $label" >&2
    echo "File: $file" >&2
    exit 1
  fi
}

PURGE_SRC="src/main/java/com/ecoamazonas/eco_agua/platform/control/purge"
PURGE_TPL="src/main/resources/templates/control_center/purge"

assert_file "$PURGE_SRC/Matrix26PurgeController.java"
assert_file "$PURGE_SRC/Matrix26PurgeService.java"
assert_file "$PURGE_SRC/Matrix26PurgeRepository.java"
assert_file "$PURGE_SRC/Matrix26PurgeInitializer.java"
assert_file "$PURGE_TPL/detail.html"
assert_file scripts/configure-matrix26-purge-phase3h2.sh

assert_contains "$PURGE_SRC/Matrix26PurgeStatus.java" 'READY_TO_PURGE' 'Ready-to-purge state exists'
assert_contains "$PURGE_SRC/Matrix26PurgeStatus.java" 'PARTIALLY_PURGED' 'Partial purge state exists'
assert_contains "$PURGE_SRC/Matrix26PurgeStatus.java" 'PURGED' 'Purged state exists'
assert_contains "$PURGE_SRC/Matrix26PurgeController.java" 'prepare-execution' 'Prepare execution route exists'
assert_contains "$PURGE_SRC/Matrix26PurgeController.java" '/execute' 'Execute route exists'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'PREPARE PURGE EXECUTION' 'First operational confirmation is required'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'PURGE INSTANCE' 'Purge instance confirmation is required'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'DROP ARCHIVED DATABASE' 'Database confirmation is required'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'dropSchema' 'Database drop is centralized'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'deleteDirectory' 'Directory deletion is centralized'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'isSafeChild' 'Directory deletion is root-bound'
assert_contains "$PURGE_SRC/Matrix26PurgeService.java" 'Final archive, backups, archive records, decommission records, purge records, and clone restores were preserved' 'Preservation guarantee is logged'
assert_contains "$PURGE_SRC/Matrix26PurgeRepository.java" 'DROP DATABASE' 'Database drop exists only in repository'
assert_contains "$PURGE_SRC/Matrix26PurgeRepository.java" 'markInstancePurged' 'Instance is marked PURGED after execution'
assert_contains "$PURGE_SRC/Matrix26PurgeInitializer.java" 'execution_status' 'Execution status columns are initialized'
assert_contains "$PURGE_TPL/detail.html" 'READY_TO_PURGE' 'Template shows armed purge state'
assert_contains "$PURGE_TPL/detail.html" 'Execute operational purge' 'Template exposes execution form'
assert_contains scripts/configure-matrix26-purge-phase3h2.sh 'matrix26-archived-restore-test' 'Archive clone is protected in configuration'

if grep -RniE 'DROP DATABASE|DROP SCHEMA|Files\.delete|deleteRecursively|deleteIfExists|removeDirectory' \
  src/main/java/com/ecoamazonas/eco_agua/platform/control \
  | grep -v 'src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeRepository.java' \
  | grep -v 'src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java' \
  | grep -v 'Matrix26Backup' \
  | grep -v 'Matrix26FullBackupAssembler' \
  | grep -v 'Matrix26RuntimeControlService' \
  | grep -v 'Matrix26BrandingService' \
  | grep -v 'Matrix26ProvisioningAppearanceService' \
  | grep -v 'Matrix26Restore' \
  | grep -v 'Matrix26InPlaceRestore'; then
  echo "Unexpected destructive operation outside known controlled services." >&2
  exit 1
fi

echo "Operational purge states and routes: OK"
echo "Triple confirmation flow: OK"
echo "Final archive and clone preservation gates: OK"
echo "Database drop and directory deletion are centralized and guarded: OK"
echo "Execution evidence columns and UI: OK"
echo "Matrix26 Purge Manager Phase 3H.2 static checks passed."
