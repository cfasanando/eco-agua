#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance/Matrix26AcceptanceMatrixController.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance/Matrix26AcceptanceMatrixService.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance/Matrix26AcceptanceMatrix.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance/Matrix26AcceptanceGroup.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance/Matrix26AcceptanceItem.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance/Matrix26AcceptanceStatus.java"
  "src/main/resources/templates/control_center/operations/acceptance/index.html"
)

for file in "${required_files[@]}"; do
  if [ ! -f "$file" ]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

grep -R "control-center/operations/acceptance" src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance src/main/resources/templates/control_center >/dev/null
grep -R "Final Acceptance Matrix" src/main/resources/templates/control_center/operations/acceptance >/dev/null
grep -R "Archive destruction execution disabled" src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance >/dev/null
grep -R "Runtime data remains outside Git" src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance >/dev/null

if grep -RniE "@PostMapping|DROP DATABASE|DROP SCHEMA|Files\.delete|deleteRecursively|deleteIfExists|package\.m26backup|database\.sql\.gz|instance-files\.zip" \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/acceptance \
  src/main/resources/templates/control_center/operations/acceptance; then
  echo "Unexpected write/destructive operation found in Phase 3I.4 acceptance files." >&2
  exit 1
fi

echo "Matrix26 Operations Phase 3I.4 static checks passed."
