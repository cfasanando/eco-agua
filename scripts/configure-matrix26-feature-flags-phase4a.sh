#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPERTIES_FILE="$ROOT_DIR/runtime-clients/matrix26_control/application.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Matrix26 control runtime properties file not found: $PROPERTIES_FILE" >&2
  exit 1
fi

backup_file="$PROPERTIES_FILE.phase4a.bak.$(date +%Y%m%d-%H%M%S)"
cp "$PROPERTIES_FILE" "$backup_file"

ensure_property() {
  local key="$1"
  local value="$2"
  if grep -qE "^[[:space:]]*${key}=" "$PROPERTIES_FILE"; then
    sed -i -E "s#^[[:space:]]*${key}=.*#${key}=${value}#" "$PROPERTIES_FILE"
  else
    printf '\n%s=%s\n' "$key" "$value" >> "$PROPERTIES_FILE"
  fi
}

ensure_property "matrix26.control-center.enabled" "true"

cat <<MSG
Matrix26 Feature Flags Phase 4A configuration checked.
Backup created: $backup_file

This phase uses existing platform_module_catalog and platform_client_module tables,
plus matrix26_instance_module_activation_event for audit evidence.
MSG
