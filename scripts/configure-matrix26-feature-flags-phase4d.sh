#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mkdir -p reports
{
  echo "Matrix26 Feature Flags Phase 4D configuration"
  echo "Generated at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo
  echo "Phase 4D is read-only. No runtime, database, module activation, purge, backup or restore configuration is changed."
  echo "Open: /control-center/modules/acceptance"
} > reports/matrix26-feature-flags-phase4d-configuration.txt

echo "Matrix26 Feature Flags Phase 4D is read-only; no configuration changes were applied."
