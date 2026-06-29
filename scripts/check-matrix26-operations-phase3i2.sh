#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlert.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertController.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertEvent.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertInitializer.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertRepository.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertService.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertSeverity.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertSource.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertStatus.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertSummary.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertCenterView.java"
  "src/main/resources/templates/control_center/operations/alerts/index.html"
  "src/main/resources/templates/control_center/operations/alerts/detail.html"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

if ! grep -R "@RequestMapping(\"/control-center/operations/alerts\")" -n src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertController.java >/dev/null; then
  echo "Operation alert route was not registered." >&2
  exit 1
fi

if ! grep -R "CREATE TABLE IF NOT EXISTS matrix26_operation_alert" -n src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlertInitializer.java >/dev/null; then
  echo "Operation alert table initializer was not found." >&2
  exit 1
fi

if ! grep -R "matrix26_operation_alerts" -n src/main/resources/templates/control_center/fragments/sidebar.html >/dev/null; then
  echo "Sidebar does not expose the operation alert center entry." >&2
  exit 1
fi

if ! grep -R "Open Alert Center" -n src/main/resources/templates/control_center/operations/dashboard.html >/dev/null; then
  echo "Operations dashboard does not link to the alert center." >&2
  exit 1
fi

if grep -RniE "DROP DATABASE|DROP SCHEMA|Files\.delete|deleteIfExists|deleteRecursively|DESTROY ARCHIVE PACKAGE|PURGE INSTANCE|DROP ARCHIVED DATABASE" \
    src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationAlert*.java \
    src/main/resources/templates/control_center/operations/alerts >/dev/null; then
  echo "Unexpected destructive operation detected in the alert center." >&2
  exit 1
fi

echo "Matrix26 Operations Phase 3I.2 static checks passed."
