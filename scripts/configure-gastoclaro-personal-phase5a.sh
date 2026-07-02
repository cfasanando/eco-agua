#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

update_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  if ! grep -q '^ecoagua.features.personal-finance=' "$file"; then
    cat >> "$file" <<'PROPS'

# GastoClaro Personal
# Personal finance module visibility. Runtime DB setting module.personal_finance.enabled remains the source of truth.
ecoagua.features.personal-finance=true
PROPS
    echo "Configured $file"
  else
    echo "Already configured $file"
  fi
}

update_file "src/main/resources/application-aguaeco.properties"
update_file "src/main/resources/application-belen.properties"
update_file "src/main/resources/application-example.properties"

find runtime-clients -maxdepth 2 -name application.properties -print 2>/dev/null | while read -r file; do
  update_file "$file"
done

echo "GastoClaro Personal Phase 5A configuration completed."
