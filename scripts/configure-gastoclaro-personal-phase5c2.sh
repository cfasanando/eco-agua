#!/usr/bin/env bash
set -euo pipefail

PROPERTY='ecoagua.features.personal-finance=true'
FILES=(
  src/main/resources/application-aguaeco.properties
  src/main/resources/application-belen.properties
  src/main/resources/application-example.properties
)

for file in "${FILES[@]}"; do
  [ -f "$file" ] || continue
  if ! grep -q '^ecoagua\.features\.personal-finance=' "$file"; then
    printf '\n# GastoClaro Personal\n%s\n' "$PROPERTY" >> "$file"
    echo "Configured: $file"
  else
    echo "Already configured: $file"
  fi
done

echo "No runtime-client files or personal data were modified."
