#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESTORE_JAVA="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores"
BACKUP_SECURITY="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/backups/Matrix26BackupSecurityService.java"
SIDEBAR="$ROOT/src/main/resources/templates/control_center/fragments/sidebar.html"
TEMPLATES="$ROOT/src/main/resources/templates/control_center/restores"
OPERATIONS="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsProperties.java"

required_files=(
  "$RESTORE_JAVA/Matrix26RestoreController.java"
  "$RESTORE_JAVA/Matrix26RestoreService.java"
  "$RESTORE_JAVA/Matrix26RestoreRepository.java"
  "$RESTORE_JAVA/Matrix26RestoreInitializer.java"
  "$TEMPLATES/index.html"
  "$TEMPLATES/new.html"
  "$TEMPLATES/detail.html"
)
for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q 'extractVerifiedBackup' "$BACKUP_SECURITY"
grep -q 'AES-GCM authentication failed' "$BACKUP_SECURITY"
echo "Encrypted package extraction and internal verification: OK"

grep -q 'matrix26-restore-test' "$RESTORE_JAVA/Matrix26RestoreProperties.java"
grep -q 'matrix26_restore_test' "$RESTORE_JAVA/Matrix26RestoreProperties.java"
grep -q '8095' "$RESTORE_JAVA/Matrix26RestoreProperties.java"
grep -q 'validateTargetAvailable' "$RESTORE_JAVA/Matrix26RestoreService.java"
if grep -RqiE 'DROP[[:space:]]+DATABASE|TRUNCATE[[:space:]]+matrix26_restore|REPLACE_EXISTING.*runtime-clients' "$RESTORE_JAVA"; then
  echo "Unsafe overwrite or database drop operation detected." >&2
  exit 1
fi
echo "Fixed isolated target and no-overwrite boundary: OK"

grep -q 'MYSQL_PWD' "$RESTORE_JAVA/Matrix26RestoreService.java"
if grep -q -- '--password=' "$RESTORE_JAVA/Matrix26RestoreService.java"; then
  echo "Password detected in database client arguments." >&2
  exit 1
fi
echo "Database credentials remain outside process arguments: OK"

grep -q 'safeZipEntry' "$RESTORE_JAVA/Matrix26RestoreService.java"
grep -q 'ensureInside' "$RESTORE_JAVA/Matrix26RestoreService.java"
grep -q 'matrix26_instance_appearance_config' "$RESTORE_JAVA/Matrix26RestoreService.java"
echo "Safe archive extraction and clone identity remapping: OK"

grep -q 'matrix26-restore-test' "$OPERATIONS"
grep -q '/control-center/restores' "$SIDEBAR"
echo "Runtime Control integration and navigation: OK"

python - "$TEMPLATES" <<'PY'
from pathlib import Path
import sys
for path in Path(sys.argv[1]).glob('*.html'):
    text = path.read_text(encoding='utf-8')
    if text.count('<html') != 1 or text.count('</html>') != 1:
        raise SystemExit(f'Invalid HTML document structure: {path}')
    if text.count('<form') != text.count('</form>'):
        raise SystemExit(f'Unbalanced form tags: {path}')
print('Restore templates: OK')
PY

echo "Matrix26 Restore Manager Phase 3F.1 static checks passed."
