# Corta Spam

Open-source call blocking app. No ads. No tracking. No data leaves your device.

Screens incoming calls before your phone rings. Checks every number against your rules — manual blocklist, pattern matching, country blocking, quiet hours — plus an optional community spam provider. Blocks, allows, or answers with a custom greeting.

**i18n**: English, Spanish (LATAM), Portuguese (Brazil), Hindi.

## Status

M0–M10 complete. Adaptive landscape/tablet layout integrated. 4-language i18n. Open source under MIT License. 159+ automated tests pass. Android APK builds. iOS deferred.

- [`docs/SPEC.md`](docs/SPEC.md) — product spec, platform capability matrix, architecture, tech stack
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — milestone breakdown with acceptance tests
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — landscape/tablet layout plan
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — Google Play declaration + privacy policy
- [`LICENSE`](LICENSE) — MIT License

## Features

- **Manual blocking** — block or allow specific numbers
- **Pattern rules** — block by prefix, suffix, or wildcard (`+34900*`, `*1234`)
- **Country blocking** — block all numbers from a country code
- **Quiet hours** — silence all calls on a schedule (TimePicker with presets: Night, Siesta, Work)
- **Auto-responder (Experimental)** — answer blocked calls with TTS greeting or custom audio
- **Call log** — every call with local timestamp, outcome, and rule detail (list-detail two-pane on tablet)
- **Call back** — tap any number in the call log to return the call
- **Copy number** — copy phone numbers to clipboard from the call log
- **Stats** — blocked-call counts by day/week/month
- **Backup/restore** — export/import all rules as JSON
- **Adaptive layout** — bottom bar on phone, nav rail on tablet/landscape, content capped at 600dp
- **Duplicate warnings** — warns when adding a number already present in the other list
- **Precedence engine** — manual block overrides contacts and allowlist
- **Contact normalization** — matches formatted contact numbers against raw incoming call numbers
- **Privacy & Terms** — in-app privacy policy and MIT license terms
- **i18n** — English (default), Spanish (es), Portuguese (pt), Hindi (hi)

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

# or use the helper script:
./install_android.sh
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
./gradlew :shared:testDebugUnitTest          # 159+ tests, commonTest + androidUnitTest (Robolectric)
./gradlew :shared:iosSimulatorArm64Test      # commonTest on Kotlin/Native (deferred)
```

## Icon regeneration

If SVG sources change, regenerate all 21 PNG icons:

```sh
npm install --no-save sharp
node design/iconography/render_ui_icons.mjs
# Expected output: "Rendered and validated 21 interface icons."
```

## Localization

String resources are in `shared/src/commonMain/composeResources/`:

| Directory | Language |
|---|---|
| `values/` | English (default) |
| `values-es/` | Spanish (LATAM) |
| `values-pt/` | Portuguese (Brazil) |
| `values-hi/` | Hindi |

Add new locales by creating a `values-<code>/strings.xml` file following the same key structure.

## License

MIT — see [`LICENSE`](LICENSE). Corta Spam is free and open source.
