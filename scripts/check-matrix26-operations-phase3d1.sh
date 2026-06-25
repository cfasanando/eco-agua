#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

OPERATIONS_JAVA="src/main/java/com/ecoamazonas/eco_agua/platform/control/operations"
OPERATIONS_TEMPLATES="src/main/resources/templates/control_center/operations"

required_files=(
  "$OPERATIONS_JAVA/Matrix26OperationsController.java"
  "$OPERATIONS_JAVA/Matrix26OperationsInventoryService.java"
  "$OPERATIONS_JAVA/Matrix26SystemProbe.java"
  "$OPERATIONS_TEMPLATES/dashboard.html"
  "$OPERATIONS_TEMPLATES/runtimes.html"
  "$OPERATIONS_TEMPLATES/runtime_detail.html"
  "$OPERATIONS_TEMPLATES/ports.html"
  "$OPERATIONS_TEMPLATES/logs.html"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

if grep -RniE '@PostMapping|@PutMapping|@DeleteMapping|taskkill|Stop-Process|kill -9|Runtime\.getRuntime\(\)\.exec' \
  "$OPERATIONS_JAVA"; then
  echo "Unsafe runtime write marker found in Phase 3D.1." >&2
  exit 1
fi

for route in \
  '/control-center/operations' \
  '/runtimes' \
  '/ports' \
  '/logs'; do
  if ! grep -Rq --fixed-strings "$route" \
      "$OPERATIONS_JAVA" \
      "$OPERATIONS_TEMPLATES" \
      src/main/resources/templates/control_center/fragments/sidebar.html; then
    echo "Missing route reference: $route" >&2
    exit 1
  fi
done

python - <<'PY'
from pathlib import Path

root = Path("src/main/resources/templates/control_center/operations")
for path in sorted(root.glob("*.html")):
    text = path.read_text(encoding="utf-8")
    if "<html" not in text or "</html>" not in text:
        raise SystemExit(f"Invalid HTML document: {path}")
    if text.count('th:') and text.count('"') % 2:
        raise SystemExit(f"Unbalanced quotes in: {path}")

css = Path("src/main/resources/static/css/matrix26-control.css").read_text(encoding="utf-8")
if css.count("{") != css.count("}"):
    raise SystemExit("CSS braces are unbalanced")

java_root = Path("src/main/java/com/ecoamazonas/eco_agua/platform/control/operations")
for path in java_root.glob("*.java"):
    text = path.read_text(encoding="utf-8")
    if text.count("{") != text.count("}"):
        raise SystemExit(f"Java braces are unbalanced: {path}")

print("Static HTML, CSS, and Java structure: OK")
PY

if command -v mvn >/dev/null 2>&1; then
  mvn -DskipTests compile
else
  echo "Maven was not found; compile check skipped by this script." >&2
fi

echo "Matrix26 Operations Phase 3D.1 static checks passed."
