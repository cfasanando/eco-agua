#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTROL_CONFIG="$ROOT_DIR/runtime-clients/matrix26_control/application.properties"

if [[ -f "$CONTROL_CONFIG" ]]; then
  if grep -q '^matrix26.control-center.enabled=' "$CONTROL_CONFIG"; then
    sed -i.bak.phase4c -E 's/^matrix26\.control-center\.enabled=.*/matrix26.control-center.enabled=true/' "$CONTROL_CONFIG"
  else
    printf '\nmatrix26.control-center.enabled=true\n' >> "$CONTROL_CONFIG"
  fi
fi

echo "Matrix26 Feature Flags Phase 4C configuration checked."
echo "This phase protects direct client routes with HTTP 403 when their module flag is inactive."
