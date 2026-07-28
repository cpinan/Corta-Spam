#!/bin/bash
# Regenerate iOS AppIcon PNG sizes from master SVG.
# Uses macOS qlmanage for SVG→PNG rasterization.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MASTER_SVG="$SCRIPT_DIR/app-icon-master.svg"
IOS_ASSETS_DIR="$SCRIPT_DIR/../../iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"

# iOS icon sizes: width in pixels (size × scale)
SIZES=(
    "40:AppIcon-20@2x"    # 20pt × 2
    "60:AppIcon-20@3x"    # 20pt × 3
    "58:AppIcon-29@2x"    # 29pt × 2
    "87:AppIcon-29@3x"    # 29pt × 3
    "80:AppIcon-40@2x"    # 40pt × 2
    "120:AppIcon-40@3x"   # 40pt × 3
    "120:AppIcon-60@2x"   # 60pt × 2
    "180:AppIcon-60@3x"   # 60pt × 3
    "1024:AppIcon-1024"   # App Store
)

echo "Generating iOS icons from $(basename "$MASTER_SVG") → $(basename "$IOS_ASSETS_DIR")/"
TMPDIR="$(mktemp -d)"

for entry in "${SIZES[@]}"; do
    size="${entry%%:*}"
    name="${entry##*:}"
    echo "  $name.png (${size}x${size})"
    qlmanage -t -s "$size" -o "$TMPDIR" "$MASTER_SVG" > /dev/null 2>&1
    # qlmanage renames by appending .png, but can add extra suffix for square thumbnails
    thumb=$(ls "$TMPDIR"/*.png | head -1)
    cp "$thumb" "$IOS_ASSETS_DIR/${name}.png"
    rm "$TMPDIR"/*.png
done

rm -rf "$TMPDIR"
echo "Done: $(ls "$IOS_ASSETS_DIR"/*.png 2>/dev/null | wc -l) iOS icons regenerated"
