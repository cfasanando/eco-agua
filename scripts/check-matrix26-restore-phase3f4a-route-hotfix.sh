#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESTORE_CONTROLLER="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores/Matrix26RestoreController.java"
INPLACE_CONTROLLER="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores/Matrix26InPlaceRestoreController.java"

for file in "$RESTORE_CONTROLLER" "$INPLACE_CONTROLLER"; do
  test -f "$file"
done

grep -Fq '@RequestMapping("/control-center/restores/in-place")' "$INPLACE_CONTROLLER"
grep -Fq '@GetMapping("/{id:\\d+}")' "$RESTORE_CONTROLLER"
grep -Fq '@GetMapping("/{id:\\d+}")' "$INPLACE_CONTROLLER"
grep -Fq '@GetMapping("/validations/{runId:\\d+}/report")' "$RESTORE_CONTROLLER"

if grep -Eq '@(Get|Post|Put|Delete|Patch)Mapping\("/\{id\}([/\"]|$)' "$RESTORE_CONTROLLER" "$INPLACE_CONTROLLER"; then
  echo "ERROR: Unrestricted restore id route remains."
  exit 1
fi

if grep -Eq '\{planId\}' "$RESTORE_CONTROLLER"; then
  echo "ERROR: Unrestricted cleanup plan id route remains."
  exit 1
fi

echo "Static /in-place route remains explicit: OK"
echo "Restore detail routes accept numeric IDs only: OK"
echo "Validation and cleanup identifiers accept numeric IDs only: OK"
echo "Matrix26 Restore Manager Phase 3F.4a route hotfix checks passed."
