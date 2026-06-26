#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/matrix26_control/application.properties"
MAINTENANCE_DIR="$PROJECT_ROOT/runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$MAINTENANCE_DIR/application.properties.phase3g1.bak"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Matrix26 runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$MAINTENANCE_DIR"
cp -f "$CONFIG_FILE" "$BACKUP_FILE"

TEMP_FILE="$(mktemp)"
grep -vE '^matrix26\.control-center\.lifecycle\.(enabled|allowed-instance-codes|maximum-verified-backup-age-hours|minimum-reason-length)=' \
  "$CONFIG_FILE" > "$TEMP_FILE" || true

cat >> "$TEMP_FILE" <<'PROPERTIES'

# Matrix26 Lifecycle Manager Phase 3G.1
matrix26.control-center.lifecycle.enabled=true
matrix26.control-center.lifecycle.allowed-instance-codes=matrix26-appearance-lab
matrix26.control-center.lifecycle.maximum-verified-backup-age-hours=72
matrix26.control-center.lifecycle.minimum-reason-length=10
PROPERTIES

mv "$TEMP_FILE" "$CONFIG_FILE"

echo "Lifecycle Manager Phase 3G.1 configuration updated:"
echo "$CONFIG_FILE"
echo "Previous configuration preserved at:"
echo "$BACKUP_FILE"
echo "No password, token, database credential, or backup master key was changed."
