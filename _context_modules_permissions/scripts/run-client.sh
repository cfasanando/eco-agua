#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"

if [[ -z "$PROFILE" ]]; then
  echo "Usage: bash scripts/run-client.sh <aguaeco|belen>"
  exit 1
fi

mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
