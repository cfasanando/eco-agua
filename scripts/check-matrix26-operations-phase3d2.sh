#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CONTROL_DIR="src/main/java/com/ecoamazonas/eco_agua/platform/control/operations"
TEMPLATE_DIR="src/main/resources/templates/control_center/operations"

required_files=(
  "$CONTROL_DIR/Matrix26RuntimeControlService.java"
  "$CONTROL_DIR/Matrix26RuntimeControlInitializer.java"
  "$CONTROL_DIR/Matrix26RuntimeOperation.java"
  "$CONTROL_DIR/Matrix26RuntimeManagedState.java"
  "$CONTROL_DIR/Matrix26RuntimeOperationRepository.java"
  "$CONTROL_DIR/Matrix26RuntimeManagedStateRepository.java"
  "$TEMPLATE_DIR/runtime_detail.html"
  "$TEMPLATE_DIR/runtimes.html"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

grep -q 'matrix26-restaurant-lab' "$CONTROL_DIR/Matrix26OperationsProperties.java"
grep -q 'matrix26-appearance-lab' "$CONTROL_DIR/Matrix26OperationsProperties.java"
grep -q '@PostMapping("/runtimes/{runtimeKey}/start")' "$CONTROL_DIR/Matrix26OperationsController.java"
grep -q '@PostMapping("/runtimes/{runtimeKey}/stop")' "$CONTROL_DIR/Matrix26OperationsController.java"
grep -q '@PostMapping("/runtimes/{runtimeKey}/restart")' "$CONTROL_DIR/Matrix26OperationsController.java"
grep -q 'CREATE TABLE IF NOT EXISTS matrix26_runtime_state' "$CONTROL_DIR/Matrix26RuntimeControlInitializer.java"
grep -q 'CREATE TABLE IF NOT EXISTS matrix26_runtime_operation' "$CONTROL_DIR/Matrix26RuntimeControlInitializer.java"
grep -q 'ProcessBuilder' "$CONTROL_DIR/Matrix26RuntimeControlService.java"
grep -q 'ProcessHandle' "$CONTROL_DIR/Matrix26RuntimeControlService.java"
grep -q 'Force stop is not available' "$TEMPLATE_DIR/runtime_detail.html"

if grep -qE 'destroyForcibly|taskkill|Stop-Process|kill -9|Runtime\.getRuntime\(' \
  "$CONTROL_DIR/Matrix26RuntimeControlService.java"; then
  echo "Unsafe force-stop or shell execution detected in runtime control service." >&2
  exit 1
fi

if grep -qE '8081|8082|8084|matrix26-control' \
  "$CONTROL_DIR/Matrix26OperationsProperties.java"; then
  echo "A protected production runtime was added to the control allowlist." >&2
  exit 1
fi

if grep -RniE 'password=[^*]|token=[^*]|api[-_.]?key=[^*]' \
  "$CONTROL_DIR/Matrix26RuntimeControlService.java" \
  "$TEMPLATE_DIR" >/dev/null 2>&1; then
  echo "Potential plaintext secret detected in Phase 3D.2 files." >&2
  exit 1
fi

echo "Runtime allowlist and protected-instance boundaries: OK"
echo "Controlled POST endpoints and confirmation flow: OK"
echo "No force-stop or arbitrary shell execution in runtime control service: OK"
echo "Matrix26 Operations Phase 3D.2 static checks passed."
