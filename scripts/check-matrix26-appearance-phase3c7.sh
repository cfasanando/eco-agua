#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
    echo "ERROR: $1" >&2
    exit 1
}

require_file() {
    local path="$1"
    [ -f "$path" ] || fail "Required file is missing: $path"
}

echo "Checking Matrix26 Appearance Studio Phase 3C.7..."

require_file "src/main/java/com/ecoamazonas/eco_agua/appearance/InstanceAppearanceModelAdvice.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceStudioController.java"
require_file "src/main/resources/templates/control_center/appearance/quality_lab.html"
require_file "src/main/resources/static/js/matrix26-appearance-quality-lab.js"
require_file "src/main/resources/static/js/matrix26-appearance-editor.js"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceEditorForm.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceEditorService.java"
require_file "src/main/resources/static/css/appearance/runtime.css"
require_file "src/main/resources/templates/public/restaurant_menu.html"

grep -q "appearancePublicCssVariables" \
    src/main/java/com/ecoamazonas/eco_agua/appearance/InstanceAppearanceModelAdvice.java \
    || fail "Public CSS variables are not exposed."

grep -q "appearanceAdminCssVariables" \
    src/main/java/com/ecoamazonas/eco_agua/appearance/InstanceAppearanceModelAdvice.java \
    || fail "Admin CSS variables are not exposed."

grep -q "customPalette" \
    src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceEditorForm.java \
    || fail "Custom palette mode is missing from the editor form."

grep -q 'result.put("customPalette"' \
    src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceEditorService.java \
    || fail "Custom palette mode is not serialized."

grep -q "publicPreviewVariables" \
    src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceEditorController.java \
    || fail "Public preview variables are not separated."

grep -q "/appearance/quality-lab" \
    src/main/java/com/ecoamazonas/eco_agua/platform/control/appearance/Matrix26AppearanceStudioController.java \
    || fail "Quality Lab route is not registered."

grep -q "appearanceProductPlaceholderUrl" \
    src/main/resources/templates/public/restaurant_menu.html \
    || fail "Restaurant placeholder integration is missing."

grep -q -- "--appearance-hero-image" \
    src/main/resources/static/css/appearance/runtime.css \
    || fail "Hero asset preservation is missing."

if command -v node >/dev/null 2>&1; then
    node --check src/main/resources/static/js/matrix26-appearance-quality-lab.js
    node --check src/main/resources/static/js/matrix26-appearance-editor.js
    echo "JavaScript syntax: OK"
else
    echo "JavaScript syntax: SKIPPED (node was not found)"
fi

python_command=""
if command -v python3 >/dev/null 2>&1; then
    python_command="python3"
elif command -v python >/dev/null 2>&1; then
    python_command="python"
fi

if [ -n "$python_command" ]; then
    "$python_command" - <<'PY'
from pathlib import Path

files = [
    Path("src/main/resources/static/css/appearance/tokens.css"),
    Path("src/main/resources/static/css/appearance/runtime.css"),
    *Path("src/main/resources/static/css/themes").glob("*.css"),
    *Path("src/main/resources/static/css/layouts").glob("*.css"),
    Path("src/main/resources/static/css/matrix26-control.css"),
]

for path in files:
    content = path.read_text(encoding="utf-8")
    if content.count("{") != content.count("}"):
        raise SystemExit(f"Unbalanced CSS braces: {path}")

print(f"CSS structure: OK ({len(files)} files)")

import re

def channel(value):
    normalized = value / 255.0
    return normalized / 12.92 if normalized <= 0.03928 else ((normalized + 0.055) / 1.055) ** 2.4

def luminance(color):
    red = int(color[1:3], 16)
    green = int(color[3:5], 16)
    blue = int(color[5:7], 16)
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)

def contrast(first, second):
    first_lum = luminance(first)
    second_lum = luminance(second)
    return (max(first_lum, second_lum) + 0.05) / (min(first_lum, second_lum) + 0.05)

pairs = [
    ("--theme-primary", "--theme-on-primary"),
    ("--theme-primary-hover", "--theme-on-primary-hover"),
    ("--theme-accent", "--theme-on-accent"),
    ("--theme-background", "--theme-text"),
    ("--theme-surface", "--theme-text"),
]

for path in Path("src/main/resources/static/css/themes").glob("*.css"):
    content = path.read_text(encoding="utf-8")
    variables = dict(re.findall(r"(--[a-z0-9-]+)\s*:\s*(#[0-9a-fA-F]{6})", content))
    for background_key, foreground_key in pairs:
        background = variables.get(background_key)
        foreground = variables.get(foreground_key)
        if not background or not foreground:
            raise SystemExit(f"Missing contrast variables in {path}: {background_key}, {foreground_key}")
        ratio = contrast(background, foreground)
        if ratio < 4.5:
            raise SystemExit(
                f"Insufficient contrast in {path}: {foreground_key} on {background_key} = {ratio:.2f}"
            )

print("Theme contrast: OK")
PY
else
    echo "CSS structure: SKIPPED (Python was not found)"
fi

if [ "${CHECK_HTTP:-0}" = "1" ]; then
    command -v curl >/dev/null 2>&1 || fail "curl is required when CHECK_HTTP=1."

    MATRIX26_URL="${MATRIX26_URL:-http://localhost:8091}"
    LAB_URL="${LAB_URL:-http://localhost:8094}"

    for url in \
        "$MATRIX26_URL/control-center/appearance/quality-lab" \
        "$LAB_URL/login" \
        "$LAB_URL/restaurant/menu"
    do
        status="$(curl -L -s -o /dev/null -w '%{http_code}' "$url")"
        case "$status" in
            200|302) echo "HTTP $status: $url" ;;
            *) fail "Unexpected HTTP $status from $url" ;;
        esac
    done
fi

echo "Matrix26 Appearance Studio Phase 3C.7 static checks passed."
