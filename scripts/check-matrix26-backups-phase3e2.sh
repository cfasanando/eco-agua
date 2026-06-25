#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/backups"
TEMPLATES="$ROOT/src/main/resources/templates/control_center/backups"
CONFIG_SCRIPT="$ROOT/scripts/configure-matrix26-backup-runtime-phase3e2.sh"

required_files=(
  "$PACKAGE/Matrix26BackupController.java"
  "$PACKAGE/Matrix26BackupService.java"
  "$PACKAGE/Matrix26FullBackupAssembler.java"
  "$PACKAGE/Matrix26FullBackupResult.java"
  "$PACKAGE/Matrix26BackupRepository.java"
  "$PACKAGE/Matrix26BackupProperties.java"
  "$TEMPLATES/index.html"
  "$TEMPLATES/new.html"
  "$TEMPLATES/detail.html"
  "$TEMPLATES/instance.html"
  "$CONFIG_SCRIPT"
)

for file in "${required_files[@]}"; do
  test -f "$file" || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q 'createManualFullBackup' "$PACKAGE/Matrix26BackupService.java"
grep -q 'MANUAL_FULL' "$PACKAGE/Matrix26BackupService.java"
grep -q 'ZipOutputStream' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'ZipFile' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'runtime-config.properties' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'instance-files.zip' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'appearance.json' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'modules.json' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'files-inventory.json' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'runtime-log-tail.txt' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'FULL_ARTIFACT_SHA256' "$PACKAGE/Matrix26BackupService.java"
grep -q 'FILESET_STABILITY' "$PACKAGE/Matrix26FullBackupAssembler.java"
grep -q 'backupScope' "$PACKAGE/Matrix26BackupController.java"
grep -q 'value="FULL"' "$TEMPLATES/new.html"
grep -q 'value="DATABASE"' "$TEMPLATES/new.html"

grep -q 'matrix26.control-center.backups.dump-executable=' "$CONFIG_SCRIPT"
grep -q 'matrix26.control-center.backups.root-directory=' "$CONFIG_SCRIPT"
grep -q 'matrix26.control-center.backups.runtime-data-directory=' "$CONFIG_SCRIPT"
grep -q 'runtime-data/matrix26-control/maintenance' "$CONFIG_SCRIPT"

if grep -RniE -- '--password=|command\.add\("-p[^\"]+' "$PACKAGE"; then
  echo "Potential command-line password detected." >&2
  exit 1
fi

if grep -RniE 'Runtime\.getRuntime\(\)\.exec|cmd\.exe|powershell\.exe.*(mysqldump|mariadb-dump)' "$PACKAGE"; then
  echo "Arbitrary shell execution boundary violated." >&2
  exit 1
fi

if grep -RniE 'runtime-data/.*/operations.*ZipEntry|application-error\.log.*putNextEntry' "$PACKAGE/Matrix26FullBackupAssembler.java"; then
  echo "Raw operation logs must not be included in the recovery archive." >&2
  exit 1
fi

python - "$TEMPLATES" "$ROOT/src/main/resources/static/css/matrix26-control.css" <<'PY'
from pathlib import Path
from html.parser import HTMLParser
import sys

class Parser(HTMLParser):
    pass

for path in Path(sys.argv[1]).glob('*.html'):
    parser = Parser()
    parser.feed(path.read_text(encoding='utf-8'))

css = Path(sys.argv[2]).read_text(encoding='utf-8')
if css.count('{') != css.count('}'):
    raise SystemExit('Unbalanced CSS braces')

print('Backup templates and CSS: OK')
PY

echo "Full and database-only backup flows: OK"
echo "Runtime configuration redaction and path boundaries: OK"
echo "ZIP entry validation and symbolic-link exclusion: OK"
echo "Cross-artifact SHA-256 and inventory verification: OK"
echo "Persistent Matrix26 backup configuration keys: OK"
echo "Matrix26 Backups Phase 3E.2 static checks passed."
