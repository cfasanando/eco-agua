#!/usr/bin/env bash
set -euo pipefail

BASE="src/main/resources/templates/control_center/restores"
CONTROLLER="src/main/java/com/ecoamazonas/eco_agua/platform/control/restores/Matrix26InPlaceRestoreController.java"

for file in in_place_index.html in_place_new.html in_place_detail.html; do
  test -f "$BASE/$file" || {
    echo "ERROR: Missing $BASE/$file"
    exit 1
  }
done

grep -Fq 'return "control_center/restores/in_place_index";' "$CONTROLLER"
grep -Fq 'return "control_center/restores/in_place_new";' "$CONTROLLER"
grep -Fq 'return "control_center/restores/in_place_detail";' "$CONTROLLER"

grep -Fq '@{/control-center/restores/in-place/new}' "$BASE/in_place_index.html"
grep -Fq '@{/control-center/restores/in-place}' "$BASE/in_place_new.html"
grep -Fq '@{/control-center/restores/in-place/{id}/switch' "$BASE/in_place_detail.html"

echo "In-place restore index template: OK"
echo "In-place restore create template: OK"
echo "In-place restore detail template: OK"
echo "Controller template names and routes: OK"
echo "Matrix26 Restore Manager Phase 3F.4c template hotfix checks passed."
