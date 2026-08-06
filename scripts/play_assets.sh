#!/usr/bin/env bash
# play_assets.sh — turn raw screencaps into Play-acceptable store assets.
#
# Play requires 16:9 or 9:16 and rejects much past 1:2. A Pixel 8 Pro capture is 1344x2992,
# i.e. 9:20, so every raw screenshot bounces at upload time — after the listing copy is typed.
# This pads them to exactly 9:16 by replicating the outermost pixel column outward, which keeps
# the status bar and navigation bar continuous instead of framing the shot in a flat block.
#
# Usage:
#   ./scripts/play_assets.sh                 # docs/store/*.png -> docs/store/play/
#   ./scripts/play_assets.sh <src> <dst>
set -euo pipefail
cd "$(dirname "$0")/.."

SRC="${1:-docs/store}"
DST="${2:-docs/store/play}"
mkdir -p "$DST"

python3 - "$SRC" "$DST" <<'PY'
import glob, os, sys
try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required: python3 -m pip install Pillow")

src_dir, dst_dir = sys.argv[1], sys.argv[2]
RATIO = 9 / 16
made = 0

for src in sorted(glob.glob(os.path.join(src_dir, "*.png"))):
    name = os.path.basename(src)
    im = Image.open(src).convert("RGB")
    w, h = im.size

    # Icon (512x512) and feature graphic (1024x500) are fixed sizes -- copy, don't pad.
    if (w, h) in {(512, 512), (1024, 500)}:
        im.save(os.path.join(dst_dir, name), "PNG", optimize=True)
        print(f"  copied  {name:<26} {w}x{h}")
        continue

    if abs(w / h - RATIO) < 0.001:
        im.save(os.path.join(dst_dir, name), "PNG", optimize=True)
        print(f"  ok      {name:<26} {w}x{h} already 9:16")
        made += 1
        continue

    target_w = round(h * RATIO)
    if target_w < w:                       # wider than 9:16; pad height instead of cropping
        target_h, target_w = round(w / RATIO), w
        canvas = Image.new("RGB", (target_w, target_h), im.getpixel((w // 2, 0)))
        canvas.paste(im, (0, (target_h - h) // 2))
    else:
        pad_l = (target_w - w) // 2
        pad_r = target_w - w - pad_l
        canvas = Image.new("RGB", (target_w, h))
        canvas.paste(im, (pad_l, 0))
        canvas.paste(im.crop((0, 0, 1, h)).resize((pad_l, h), Image.NEAREST), (0, 0))
        canvas.paste(im.crop((w - 1, 0, w, h)).resize((pad_r, h), Image.NEAREST), (pad_l + w, 0))
        target_h = h

    canvas.save(os.path.join(dst_dir, name), "PNG", optimize=True)
    print(f"  padded  {name:<26} {w}x{h} -> {target_w}x{target_h}")
    made += 1

print(f"\n{made} screenshot(s) ready in {dst_dir}")
PY

echo
echo "Reminder: default-language graphics must be in the DEFAULT language."
echo "Play falls back to them for every translation that has none of its own."
