#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"
PORT="${2:-}"

if [[ -z "$PROFILE" ]]; then
  echo "Usage: bash scripts/run-client.sh <profile> [port]"
  echo "Example: bash scripts/run-client.sh tienda_china_express 8083"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/runtime-clients/$PROFILE/application.properties"

if [[ -f "$CONFIG_FILE" ]]; then
  echo "Using external runtime config: $CONFIG_FILE"
  if [[ -n "$PORT" ]]; then
    mvn spring-boot:run \
      -Dspring-boot.run.arguments="--spring.config.additional-location=file:$CONFIG_FILE --server.port=$PORT"
  else
    mvn spring-boot:run \
      -Dspring-boot.run.arguments="--spring.config.additional-location=file:$CONFIG_FILE"
  fi
  exit 0
fi

echo "External runtime config not found: $CONFIG_FILE"
echo "Falling back to Spring profile: $PROFILE"

if [[ -n "$PORT" ]]; then
  mvn spring-boot:run \
    -Dspring-boot.run.profiles="$PROFILE" \
    -Dspring-boot.run.arguments="--server.port=$PORT"
else
  mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
fi
