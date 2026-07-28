# Corta Spam

Open-source call blocking app. No ads. No tracking. No data leaves your device.

Screens incoming calls before your phone rings. Checks every number against your rules — manual blocklist, pattern matching, country blocking, quiet hours — plus an optional community spam provider. Blocks, allows, or answers with a custom greeting.

## Status

M0–M10 complete. Adaptive landscape/tablet layout integrated. 151 automated tests pass. Android APK builds. iOS deferred.

- [`docs/SPEC.md`](docs/SPEC.md) — product spec, platform capability matrix, architecture, tech stack
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — milestone breakdown with acceptance tests
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — landscape/tablet layout plan
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — Google Play declaration + privacy policy

## Features

- **Manual blocking** — block or allow specific numbers
- **Pattern rules** — block by prefix, suffix, or wildcard (`+34900*`, `*1234`)
- **Country blocking** — block all numbers from a country code
- **Quiet hours** — silence all calls on a schedule (TimePicker with presets)
- **Auto-responder** — answer blocked calls with TTS greeting or custom audio
- **Spam provider** — optional community spam database (off by default)
- **Call log** — every call with outcome and rule detail (list-detail two-pane on tablet)
- **Stats** — blocked-call counts by day/week/month
- **Backup/restore** — export/import all rules as JSON
- **Adaptive layout** — bottom bar on phone, nav rail on tablet/landscape, content capped at 600dp
- **Corta Spam icon** — custom "Call Barrier" identity (navy handset, coral barrier, cream field)

## Project layout

```
shared/       Kotlin Multiplatform module — commonMain domain logic, Compose UI, SQLDelight
androidApp/   Android application shell (MainActivity, InCallService, InCallActivity)
iosApp/       iOS application shell — project.yml (xcodegen) + Swift entry point
docs/         Spec, milestones, adaptive plan, store compliance, QA scripts
design/       Iconography — SVG masters, Sharp renderer, brand assets
```

## Prerequisites

- JDK 17
- Android SDK (`ANDROID_HOME` set), platform 36 + build-tools
- Xcode 16+ and [xcodegen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) — iOS only
- Node.js (for icon regeneration)

## Build & run — Android

```sh
./gradlew :androidApp:assembleDebug
# with device/emulator connected:
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n org.carlospinan.bloqueador.app/.MainActivity
```

## Build & run — iOS

```sh
cd iosApp && xcodegen generate && cd ..
open iosApp/iosApp.xcodeproj
# or headless:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -configuration Debug build
```

## Tests

```sh
./gradlew :shared:testDebugUnitTest          # 151 tests, commonTest + androidUnitTest (Robolectric)
./gradlew :shared:iosSimulatorArm64Test      # commonTest on Kotlin/Native (deferred)
```

## Icon regeneration

If SVG sources change, regenerate all 21 PNG icons:

```sh
npm install --no-save sharp
node design/iconography/render_ui_icons.mjs
# Expected output: "Rendered and validated 21 interface icons."
```

## License

MIT — see [Terms & Conditions](docs/STORE_COMPLIANCE.md) in-app or at the public repository.
