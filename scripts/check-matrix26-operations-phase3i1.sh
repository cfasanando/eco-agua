#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboardService.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboard.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboardMetric.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboardAlert.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboardActivity.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboardInstance.java"
  "src/main/resources/templates/control_center/operations/dashboard.html"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

if ! grep -R "@GetMapping(\"/dashboard\")" -n src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsController.java >/dev/null; then
  echo "Operations dashboard route was not registered." >&2
  exit 1
fi

if ! grep -R "operationsDashboard" -n src/main/resources/templates/control_center/operations/dashboard.html >/dev/null; then
  echo "Dashboard template does not render the consolidated dashboard model." >&2
  exit 1
fi

if ! grep -R "matrix26_operations_dashboard" -n src/main/resources/templates/control_center/fragments/sidebar.html >/dev/null; then
  echo "Sidebar does not expose the operations dashboard entry." >&2
  exit 1
fi

if grep -RniE "DROP DATABASE|DROP SCHEMA|Files\.delete|deleteIfExists|deleteRecursively|DESTROY ARCHIVE PACKAGE|PURGE INSTANCE|DROP ARCHIVED DATABASE" \
    src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26OperationsDashboard*.java \
    src/main/resources/templates/control_center/operations/dashboard.html >/dev/null; then
  echo "Unexpected destructive operation detected in the read-only operations dashboard." >&2
  exit 1
fi

echo "Matrix26 Operations Phase 3I.1 static checks passed."
