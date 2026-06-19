#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$PROJECT_DIR/runtime-clients/demo_restaurante_buen_sabor"
CONFIG_FILE="$RUNTIME_DIR/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "[ERROR] Runtime config not found: $CONFIG_FILE"
  exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
  CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
else
  CONFIG_PATH="$CONFIG_FILE"
fi

cd "$PROJECT_DIR"
mkdir -p logs

echo "[INFO] Starting Restaurante El Buen Sabor runtime"
echo "[INFO] Config: $CONFIG_PATH"
echo "[INFO] Expected URL: http://localhost:8084"

mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.config.additional-location=file:$CONFIG_PATH --server.port=8084" \
  2>&1 | tee logs/restaurant-8084.log
