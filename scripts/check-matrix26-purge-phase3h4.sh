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
ARCHIVE_TPL="src/main/resources/templates/control_center/purge/archive-destruction"

assert_file "$PURGE_SRC/Matrix26ArchiveDestructionController.java"
assert_file "$PURGE_SRC/Matrix26ArchiveDestructionService.java"
assert_file "$PURGE_SRC/Matrix26ArchiveDestructionRepository.java"
assert_file "$PURGE_SRC/Matrix26HistoricalArchiveExecutor.java"
assert_file "$PURGE_SRC/Matrix26ArchiveDestructionStatus.java"
assert_file "$ARCHIVE_TPL/detail.html"
assert_file scripts/configure-matrix26-purge-phase3h4.sh

assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'APPROVE ARCHIVE DESTRUCTION' 'Approval confirmation exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'DESTROY ARCHIVE PACKAGE' 'Destroy confirmation exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'I UNDERSTAND THIS ARCHIVE CANNOT BE RESTORED' 'Irreversible confirmation exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'archive-destruction-execution-enabled=true' 'Execution gate message exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'Central archive, backup, purge and decommission metadata were preserved' 'Central metadata preservation is logged'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'NO_CLONE_DEPENDENCY' 'Clone dependency gate remains active'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'RETENTION_EXPIRED' 'Retention gate remains active'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionRepository.java" 'PACKAGE_DESTROYED' 'Archive status is updated after successful package destruction'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionRepository.java" 'execution_status' 'Item execution evidence is stored'
assert_contains "$PURGE_SRC/Matrix26HistoricalArchiveExecutor.java" 'Files\.delete' 'Physical file deletion is isolated in the executor'
assert_contains "$PURGE_SRC/Matrix26HistoricalArchiveExecutor.java" 'Refusing to destroy a path outside the configured backup root' 'Backup root safety boundary exists'
assert_contains "$PURGE_SRC/Matrix26HistoricalArchiveExecutor.java" 'Refusing to destroy unexpected archive file' 'Unexpected files are blocked'
assert_contains "$PURGE_SRC/Matrix26PurgeInitializer.java" 'idx_matrix26_archive_destruction_item_execution' 'Archive destruction execution index exists'
assert_contains "$PURGE_SRC/Matrix26PurgeProperties.java" 'archiveDestructionExecutionEnabled = false' 'Archive destruction execution remains disabled by default'
assert_contains "$ARCHIVE_TPL/detail.html" 'APPROVE ARCHIVE DESTRUCTION' 'UI exposes approval confirmation'
assert_contains "$ARCHIVE_TPL/detail.html" 'DESTROY ARCHIVE PACKAGE' 'UI exposes destruction confirmation'
assert_contains "$ARCHIVE_TPL/detail.html" 'I UNDERSTAND THIS ARCHIVE CANNOT BE RESTORED' 'UI exposes irreversible confirmation'
assert_contains scripts/configure-matrix26-purge-phase3h4.sh 'archive-destruction-execution-enabled" "false' 'Configuration keeps execution disabled by default'

if grep -RniE 'DROP DATABASE|DROP SCHEMA' "$PURGE_SRC/Matrix26ArchiveDestruction"*.java "$PURGE_SRC/Matrix26HistoricalArchiveExecutor.java"; then
  echo "Archive destruction executor must not drop databases." >&2
  exit 1
fi

if grep -RniE 'Files\.delete|deleteRecursively|deleteIfExists|removeDirectory' "$PURGE_SRC/Matrix26ArchiveDestruction"*.java; then
  echo "Physical file deletion must stay isolated outside Matrix26ArchiveDestruction*.java." >&2
  exit 1
fi

if grep -RniE 'Files\.delete|deleteRecursively|deleteIfExists|removeDirectory' "$PURGE_SRC"/*.java | grep -v 'Matrix26HistoricalArchiveExecutor.java' | grep -v 'Matrix26PurgeService.java'; then
  echo "Unexpected physical deletion outside the approved executors." >&2
  exit 1
fi

echo "Archive destruction execution routes: OK"
echo "Three explicit confirmations: OK"
echo "Execution disabled by default: OK"
echo "Retention, clone and final backup gates: OK"
echo "Physical deletion isolated in Matrix26HistoricalArchiveExecutor: OK"
echo "Central metadata preservation: OK"
echo "Matrix26 Purge Manager Phase 3H.4 static checks passed."
