#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"
PORT="${2:-}"

if [[ -z "$PROFILE" ]]; then
  echo "Usage: bash scripts/run-client.sh <client-profile> [port]"
  echo "Example: bash scripts/run-client.sh demo_tienda_china_temu 8083"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLIENT_DIR="$ROOT_DIR/runtime-clients/$PROFILE"
CONFIG_FILE="$CLIENT_DIR/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "[ERROR] Runtime config not found:"
  echo "$CONFIG_FILE"
  echo ""
  echo "Generate runtime files from:"
  echo "/admin/platform/clients/{id}/provisioning"
  echo ""
  echo "Available runtime clients:"
  if [[ -d "$ROOT_DIR/runtime-clients" ]]; then
    find "$ROOT_DIR/runtime-clients" -mindepth 1 -maxdepth 1 -type d -printf '  - %f\n' 2>/dev/null || ls -1 "$ROOT_DIR/runtime-clients"
  else
    echo "  none"
  fi
  exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
  CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
else
  CONFIG_PATH="$CONFIG_FILE"
fi

SPRING_ARGS="--spring.config.additional-location=file:${CONFIG_PATH}"
if [[ -n "$PORT" ]]; then
  SPRING_ARGS="$SPRING_ARGS --server.port=$PORT"
fi

cd "$ROOT_DIR"

echo "[INFO] Starting client profile: $PROFILE"
echo "[INFO] Config: $CONFIG_PATH"
if [[ -n "$PORT" ]]; then
  echo "[INFO] Port override: $PORT"
fi

mvn spring-boot:run -Dspring-boot.run.arguments="$SPRING_ARGS"
