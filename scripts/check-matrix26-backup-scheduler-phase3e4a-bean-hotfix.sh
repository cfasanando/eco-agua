#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$ROOT_DIR/src/main/java/com/ecoamazonas/eco_agua/platform/control/backups"
RUNTIME_CONFIG="$ROOT_DIR/runtime-clients/matrix26_control/application.properties"

SERVICE="$BACKUP_DIR/Matrix26BackupScheduleService.java"
SCHEDULER="$BACKUP_DIR/Matrix26BackupScheduler.java"
CONFIGURATION="$BACKUP_DIR/Matrix26BackupSchedulingConfiguration.java"

for file in "$SERVICE" "$SCHEDULER" "$CONFIGURATION"; do
    test -f "$file" || { echo "Missing file: $file" >&2; exit 1; }
done

grep -q '@Service' "$SERVICE"
grep -q 'class Matrix26BackupScheduleService' "$SERVICE"
echo "Backup schedule service bean annotation: OK"

for file in "$SCHEDULER" "$CONFIGURATION"; do
    grep -q 'prefix = "matrix26.control-center"' "$file"
    grep -q 'name = {"enabled", "backups.scheduling-enabled"}' "$file"
    grep -q 'havingValue = "true"' "$file"
    if grep -q 'matchIfMissing = true' "$file"; then
        echo "Unsafe matchIfMissing remains in $file" >&2
        exit 1
    fi
done
echo "Scheduler and scheduling configuration conditions are aligned: OK"

if [[ -f "$RUNTIME_CONFIG" ]]; then
    grep -q '^matrix26\.control-center\.enabled=true$' "$RUNTIME_CONFIG" \
        || { echo "Missing matrix26.control-center.enabled=true in runtime configuration" >&2; exit 1; }
    grep -q '^matrix26\.control-center\.backups\.scheduling-enabled=true$' "$RUNTIME_CONFIG" \
        || { echo "Missing scheduling-enabled=true in runtime configuration" >&2; exit 1; }
    echo "Matrix26 runtime properties: OK"
else
    echo "Runtime configuration was not found; source checks passed only."
fi

echo "Matrix26 Backup Scheduler Phase 3E.4a bean hotfix checks passed."
