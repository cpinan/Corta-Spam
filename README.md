# BloqueaLlamadas

Open-source call-blocking app for Android + iOS. TrueCaller-style blocking capability, zero social graph, zero ads, no data leaves the device unless you explicitly opt in.

- [`docs/SPEC.md`](docs/SPEC.md) — product spec, platform capability matrix, architecture, tech stack.
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — milestone breakdown, each independently testable, with tasks tagged for agent/model assignment.
- [`docs/NAVIGATION.md`](docs/NAVIGATION.md) — screen map, navigation graph, and a ready-to-use design prompt for Claude.

Status: M0 (repo & KMM scaffold) and M1 (Android default-dialer foundation) done and verified on-device. Next up is M2 in `docs/MILESTONES.md`.

## Project layout

```
shared/       Kotlin Multiplatform module — commonMain domain logic, Compose UI, SQLDelight
androidApp/   Android application shell (MainActivity, PassthroughInCallService, InCallActivity)
iosApp/       iOS application shell — project.yml (xcodegen) + Swift entry point
docs/         Spec, milestones, navigation, manual QA scripts
```

## Progress log

### M1 — Android default-dialer foundation (2026-07-26)

Goal: app can become the default phone app and behaves identically to the
stock dialer (pure pass-through, no blocking logic yet).

- **Architecture correction:** `docs/SPEC.md`/`docs/MILESTONES.md` originally
  called for a "self-managed `ConnectionService`" default-dialer tier.
  Verified against Android's docs that self-managed `PhoneAccount`s are
  explicitly *ineligible* for `RoleManager.ROLE_DIALER` — corrected both
  docs to the real stack: `ROLE_DIALER` + `InCallService`, with real SIM
  calls still handled by Android's own built-in telephony
  `ConnectionService`.
- **Default-dialer onboarding flow** (`shared` commonMain
  `DialerOnboardingViewModel` + Compose screens matching the permission
  explainer mockup, wired into `androidApp`'s `MainActivity`): explainer →
  real OS role-request dialog (`RoleManager` on API 29+, legacy
  `ACTION_CHANGE_DEFAULT_DIALER` below that) → granted/denied/already-default
  states, each covered by unit tests (`DialerOnboardingViewModelTest`, 8
  cases).
- **`PassthroughInCallService` + `InCallActivity`**: the mandatory in-call
  UI/audio contract for the default-dialer role — minimal answer/decline/
  hang-up screen, no blocking logic.
- **Verified for real on-device** (`Pixel_8_Pro_API_33`), not just built:
  drove the explainer screen, accepted the real system "set as default
  phone app" dialog, confirmed via `dumpsys telecom` that the app became
  the registered default dialer, confirmed a cold relaunch skips onboarding
  correctly, then placed simulated real incoming calls
  (`adb emu gsm call`) and confirmed ring → answer → active → hang-up all
  work through the app's own UI.
- **Two real bugs found and fixed by that on-device verification:**
  1. `CallLog.Calls.addCall()` looks like the sanctioned way to write a
     call-log entry but is a hidden `@SystemApi`, not present in the public
     SDK (confirmed via `javap` against the compileSdk 36 `android.jar`).
  2. Writing call-log entries manually turned out unnecessary and harmful:
     Android's Telecom stack logs every call itself regardless of which app
     is the default dialer. The app-side write produced a confirmed
     duplicate row per call — deleted that code entirely and dropped the
     now-unused `WRITE_CALL_LOG`/`READ_CALL_LOG` permissions.
- Manual QA script for the parts that need a human with real telephony:
  `docs/QA_M1_MANUAL.md`.

## Prerequisites

- JDK 17
- Android SDK (`ANDROID_HOME` set), platform 36 + build-tools installed
- Xcode 16+ and [xcodegen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) — iOS only
- No global Gradle/Kotlin install needed — everything runs through the wrapper (`./gradlew`)

## Build & run — Android

```sh
./gradlew :androidApp:assembleDebug
# or, with an emulator/device already running:
./gradlew :androidApp:installDebug
adb shell am start -n org.carlospinan.bloqueador.app/org.carlospinan.bloqueador.app.MainActivity
```

## Build & run — iOS

The Xcode project is generated from `iosApp/project.yml`, not committed (avoids pbxproj merge conflicts):

```sh
cd iosApp && xcodegen generate && cd ..
open iosApp/iosApp.xcodeproj
# Cmd+R in Xcode, or headless:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -configuration Debug build
```

The `iosApp` target's first build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, which compiles the Kotlin shared module into `Shared.framework` and embeds it — no separate manual step needed.

## Tests

```sh
./gradlew :shared:testDebugUnitTest          # commonTest + androidUnitTest, on the JVM
./gradlew :shared:iosSimulatorArm64Test      # commonTest, on Kotlin/Native (iOS simulator)
```

CI (`.github/workflows/ci.yml`) runs both of the above plus a full Android APK assemble and iOS simulator build on every push/PR.
