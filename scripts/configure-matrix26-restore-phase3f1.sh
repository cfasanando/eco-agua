#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/matrix26_control/application.properties"
MAINTENANCE_DIR="$PROJECT_ROOT/runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$MAINTENANCE_DIR/application.properties.phase3f1.bak"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Matrix26 runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$MAINTENANCE_DIR"
cp -f "$CONFIG_FILE" "$BACKUP_FILE"

TEMP_FILE="$(mktemp)"
grep -v '^matrix26\.control-center\.restores\.' "$CONFIG_FILE" > "$TEMP_FILE" || true
cat >> "$TEMP_FILE" <<'PROPERTIES'

# Matrix26 Restore Manager Phase 3F.1
matrix26.control-center.restores.enabled=true
matrix26.control-center.restores.allowed-source-instance-codes=matrix26-appearance-lab
matrix26.control-center.restores.target-instance-code=matrix26-restore-test
matrix26.control-center.restores.target-instance-name=Matrix26 Restore Test
matrix26.control-center.restores.target-database-name=matrix26_restore_test
matrix26.control-center.restores.target-runtime-profile=matrix26_restore_test
matrix26.control-center.restores.target-runtime-port=8095
matrix26.control-center.restores.target-public-url=http://localhost:8095
matrix26.control-center.restores.runtime-directory=runtime-clients
matrix26.control-center.restores.runtime-data-directory=runtime-data
matrix26.control-center.restores.import-executable=${MATRIX26_MYSQL_PATH:C:/wamp64/bin/mariadb/mariadb10.10.2/bin/mariadb.exe}
matrix26.control-center.restores.process-timeout-seconds=1200
matrix26.control-center.restores.start-after-restore-default=true
PROPERTIES

mv "$TEMP_FILE" "$CONFIG_FILE"

echo "Restore Manager configuration updated:"
echo "$CONFIG_FILE"
echo "Previous configuration preserved at:"
echo "$BACKUP_FILE"
