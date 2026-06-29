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
assert_file "$PURGE_SRC/Matrix26ArchiveDestructionPlan.java"
assert_file "$PURGE_SRC/Matrix26ArchiveDestructionItem.java"
assert_file "$PURGE_SRC/Matrix26ArchiveDestructionStatus.java"
assert_file "$ARCHIVE_TPL/index.html"
assert_file "$ARCHIVE_TPL/new.html"
assert_file "$ARCHIVE_TPL/detail.html"
assert_file scripts/configure-matrix26-purge-phase3h3.sh

assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionController.java" 'archive-destruction' 'Archive destruction routes exist'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'PREPARE ARCHIVE DESTRUCTION' 'Planner confirmation is required'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'Deleted resources: 0' 'No-deletion guarantee is logged'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'Phase 3H\.3 must not expose operational file removal' 'Execution-disabled gate exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'NO_CLONE_DEPENDENCY' 'Clone dependency gate exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionService.java" 'RETENTION_EXPIRED' 'Retention gate exists'
assert_contains "$PURGE_SRC/Matrix26ArchiveDestructionRepository.java" 'matrix26_archive_destruction_plan' 'Archive destruction plan table is used'
assert_contains "$PURGE_SRC/Matrix26PurgeInitializer.java" 'matrix26_archive_destruction_plan' 'Archive destruction tables are initialized'
assert_contains "$PURGE_SRC/Matrix26PurgeProperties.java" 'archiveDestructionExecutionEnabled = false' 'Archive destruction execution is disabled by default'
assert_contains "$ARCHIVE_TPL/detail.html" 'Phase 3H\.3 is read-only' 'Detail template states read-only behavior'
assert_contains "$ARCHIVE_TPL/new.html" 'PREPARE ARCHIVE DESTRUCTION matrix26-appearance-lab' 'UI shows exact confirmation'
assert_contains "src/main/resources/templates/control_center/fragments/sidebar.html" 'Archive destruction' 'Sidebar link exists'
assert_contains scripts/configure-matrix26-purge-phase3h3.sh 'archive-destruction-execution-enabled" "false' 'Configuration keeps execution disabled'

if grep -RniE 'DROP DATABASE|DROP SCHEMA|Files\.delete|deleteRecursively|deleteIfExists|removeDirectory' \
  "$PURGE_SRC/Matrix26ArchiveDestruction"*.java; then
  echo "Unexpected destructive operation in archive destruction planner." >&2
  exit 1
fi

if grep -RniE 'package\.m26backup.*(remove|delete|drop)|remove.*package\.m26backup|delete.*package\.m26backup' \
  "$PURGE_SRC/Matrix26ArchiveDestruction"*.java; then
  echo "Archive destruction planner must not implement package removal." >&2
  exit 1
fi

echo "Archive destruction planner routes: OK"
echo "Read-only confirmation flow: OK"
echo "Retention, clone and final backup gates: OK"
echo "Archive destruction tables and sidebar link: OK"
echo "No destructive operation exists in Phase 3H.3 planner: OK"
echo "Matrix26 Purge Manager Phase 3H.3 static checks passed."
