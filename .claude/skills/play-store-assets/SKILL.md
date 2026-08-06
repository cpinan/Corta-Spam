---
name: play-store-assets
description: Use when preparing Google Play store listing assets — screenshots, icon, feature graphic — or when Play rejects an upload for aspect ratio or shows the wrong language's graphics. Covers the traps that only surface after the listing form is already filled in.
metadata:
  type: release-runbook
  version: "1.0.0"
---

# Preparing Play store assets without a rejected upload

Three traps, all of which cost real time on 2026-08-05, and all of which surface *after* you have
typed the listing copy.

## 1. Phone screenshots are almost never the right aspect ratio

Play asks for **16:9 or 9:16** and rejects anything much past 1:2. Modern phones are taller than
that, so a raw `screencap` is usually invalid:

| Device | Capture | Ratio | Play |
|---|---|---|---|
| Pixel 8 Pro AVD | 1344×2992 | 0.449 (9:20) | ✗ rejected |
| padded | 1683×2992 | 0.5625 (9:16) | ✓ |

**Pad, don't crop** — cropping eats the navigation bar or the app bar. Width for a given height
is `round(h * 9 / 16)`.

**Pad by replicating the edge column, not with a flat colour.** A phone screenshot has three
horizontal bands with different backgrounds — status bar, content, navigation bar — so one fill
colour leaves a visible seam across two of them.

```python
from PIL import Image
im = Image.open(src).convert("RGB"); w, h = im.size
target_w = round(h * 9 / 16)
pad_l = (target_w - w) // 2; pad_r = target_w - w - pad_l
canvas = Image.new("RGB", (target_w, h)); canvas.paste(im, (pad_l, 0))
canvas.paste(im.crop((0, 0, 1, h)).resize((pad_l, h), Image.NEAREST), (0, 0))
canvas.paste(im.crop((w - 1, 0, w, h)).resize((pad_r, h), Image.NEAREST), (pad_l + w, 0))
```

Keep the unpadded originals; padding is an upload concern, not something to bake into the source.

## 2. Graphics fall back to the **default language**, not to English

Play calls the icon, feature graphic and screenshots *common visual assets*. Any translation that
does not supply its own gets the **default language's** versions.

So if the store's default language is `es-419`, the plain `feature_1024x500.png` must be the
Spanish one. Shipping an English feature graphic as the default puts English marketing copy on
every Spanish listing. Name the variant explicitly (`feature_1024x500_en.png`) and attach it only
to the English translation.

The same applies to screenshots: capture one set per language you actually list.

## 3. Screenshots of an empty app sell nothing

A fresh install shows zeros everywhere. Seed realistic data first.

- **Debug builds:** push a prepared SQLite file with
  `cat db | adb shell "run-as <pkg> sh -c 'cat > databases/<name>.db'"`.
- **Release builds:** `run-as` fails with `package not debuggable`. Use the app's own import
  feature, or screenshot the debug build.

**Every number in a screenshot becomes public.** Use ranges that cannot be a real subscriber —
`+34 900` is Spanish premium-rate/service. Never a real contact.

For a localized set, use per-app locale rather than changing the device language (API 33+):

```bash
adb shell cmd locale set-app-locales <pkg> --locales es-ES
```

That also proves the app's own resource switching, which a device-wide change does not isolate.

## 4. Taking the localized screenshots is a real test

Screenshotting in a non-default language is the cheapest full-app localization audit available,
and it found a shipping bug the same day: the call log rendered the raw database enum
`BLOCKED`/`ALLOWED` in every locale. It was invisible in English because the stored value reads
as plausible English. Read every string in the screenshots before uploading them.

## 5. Rendering the icon and feature graphic

No design tool needed. Rebuild the adaptive icon as SVG — Android vector `pathData` is SVG path
syntax — and render with headless Chrome, which is already installed:

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --disable-gpu \
  --screenshot=out.png --window-size=512,512 "file://$PWD/icon.html"
```

Keeps the store icon in step with the launcher icon instead of drifting into a separate asset.
The feature graphic must have **no alpha**; the app icon may be 24-bit RGB (Play accepts it
despite the "32-bit" wording in older docs).

## Checklist

- [ ] Screenshots exactly 9:16, padded by edge replication, 2–8 per language
- [ ] Default-language graphics are in the **default language**
- [ ] No real phone numbers, no real contacts, no share sheets in any capture
- [ ] Icon 512×512, feature graphic 1024×500 with no alpha
- [ ] Every visible string read for untranslated leftovers
