#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"
PORT="${2:-}"

if [[ -z "$PROFILE" ]]; then
  echo "Usage: bash scripts/run-client.sh <profile> [port]"
  echo "Example: bash scripts/run-client.sh tienda_china_express 8083"
  exit 1
fi

if [[ -n "$PORT" ]]; then
  mvn spring-boot:run \
    -Dspring-boot.run.profiles="$PROFILE" \
    -Dspring-boot.run.arguments="--server.port=$PORT"
else
  mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
fi
