#!/usr/bin/env bash
set -euo pipefail

CONFIG_FILE="runtime-clients/matrix26_control/application.properties"
MAINTENANCE_DIR="runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$MAINTENANCE_DIR/application.properties.phase3h3.bak"

if [ ! -f "$CONFIG_FILE" ]; then
  echo "Matrix26 Control Center runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$MAINTENANCE_DIR"
cp "$CONFIG_FILE" "$BACKUP_FILE"

set_property() {
  local key="$1"
  local value="$2"
  if grep -qE "^[[:space:]]*${key//./\.}=" "$CONFIG_FILE"; then
    sed -i -E "s|^[[:space:]]*${key//./\.}=.*|${key}=${value}|" "$CONFIG_FILE"
  else
    printf '\n%s=%s\n' "$key" "$value" >> "$CONFIG_FILE"
  fi
}

set_property "matrix26.control-center.purge.enabled" "true"
set_property "matrix26.control-center.purge.execution-enabled" "true"
set_property "matrix26.control-center.purge.archive-destruction-enabled" "true"
set_property "matrix26.control-center.purge.archive-destruction-execution-enabled" "false"
set_property "matrix26.control-center.purge.archive-destruction-require-retention-expired" "true"
set_property "matrix26.control-center.purge.allowed-instance-codes" "matrix26-appearance-lab"
set_property "matrix26.control-center.purge.protected-instance-codes" "eco-agua,productos-selva-belen,restaurante-buen-sabor,matrix26-control-center,matrix26-archived-restore-test"
set_property "matrix26.control-center.purge.minimum-reason-length" "10"
set_property "matrix26.control-center.purge.backup-root-directory" "C:/Users/PC/Matrix26/backups"

echo "Matrix26 Purge Manager Phase 3H.3 archive destruction planner configuration updated."
echo "Backup saved at: $BACKUP_FILE"
