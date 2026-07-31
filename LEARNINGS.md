# Corta Spam — Project Learnings

Open-source call blocking app similar to TrueCaller, built with Kotlin Multiplatform and Compose Multiplatform. No ads, no tracking, no data leaves the device.

## App Identity

- **Name**: Corta Spam
- **Package**: `org.carlospinan.bloqueador.app`
- **Platform**: Android (KMP), iOS deferred
- **Min SDK**: 26
- **Target SDK**: 36
- **License**: MIT
- **GitHub**: https://github.com/cpinan/Corta-Spam

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.2.20 |
| Build | Gradle KTS | 8.11.1 |
| UI | Compose Multiplatform | 1.11.1 |
| Navigation | Navigation Compose | 2.9.2 |
| Database | SQLDelight | 2.3.2 |
| DI | Koin | 4.2.0 |
| Async | kotlinx-coroutines | 1.9.0 |
| DateTime | kotlinx-datetime | 0.6.1 |
| Serialization | kotlinx-serialization | 1.7.3 |
| Lifecycle | AndroidX Lifecycle (KMP port) | 2.8.4 |
| Testing | kotlin.test, Robolectric | 4.16.1 |
| Linting | ktlint | 14.2.0 |

---

## Project Structure

```
BloqueaLlamadas/
├── build.gradle.kts              # Root: plugin declarations, resolution strategy
├── settings.gradle.kts           # Module registry (shared + androidApp)
├── gradle.properties             # JVM args, AndroidX, native cache
├── gradle/
│   └── libs.versions.toml        # Version catalog (single source of truth)
├── shared/                       # KMP shared module — ALL business logic
│   ├── build.gradle.kts          # KMP source sets, SQLDelight config
│   └── src/
│       ├── commonMain/           # 80+ files: UI, ViewModels, repos, domain
│       │   ├── composeResources/ # String resources (4 locales)
│       │   ├── sqldelight/       # .sq schema + migrations
│       │   └── kotlin/org/carlospinan/bloqueador/app/
│       │       ├── adaptive/     # WindowSizeClass, AdaptiveScaffold
│       │       ├── autoresponder/# TTS + audio greeting
│       │       ├── backup/       # JSON export/import
│       │       ├── blocklist/    # BlockListScreens, BlockListViewModel
│       │       ├── call/         # CallScreen (answer/decline/hangup)
│       │       ├── calllog/      # CallLogScreen, CallLogViewModel
│       │       ├── contacts/     # ContactsGateway interface
│       │       ├── db/           # DriverFactory, KeyValueSettingsStore
│       │       ├── di/           # Koin modules (shared + platform)
│       │       ├── home/         # HomeScreen, HomeViewModel
│       │       ├── navigation/   # AppNavHost, Routes
│       │       ├── onboarding/   # DialerOnboardingScreen + ViewModel
│       │       ├── rules/        # Rule engine (resolver, repositories)
│       │       ├── settings/     # SettingsScreen, SettingsViewModel
│       │       ├── spam/         # SpamProviderClient, bundled heuristics
│       │       ├── stats/        # StatsScreen, StatsViewModel
│       │       ├── welcome/      # WelcomeScreen
│       │       └── App.kt        # Root composable
│       ├── commonTest/           # 18 files, 120+ unit tests
│       ├── androidMain/          # 4 files: platform actual implementations
│       ├── androidUnitTest/      # Robolectric + Compose UI tests
│       └── iosMain/              # 4 files: iOS actual implementations
├── androidApp/                   # Android application shell
│   ├── build.gradle.kts          # App config, versionCode/Name
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, activities, InCallService
│       ├── res/                  # Launcher icons, colors
│       └── kotlin/org/carlospinan/bloqueador/app/
│           ├── BolqueaLlamadasApp.kt  # Application class (Koin init)
│           ├── MainActivity.kt        # Entry point, permission launchers
│           └── telecom/
│               ├── PassthroughInCallService.kt  # Call screening
│               ├── InCallActivity.kt            # In-call UI
│               ├── InCallState.kt               # Service↔Activity bridge
│               └── AutoResponderAudio.kt        # TTS + audio playback
├── iosApp/                       # iOS shell (xcodegen + Swift)
│   ├── project.yml               # Xcode project specification
│   └── iosApp/                   # Swift entry point
├── docs/                         # Documentation
│   ├── SPEC.md                   # Product & technical spec
│   ├── MILESTONES.md             # Milestone breakdown
│   ├── ADAPTIVE_PLAN.md          # Landscape/tablet plan
│   ├── QA_*.md                   # QA test scripts
│   ├── STORE_COMPLIANCE.md       # Google Play compliance
│   └── ICONOGRAPHY.md            # Icon specs
├── design/iconography/           # SVG masters, Sharp renderer
├── course/                       # HTML learning course (13 modules)
├── .github/workflows/ci.yml      # CI pipeline
├── install_android.sh            # Build + install helper
├── README.md                     # English README
├── README_ES.md                  # Spanish README
└── LICENSE                       # MIT License
```

---

## Architecture Overview

### Layered Architecture

```
┌──────────────────────────────────────────────┐
│  UI Layer (Compose)                          │
│  Screens → ViewModels → StateFlow            │
├──────────────────────────────────────────────┤
│  Domain Layer (commonMain)                   │
│  RuleEngine → Resolver → PhoneNumberParser   │
├──────────────────────────────────────────────┤
│  Data Layer (commonMain)                     │
│  Repositories → SQLDelight → DriverFactory   │
├──────────────────────────────────────────────┤
│  Platform Layer (androidMain / iosMain)      │
│  Drivers, Gateways, DI platform modules      │
└──────────────────────────────────────────────┘
```

### Design Decisions

| Decision | Why |
|----------|-----|
| KMP over Flutter/RN | Native Android APIs (Telecom) accessible without plugins |
| Koin over Dagger/Hilt | Works in Kotlin/Native — essential for iOS compatibility |
| StateFlow over LiveData | Pure Kotlin — works in commonMain, not Android-only |
| SQLDelight over Room | KMP-compatible — Room is Android-only |
| Fakes over MockK | Works in Kotlin/Native — same test on Android and iOS |
| Pure ResolveContext | Stateless rule engine — trivially testable, no side effects |
| Instant UI in InCallService | Wins the race against system incoming-call UI |
| Phone normalization | Strips formatting — matches contact numbers to raw incoming calls |
| Glob patterns over regex | Simple enough for iOS CallDirectory expansion |
| interface over expect class | Avoids Beta compiler limitations, platform-specific constructors |

---

## Database Schema (SQLDelight)

7 tables + 1 key-value store:

| Table | Purpose |
|-------|---------|
| BlockedNumber | Manually blocked numbers |
| AllowlistedNumber | Manually allowed numbers |
| PatternRule | Glob patterns for number ranges |
| CountryRule | Block by country code |
| ActionRule | Block after N attempts in T minutes |
| ScheduleRule | Quiet hours (start/end minute of day) |
| CallLogEntry | Every call with outcome and rule detail |
| AppSettings | Key-value store for preferences |

5 migration files (1.sqm through 5.sqm) track schema evolution.

Timestamp handling uses `strftime('%s', 'now')` instead of `unixepoch()` for compatibility with SQLite versions older than 3.38 (Android API < 33).

---

## Navigation Structure

```
Bottom Bar / Nav Rail (4 sections):
├── Home (0)
│   ├── home             — HomeScreen
│   └── stats            — StatsScreen
├── Call Log (1)
│   └── call_log/{filter} — CallLogScreen (filter: all/today/week/month)
├── Block Lists (2)
│   ├── block_list        — BlockListHubScreen
│   ├── manual_block_list — ManualBlockListScreen
│   ├── allowlist         — AllowlistScreen
│   ├── patterns          — PatternRuleScreen
│   ├── countries         — CountryRuleScreen
│   └── schedules         — ScheduleRuleScreen
└── Settings (3)
    ├── settings          — SettingsScreen
    ├── auto_responder    — AutoResponderScreen
    ├── backup            — BackupScreen
    ├── privacy_policy    — InfoScreen
    └── terms_conditions  — InfoScreen
```

Navigation options: `launchSingleTop = true` prevents duplicate screens. `popUpTo(Routes.HOME)` clears the back stack when switching tabs. Root-destination guard prevents re-navigation when already on the current section's root.

---

## Rule Engine: Precedence Order

Evaluated sequentially — first match wins:

| Priority | Rule | Action |
|----------|------|--------|
| 1 | Manual Block | Block (overrides contacts and allowlist) |
| 2 | Allowlist (contacts + manual) | Allow (bypasses all blocks below) |
| 3 | Pattern | Block if glob matches |
| 4 | Country | Block if country code matches |
| 5 | Spam | Block if external provider flags |
| 6 | Action | Block if N attempts in T minutes exceeded |
| 7 | Schedule | Block if within quiet hours window |
| 8 | Default | Allow or Block (user's settings choice) |

### ResolveContext Pattern

All inputs passed as a data class — the resolver is a pure function:
`evaluate(number: String, context: ResolveContext) → RuleDecision`

Zero mutable state, zero side effects, trivially testable.

### Phone Number Normalization

`PhoneNumberParser.normalizeForComparison()` strips all non-digit characters. Applied at:
- Incoming call number (from Telecom)
- Contact numbers (from ContactsContract)
- Blocked/allowlisted numbers (from DB)
- Pattern core (from user input)

Solves: `(555) 123-4567` from contacts matching `+15551234567` from incoming call.

---

## Permissions & System Roles

| Permission | Type | Purpose |
|-----------|------|---------|
| READ_PHONE_STATE | Dangerous | Detect incoming/outgoing calls |
| CALL_PHONE | Dangerous | Dial numbers from call back |
| READ_CONTACTS | Dangerous | Auto-allow contacts feature |
| BIND_INCALL_SERVICE | Signature | System-only — bind InCallService |
| ROLE_DIALER | System Role | Become default phone app |

All runtime permissions use `ActivityResultContracts` (lifecycle-safe). `ROLE_DIALER` uses `RoleManager.createRequestRoleIntent()`.

### Onboarding Flow

State machine: `NOT_REQUESTED` → `REQUESTING` → `GRANTED` | `DENIED`

"Not now" is a UI-only skip. `onResume()` re-checks `isDefaultDialer()` — detects changes made from system Settings.

---

## InCallService: Call Screening

`PassthroughInCallService` extends `InCallService` — the Android Telecom callback for the default dialer.

### Manifest Metadata

- `IN_CALL_SERVICE_UI = true` — we show our own call UI
- `IN_CALL_SERVICE_RINGING = true` — we handle ringing state

Without these, Android shows its own incoming call screen.

### Critical Race Condition Fix

**Problem**: Evaluating rules takes time (DB queries, contact lookup). During evaluation, the system shows its own UI because it thinks we're unresponsive.

**Fix**: Show `InCallActivity` IMMEDIATELY in `onCallAdded()`, then evaluate rules asynchronously in a coroutine. If the call turns out to be blocked, `call.reject()` dismisses it. The system sees our UI instantly and stays out of the way.

### Auto-Responder

When a blocked call has auto-responder enabled:
1. `call.answer(VideoProfile.STATE_AUDIO_ONLY)` answers the call
2. `AutoResponderAudio` plays TTS greeting or custom audio file
3. On completion, `call.disconnect()` hangs up

Audio uses `AudioAttributes.USAGE_VOICE_COMMUNICATION` for proper routing.

---

## Adaptive Layout

Three WindowSizeClass tiers:

| Tier | Width | Scaffold | Content Layout |
|------|-------|----------|---------------|
| Compact | < 600dp | Bottom NavigationBar | Single column |
| Medium | 600–839dp | Side NavigationRail | Capped at 600dp, centered |
| Expanded | ≥ 840dp | Side NavigationRail | Two-pane (list + detail) |

- `AdaptiveContent` caps at 600dp and centers on wider screens
- `CallLogScreen` uses two-pane layout on Expanded: 340dp list + weighted detail
- `BlockListHub` uses `FlowRow` with 2 columns (Compact) or 4 columns (wider)
- Edge-to-edge handled via `WindowInsets.safeDrawing.only(Top + Horizontal)`

---

## i18n: 4 Locales

String resources in `shared/src/commonMain/composeResources/`:

| Directory | Language | Key count |
|-----------|----------|-----------|
| `values/` | English (default) | ~100 |
| `values-es/` | Spanish (LATAM) | ~100 |
| `values-pt/` | Portuguese (Brazil) | ~100 |
| `values-hi/` | Hindi | ~100 |

Uses Compose Multiplatform resource system: `stringResource(Res.string.key_name)`. Auto-generated Kotlin accessors from XML.

---

## Testing Strategy

| Layer | Count | Type | Location |
|-------|-------|------|----------|
| Unit (rules, parser) | ~120 | kotlin.test | `commonTest/` |
| ViewModel | ~10 | runTest + fakes | `commonTest/` |
| Compose UI | ~10 | Robolectric + createComposeRule | `androidUnitTest/` |
| Manual QA | ~4 | On-device scripts | `docs/QA_*.md` |

- **Fakes over mocks**: Plain Kotlin classes implementing interfaces — works in Kotlin/Native (iOS)
- **runTest**: Synchronous coroutine scope, no real delays, no flakiness
- **Robolectric**: Fake Android runtime on JVM — no emulator needed
- **No MockK**: JVM-only, doesn't work in commonTest

---

## CI Pipeline

`.github/workflows/ci.yml` — three jobs:

| Job | Runner | Steps |
|-----|--------|-------|
| ktlint | ubuntu-latest | Code style check |
| JVM + Android | ubuntu-latest | Tests + APK build |
| iOS (Kotlin/Native) | macos-15 | Native tests + xcodegen + Xcode build |

Fail-fast ordering: lint → test → build.

---

## Build Commands

```sh
# Run all tests (158+)
./gradlew :shared:testDebugUnitTest

# Build debug APK
./gradlew :androidApp:assembleDebug

# Check code style
./gradlew ktlintCheck

# Auto-fix style issues
./gradlew ktlintFormat

# Install on connected device
./install_android.sh

# Regenerate icons (21 PNGs from SVG sources)
node design/iconography/render_ui_icons.mjs
```

---

## Key Bug Fixes Throughout Development

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| Contacts not auto-allowed | Contact format "(555) 123-4567" ≠ incoming "+15551234567" | `normalizeForComparison()` strips non-digits |
| Screen reloads on double-tap | `launchSingleTop` alone insufficient | Root-destination guard in `onNavigate` |
| No incoming call UI | System shows own UI before our evaluation completes | Show InCallActivity instantly before async eval |
| Stats shows literal `%d` | Compose Multiplatform format args unreliable | Use string interpolation with separate label resource |
| Manual block overrode contacts | Precedence order was contacts > manual block | Reordered: manual block (step 1) > contacts (step 2) |
| `unixepoch()` crash on API 33 | Needs SQLite 3.38+, not on older Android | Use `strftime('%s', 'now')` instead |
| Koin pulled incompatible androidx.activity | Transitive dependency conflict | `resolutionStrategy.force()` to pin version |
| Duplicate screen on call_log nav tap | `"call_log/{filter}"` ≠ `"call_log/all"` in pattern match | Section-aware root comparison |

---

## Remaining Work

- Google Play submission (signed release + store listing)
- iOS CallDirectoryProvider extension
- Export file encryption
- Recorded caller message playback UI
- Spam provider live integration
- Settings list-detail two-pane on Expanded
- Home screen recent blocked calls list (dashboard)
