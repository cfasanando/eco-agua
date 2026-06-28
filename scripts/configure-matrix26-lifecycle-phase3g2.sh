#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/matrix26_control/application.properties"
MAINTENANCE_DIR="$PROJECT_ROOT/runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$MAINTENANCE_DIR/application.properties.phase3g2.bak"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Matrix26 runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$MAINTENANCE_DIR"
cp -f "$CONFIG_FILE" "$BACKUP_FILE"

TEMP_FILE="$(mktemp)"
grep -vE '^matrix26\.control-center\.lifecycle\.decommission\.(enabled|allowed-instance-codes|minimum-reason-length|default-retention-days|minimum-retention-days|maximum-retention-days)=' \
  "$CONFIG_FILE" > "$TEMP_FILE" || true

cat >> "$TEMP_FILE" <<'PROPERTIES'

# Matrix26 Lifecycle Manager Phase 3G.2
matrix26.control-center.lifecycle.decommission.enabled=true
matrix26.control-center.lifecycle.decommission.allowed-instance-codes=matrix26-appearance-lab
matrix26.control-center.lifecycle.decommission.minimum-reason-length=10
matrix26.control-center.lifecycle.decommission.default-retention-days=30
matrix26.control-center.lifecycle.decommission.minimum-retention-days=1
matrix26.control-center.lifecycle.decommission.maximum-retention-days=3650
PROPERTIES

mv "$TEMP_FILE" "$CONFIG_FILE"

echo "Lifecycle Manager Phase 3G.2 configuration updated:"
echo "$CONFIG_FILE"
echo "Previous configuration preserved at:"
echo "$BACKUP_FILE"
echo "No password, token, database credential, or backup master key was changed."
