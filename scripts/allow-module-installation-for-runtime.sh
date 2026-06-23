#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"
if [[ -z "$PROFILE" ]]; then
    echo "Usage: bash scripts/allow-module-installation-for-runtime.sh <runtime-profile>" >&2
    exit 1
fi

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/$PROFILE/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Runtime configuration not found: $CONFIG_FILE" >&2
    exit 1
fi

if grep -q '^ecoagua\.modules\.installation-allowed=' "$CONFIG_FILE"; then
    sed -i -E 's/^ecoagua\.modules\.installation-allowed=.*/ecoagua.modules.installation-allowed=true/' "$CONFIG_FILE"
else
    printf '\n# Explicit schema operations for this managed runtime only\necoagua.modules.installation-allowed=true\n' >> "$CONFIG_FILE"
fi

echo "Module installation enabled for runtime profile: $PROFILE"
echo "Updated: $CONFIG_FILE"
