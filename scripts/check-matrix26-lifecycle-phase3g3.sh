#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARCHIVE_DIR="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/lifecycle/archive"
DECOMMISSION_REPO="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/lifecycle/decommission/Matrix26DecommissionRepository.java"
RESTORE_SERVICE="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores/Matrix26RestoreService.java"
TEMPLATE_DIR="$PROJECT_ROOT/src/main/resources/templates/control_center/lifecycle/archive"
SIDEBAR="$PROJECT_ROOT/src/main/resources/templates/control_center/fragments/sidebar.html"
CONFIG_SCRIPT="$PROJECT_ROOT/scripts/configure-matrix26-lifecycle-phase3g3.sh"

required_java=(
  Matrix26ArchiveController.java
  Matrix26ArchiveEvent.java
  Matrix26ArchiveException.java
  Matrix26ArchiveInitializer.java
  Matrix26ArchiveProperties.java
  Matrix26ArchiveRecord.java
  Matrix26ArchiveRepository.java
  Matrix26ArchiveRestoreLink.java
  Matrix26ArchiveService.java
  Matrix26ArchiveSummary.java
)
for file in "${required_java[@]}"; do
  test -f "$ARCHIVE_DIR/$file"
done
echo "Persistent archive records, events, and clone restore links: OK"

grep -q 'matrix26_archive_record' "$ARCHIVE_DIR/Matrix26ArchiveInitializer.java"
grep -q 'matrix26_archive_event' "$ARCHIVE_DIR/Matrix26ArchiveInitializer.java"
grep -q 'matrix26_archive_restore_link' "$ARCHIVE_DIR/Matrix26ArchiveInitializer.java"
echo "Archive metadata tables: OK"

grep -q 'DECOMMISSIONED' "$ARCHIVE_DIR/Matrix26ArchiveService.java"
grep -q 'protectedFlag' "$ARCHIVE_DIR/Matrix26ArchiveService.java"
grep -q 'verifyEncryptedBackup' "$ARCHIVE_DIR/Matrix26ArchiveService.java"
grep -q 'RESTORE ARCHIVE ' "$ARCHIVE_DIR/Matrix26ArchiveService.java"
echo "Final archive verification and original decommission protection: OK"

grep -q 'restoreArchivedClone' "$RESTORE_SERVICE"
grep -q 'validateTargetAvailable(job)' "$RESTORE_SERVICE"
grep -q 'matrix26-archived-restore-test' "$ARCHIVE_DIR/Matrix26ArchiveProperties.java"
grep -q '8096' "$ARCHIVE_DIR/Matrix26ArchiveProperties.java"
echo "Archived final backup clone restore target: OK"

if grep -RniE 'DROP[[:space:]]+(DATABASE|SCHEMA)|Files\.delete|deleteIfExists|deleteRecursively|removeDirectory|DELETE[[:space:]]+FROM[[:space:]]+platform_business_client|purge' "$ARCHIVE_DIR"; then
  echo "Unexpected destructive operation detected in Phase 3G.3 archive code." >&2
  exit 1
fi
echo "Archive Manager remains non-destructive: OK"

if grep -q 'WHERE instance_id = ?[[:space:]]*$' "$DECOMMISSION_REPO" && grep -q 'INSERT IGNORE INTO matrix26_decommission_schedule_state' "$DECOMMISSION_REPO"; then
  true
fi
if grep -nA8 'snapshotSchedules' "$DECOMMISSION_REPO" | grep -c 'WHERE instance_id = ?' | grep -qv '^1$'; then
  echo "Unexpected duplicate WHERE clause in decommission schedule snapshot." >&2
  exit 1
fi
echo "Decommission schedule snapshot SQL remains valid: OK"

for file in index.html detail.html restores.html; do
  test -f "$TEMPLATE_DIR/$file"
done
grep -q '/control-center/lifecycle/archive' "$SIDEBAR"
grep -q 'matrix26_archive' "$SIDEBAR"
echo "Archive templates and navigation: OK"

grep -q 'application.properties.phase3g3.bak' "$CONFIG_SCRIPT"
grep -q 'matrix26.control-center.lifecycle.archive.enabled=true' "$CONFIG_SCRIPT"
grep -q 'clone-runtime-port=8096' "$CONFIG_SCRIPT"
if grep -Ei 'MATRIX26_BACKUP_MASTER_KEY[[:space:]]*=|spring\.datasource\.password[[:space:]]*=' "$CONFIG_SCRIPT"; then
  echo "Configuration script must not write secrets." >&2
  exit 1
fi
echo "Configuration preserves credentials and enables only archive clone settings: OK"

echo "Matrix26 Lifecycle Manager Phase 3G.3 static checks passed."
