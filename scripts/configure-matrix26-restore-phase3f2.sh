#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/matrix26_control/application.properties"
MAINTENANCE_DIR="$PROJECT_ROOT/runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$MAINTENANCE_DIR/application.properties.phase3f2.bak"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Matrix26 runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$MAINTENANCE_DIR"
cp -f "$CONFIG_FILE" "$BACKUP_FILE"

TEMP_FILE="$(mktemp)"
grep -v '^matrix26\.control-center\.restores\.verification-enabled=' "$CONFIG_FILE" \
  | grep -v '^matrix26\.control-center\.restores\.resume-enabled=' \
  | grep -v '^matrix26\.control-center\.restores\.verification-http-timeout-seconds=' \
  | grep -v '^matrix26\.control-center\.restores\.verification-http-paths=' \
  > "$TEMP_FILE" || true

cat >> "$TEMP_FILE" <<'PROPERTIES'

# Matrix26 Restore Manager Phase 3F.2
matrix26.control-center.restores.verification-enabled=true
matrix26.control-center.restores.resume-enabled=true
matrix26.control-center.restores.verification-http-timeout-seconds=10
matrix26.control-center.restores.verification-http-paths=/,/login,/restaurant/menu,/admin/restaurant/dashboard
PROPERTIES

mv "$TEMP_FILE" "$CONFIG_FILE"

echo "Restore Manager Phase 3F.2 configuration updated:"
echo "$CONFIG_FILE"
echo "Previous configuration preserved at:"
echo "$BACKUP_FILE"
echo "No password, token, or backup master key was changed."
