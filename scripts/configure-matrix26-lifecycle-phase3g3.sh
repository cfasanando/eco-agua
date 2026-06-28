#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/matrix26_control/application.properties"
MAINTENANCE_DIR="$PROJECT_ROOT/runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$MAINTENANCE_DIR/application.properties.phase3g3.bak"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Matrix26 runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$MAINTENANCE_DIR"
cp -f "$CONFIG_FILE" "$BACKUP_FILE"

TEMP_FILE="$(mktemp)"
grep -vE '^matrix26\.control-center\.lifecycle\.archive\.(enabled|allowed-instance-codes|clone-instance-code|clone-instance-name|clone-database-name|clone-runtime-profile|clone-runtime-port|clone-public-url)=' \
  "$CONFIG_FILE" > "$TEMP_FILE" || true

cat >> "$TEMP_FILE" <<'PROPERTIES'

# Matrix26 Lifecycle Manager Phase 3G.3
matrix26.control-center.lifecycle.archive.enabled=true
matrix26.control-center.lifecycle.archive.allowed-instance-codes=matrix26-appearance-lab
matrix26.control-center.lifecycle.archive.clone-instance-code=matrix26-archived-restore-test
matrix26.control-center.lifecycle.archive.clone-instance-name=Matrix26 Archived Restore Test
matrix26.control-center.lifecycle.archive.clone-database-name=matrix26_archived_restore_test
matrix26.control-center.lifecycle.archive.clone-runtime-profile=matrix26_archived_restore_test
matrix26.control-center.lifecycle.archive.clone-runtime-port=8096
matrix26.control-center.lifecycle.archive.clone-public-url=http://localhost:8096
PROPERTIES

mv "$TEMP_FILE" "$CONFIG_FILE"

echo "Lifecycle Manager Phase 3G.3 archive configuration updated:"
echo "$CONFIG_FILE"
echo "Previous configuration preserved at:"
echo "$BACKUP_FILE"
echo "No password, token, database credential, backup master key, or existing decommission state was changed."
