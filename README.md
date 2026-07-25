# BloqueaLlamadas

Open-source call-blocking app for Android + iOS. TrueCaller-style blocking capability, zero social graph, zero ads, no data leaves the device unless you explicitly opt in.

- [`docs/SPEC.md`](docs/SPEC.md) — product spec, platform capability matrix, architecture, tech stack.
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — milestone breakdown, each independently testable, with tasks tagged for agent/model assignment.
- [`docs/NAVIGATION.md`](docs/NAVIGATION.md) — screen map, navigation graph, and a ready-to-use design prompt for Claude.

Status: M0 (repo & KMM scaffold) done and verified on-device. Next up is M1 in `docs/MILESTONES.md`.

## Project layout

```
shared/       Kotlin Multiplatform module — commonMain domain logic, Compose UI, SQLDelight
androidApp/   Android application shell (Activity hosting the shared Compose UI)
iosApp/       iOS application shell — project.yml (xcodegen) + Swift entry point
docs/         Spec, milestones, navigation
```

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
