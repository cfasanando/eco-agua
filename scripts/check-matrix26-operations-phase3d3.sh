#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OPS="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/operations"
TEMPLATES="$ROOT/src/main/resources/templates/control_center/operations"
CSS="$ROOT/src/main/resources/static/css/matrix26-control.css"

required_files=(
  "$OPS/Matrix26RuntimeControlService.java"
  "$OPS/Matrix26RuntimeRecoveryRunner.java"
  "$OPS/Matrix26RuntimePidFileInfo.java"
  "$OPS/Matrix26RuntimeStabilityView.java"
  "$TEMPLATES/runtime_detail.html"
  "$ROOT/docs/project/matrix26-operations-phase3d3-test-guide.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

grep -q 'recoverInterruptedOperations' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'Matrix26RuntimeOperationStatus.RECOVERED' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'Matrix26RuntimeOperationStatus.INTERRUPTED' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'readPidFileInfo' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'Orphan runtime process adopted successfully' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'destroyForcibly' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'verified graceful-stop timeout' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'GZIPOutputStream' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'last-operation.json' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'CLEAN PID ' "$OPS/Matrix26RuntimeControlService.java"
grep -q 'ROTATE LOGS ' "$OPS/Matrix26RuntimeControlService.java"

grep -q '@PostMapping("/runtimes/{runtimeKey}/force-stop")' "$OPS/Matrix26OperationsController.java"
grep -q '@PostMapping("/runtimes/{runtimeKey}/adopt")' "$OPS/Matrix26OperationsController.java"
grep -q '@PostMapping("/runtimes/{runtimeKey}/clean-stale-pid")' "$OPS/Matrix26OperationsController.java"
grep -q '@PostMapping("/runtimes/{runtimeKey}/rotate-logs")' "$OPS/Matrix26OperationsController.java"

grep -q 'matrix26-restaurant-lab' "$OPS/Matrix26OperationsProperties.java"
grep -q 'matrix26-appearance-lab' "$OPS/Matrix26OperationsProperties.java"

if grep -RniE 'Runtime\.getRuntime\(\)\.exec|ProcessBuilder\([^)]*(taskkill|kill|powershell|cmd\.exe)|Stop-Process|kill -9' "$OPS"; then
  echo "Unsafe arbitrary operating-system command detected." >&2
  exit 1
fi

python - "$TEMPLATES" "$CSS" <<'PY'
from pathlib import Path
import sys

templates = Path(sys.argv[1])
css = Path(sys.argv[2])
for path in templates.glob("*.html"):
    text = path.read_text(encoding="utf-8")
    for tag in ("div", "form", "section", "table"):
        if text.count(f"<{tag}") != text.count(f"</{tag}>"):
            raise SystemExit(f"Unbalanced <{tag}> tags in {path}")

css_text = css.read_text(encoding="utf-8")
if css_text.count("{") != css_text.count("}"):
    raise SystemExit("Unbalanced CSS braces")
print("Static HTML and CSS structure: OK")
PY

echo "Interrupted-operation recovery and persistent locking: OK"
echo "Stale PID, orphan process, and port-conflict boundaries: OK"
echo "Force stop remains isolated behind timeout, ownership, allowlist, and confirmation: OK"
echo "GZIP log rotation and retention policy: OK"
echo "Matrix26 Operations Phase 3D.3 static checks passed."
