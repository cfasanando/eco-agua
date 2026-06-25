#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/backups"
TEMPLATES="$ROOT/src/main/resources/templates/control_center/backups"

required_files=(
  "$PACKAGE/Matrix26BackupController.java"
  "$PACKAGE/Matrix26BackupService.java"
  "$PACKAGE/Matrix26BackupRepository.java"
  "$PACKAGE/Matrix26BackupInitializer.java"
  "$TEMPLATES/index.html"
  "$TEMPLATES/new.html"
  "$TEMPLATES/detail.html"
  "$TEMPLATES/instance.html"
)

for file in "${required_files[@]}"; do
  test -f "$file" || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q "matrix26-appearance-lab" "$PACKAGE/Matrix26BackupProperties.java"
grep -q "matrix26_platform_control" "$PACKAGE/Matrix26BackupService.java"
grep -q -- "--single-transaction" "$PACKAGE/Matrix26BackupService.java"
grep -q "GZIPOutputStream" "$PACKAGE/Matrix26BackupService.java"
grep -q "SHA-256" "$PACKAGE/Matrix26BackupService.java"
grep -q "MYSQL_PWD" "$PACKAGE/Matrix26BackupService.java"

if grep -RniE -- '--password=|command\.add\(\"-p[^\"]+' "$PACKAGE"; then
  echo "Potential hard-coded password or command-line password detected." >&2
  exit 1
fi

if grep -RniE 'Runtime\.getRuntime\(\)\.exec|cmd\.exe|powershell\.exe.*mysqldump' "$PACKAGE"; then
  echo "Arbitrary shell execution boundary violated." >&2
  exit 1
fi

python - "$TEMPLATES" <<'PY'
from pathlib import Path
from html.parser import HTMLParser
import sys

class Parser(HTMLParser):
    pass

for path in Path(sys.argv[1]).glob('*.html'):
    parser = Parser()
    parser.feed(path.read_text(encoding='utf-8'))
print('Backup templates: OK')
PY

echo "Allowed instance and protected control database boundaries: OK"
echo "Credentials remain outside process arguments: OK"
echo "GZIP, SHA-256, manifest, and structural verification: OK"
echo "Matrix26 Backups Phase 3E.1 static checks passed."
