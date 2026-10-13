#!/bin/sh
# Export the owner's Aseprite sources in art/ to the textures the game loads.
#   art/<path>.aseprite  ->  src/main/resources/assets/signal/textures/<path>.png
# e.g. art/entity/blind_hound.aseprite -> textures/entity/blind_hound.png
# Animated sources (several frames) export as a vertical strip, the layout Minecraft's .mcmeta animation expects.
# Usage (from the project root): sh tools/export_art.sh      Requires Aseprite's command line (aseprite -b).
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ART="$ROOT/art"
OUT="$ROOT/src/main/resources/assets/signal/textures"
command -v aseprite >/dev/null 2>&1 || { echo "aseprite not found on PATH (Steam builds: add its folder to PATH)"; exit 1; }
find "$ART" -name '*.aseprite' | while read -r src; do
    rel="${src#"$ART"/}"
    dst="$OUT/${rel%.aseprite}.png"
    mkdir -p "$(dirname "$dst")"
    aseprite -b "$src" --sheet "$dst" --sheet-type vertical >/dev/null
    echo "exported ${rel%.aseprite}.png"
done
