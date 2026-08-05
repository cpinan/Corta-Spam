# Corta Spam

Open-source call blocking app. No ads. No tracking. No data leaves your device.

Screens incoming calls before your phone rings. Checks every number against your rules — manual blocklist, pattern matching, country blocking, quiet hours — plus an optional community spam provider. Blocks, allows, or answers with a custom greeting.

**i18n**: English, Spanish (LATAM), Portuguese (Brazil), Hindi.

## Status

M0–M13 complete, including M12's adaptive layout (both tablet list-detail panes now in). 4-language i18n. Open source under MIT License. 273 automated tests pass. Android APK builds. iOS shell builds and runs, but call blocking there is still pending the CallDirectory extension.

- [`docs/SPEC.md`](docs/SPEC.md) — product spec, platform capability matrix, architecture, tech stack
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — milestone breakdown with acceptance tests
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — landscape/tablet layout plan
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — Google Play declaration + privacy policy
- [`LICENSE`](LICENSE) — MIT License

## Features

- **Manual blocking** — block or allow specific numbers, with an optional label shown in the list and in matching Call Log rows
- **Pattern rules** — block by prefix, suffix, or wildcard (`+34900*`, `*1234`)
- **Country blocking** — block all numbers from a country code
- **Quiet hours** — silence all calls on a schedule (TimePicker with presets: Night, Siesta, Work)
- **Auto-responder (Experimental)** — answer blocked calls with TTS greeting or custom audio; "Test greeting" button previews it locally, no real call needed
- **Repeated-caller bypass** — opt-in: an unknown number that would otherwise be silently blocked gets let through once it retries enough times, with a heads-up on the ringing screen and a notification. Never applies to numbers matched by a manual block, pattern, country, spam, or schedule rule
- **Call log** — every call with local timestamp, outcome, rule detail, and the contact's name when it matches one (list-detail two-pane on tablet)
- **Call back** — tap any number in the call log to return the call
- **Copy number** — copy phone numbers to clipboard from the call log
- **Stats** — blocked-call counts by day/week/month
- **Backup/restore** — export/import all rules as JSON, with labels preserved; an in-app "View example format" dialog shows the JSON shape
- **Adaptive layout** — bottom bar on phone, nav rail on tablet/landscape, content capped at 600dp
- **Duplicate warnings** — warns when adding a number already present in the other list
- **Precedence engine** — manual block overrides contacts and allowlist
- **Contact normalization** — matches formatted contact numbers against raw incoming call numbers
- **Privacy & Terms** — in-app privacy policy and MIT license terms
- **Notification control** — a single "Show notifications" switch mutes everything the app posts, including the incoming-call alert
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
./gradlew :shared:testDebugUnitTest          # 265 tests, commonTest + androidUnitTest (Robolectric)
./gradlew :androidApp:testDebugUnitTest      # 8 tests, Android-only classes (Robolectric)
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

## Learning

A complete 19-module HTML course walks through every layer of the app — Gradle, KMM, Compose, Navigation, adaptive layouts, SQLDelight, Koin DI, permissions, Telecom/InCallService, rule engine, i18n, testing, CI, iOS debugging, call notifications/permission UX, extending the precedence engine safely, state management (MVVM + MVI done right, including when *not* to force the pattern), test doubles at scale (consolidating duplicated fakes across KMP test source sets), and (newest) testing the persistence layer against a real SQLite engine — including the labelling bug those tests caught.

Open [`course/corta_spam_course.html`](course/corta_spam_course.html) in any browser. Dark mode, progress tracking, code snippets from real project files, SVG diagrams, and 72 quiz questions included.

## Recent Fixes

**2026-08-05:**
- **M12 is finished.** Settings gains the tablet list-detail layout that had been deferred since 2026-07-28: on Expanded (>=840dp) a section list — Blocking, Contacts, Notifications, About — sits beside a detail pane, matching the Call Log's existing split. Phone and Medium layouts are unchanged, down to the ordering of the settings list. Permission warnings deliberately render on every section rather than being filed under the matching one, since a warning findable only by accident isn't a warning. Verified on an AVD at both 448dp (bottom nav, flat list) and 997dp (nav rail, split panes).
- Closed the remaining test gaps: `SqlRuleRepository` (~40 previously untested members), `BundledSpamProvider`, and `ContactNameLookup`. `androidApp` had no test source set at all — it does now, running under Robolectric and wired into CI. 239 → 273 tests.
- Fixed rule lists coming back oldest-first. `created_at` defaults to whole seconds, so rules added in the same second tied on it and SQLite fell back to insertion order — the opposite of the "most recent first" the repository documents. The five affected queries now break ties by `id`.
- Two findings left deliberately unfixed and documented in tests instead: `BundledSpamProvider`'s only spam *pattern* (`+*000*`) can never match, because the glob matcher only understands leading and trailing stars, so its 0.65-confidence branch is unreachable — making mid-string globs work would start blocking every number containing "000", which is a product call. And `InCallState` can't be fully tested without a mocking library the project deliberately avoids; covering it properly needs an interface between the Telecom callbacks and the object.

**2026-08-04:**
- Fixed the iOS CI job, red on every run for weeks, and stacked two separate faults:
  - `error: Unknown iOS simulator arch: 'x86_64'`. `shared` declares `iosArm64` and `iosSimulatorArm64` but no `iosX64`, while CI builds with `-destination 'generic/platform=iOS Simulator'` — a generic simulator destination resolves `ARCHS` to `arm64 x86_64`, so the Kotlin framework task was asked for a slice that was never configured. `iosApp/project.yml` now sets `EXCLUDED_ARCHS[sdk=iphonesimulator*]: x86_64`, making the Xcode project and the Kotlin targets describe the same architectures. Adding an `iosX64` target was the alternative, rejected because it serves only Intel-Mac simulators and doubles the Kotlin/Native compile work on every iOS build.
  - Behind it, `Undefined symbols: _OBJC_CLASS_$_UIViewLayoutRegion`. Compose Multiplatform 1.11.1 references that UIKit class from `CMPLayoutRegion.o`, and it only exists in the iOS 26 SDK — the `macos-15` runner ships Xcode 16.4 / iOS 18.5. The job now runs on `macos-26` (Xcode 26.6), matching the local toolchain; the deployment target stays at iOS 16.0, only the build SDK moved.
  - Worth knowing for next time: a local `xcodebuild` passes on Xcode 26 even while CI is broken, because the local SDK has the symbol. The job now prints its Xcode and iOS SDK versions before building, so that mismatch is one glance instead of a mystery linker error.
- Tested the last three untested ViewModels — `SettingsViewModel`, `BackupViewModel`, `AutoResponderViewModel` — taking ViewModel coverage to 8 of 8 and the suite to 239 tests. Each guards something specific: `SettingsViewModel`'s sixth flow rides a second `combine()` chained on the first (the typed overloads stop at five), so there's now an assertion that catches a future setting being dropped; `AutoResponderViewModel` gets a test per validation code, `MISSING_CONSENT` included, since recording a call without a consent line in the greeting is a legal problem rather than a cosmetic one; `BackupViewModel` emits only one-time effects on a rendezvous channel, so both export and import failure paths are covered.
- Moved all 7 Robolectric screen tests to the v2 `createComposeRule`, clearing the deprecation warnings from Compose UI test 1.11.2. The v2 rule uses `StandardTestDispatcher` rather than `UnconfinedTestDispatcher`, so coroutines queue instead of running immediately — no test here depended on that, so it was an import swap with no synchronization added.
- Fixed the Stats screen filing today's blocked calls under "Yesterday". `blockedByDay()` built its buckets by stepping forward from `now - daysBack` in 24-hour jumps, so the newest bucket spanned `[now-1d, now)` — it held every call from the last 24 hours, including one placed seconds ago — while its label came from the bucket's *start*, one day earlier. No bucket was ever labelled "Today". Buckets now align to UTC midnight, the same boundary `countBlockedCallsToday` already used, so the chart's first bar and the "blocked today" stat are computed off the same day boundary and can no longer disagree.
- Gave the persistence layer its first real tests — 36 of them, against an in-memory SQLite engine rather than a fake. Not one `Sql*` repository had a dedicated test before: `SqlSettingsRepository`, `SqlCallLogRepository`, `SqlAutoResponderRepository`, `SqlSpamProviderRepository`, and the `KeyValueSettingsStore` they all share. Coverage includes every setting surviving a restart (a second repository over the same database, since these hydrate once in `init` and never re-read), every `RuleDecision` variant round-tripping through the `CallLogEntry.rule_type` CHECK constraint, and the `strftime`-based stats windows. The labelling bug above is what they caught. A second finding is documented but deliberately unfixed: clearing the auto-responder script persists an empty string, but `readString` treats a stored blank as "unset", so a restart resurrects the default script — `readString` is shared by three repositories, so changing its semantics is a wider decision.
- Consolidated the test suite's three biggest duplicated fakes. `FakeRuleRepository` (2 copies), `FakeSettingsRepository` (5) and `FakeCallLogRepository` (3) now exist once each in `shared/src/commonTest/.../app/testing/`, shared by `commonTest` and `androidUnitTest` alike — 15% of all test source was hand-copied fake bodies, and `RuleRepository`'s 64 members meant every interface change had to be applied to each copy by hand. One side effect worth naming: `SettingsRepositoryTest.kt` turned out to assert nothing about `SqlSettingsRepository` — all five of its tests ran against the fake declared in the same file. It's now `FakeSettingsRepositoryTest.kt` and pins the shared fake's write-through setters, which the ViewModel tests genuinely depend on; the real `SqlSettingsRepository` was left untested at that point, a pre-existing gap the old filename had been hiding — closed later the same day by the persistence-test pass above. Deliberately left duplicated: the 5-to-10-line `ContactsGateway`/`DefaultDialerGateway` fakes, where a local declaration reads better than an import.
- **Breaking (pre-release):** the database file was renamed `bloquellamadas.db` → `cortaspam.db` and `rootProject.name` is now `CortaSpam`. A new filename means every device opens a fresh, empty database — existing block lists, rules and call history on dev devices are gone. Done deliberately while there is no public release.
- Fixed a migration safety net that had never actually run. `./gradlew :shared:verifySqlDelightMigration` failed with `duplicate column name: pattern_id`, and no CI job depended on it, so nothing noticed. Root cause was three-layered: `schemaOutputDirectory` was never configured so schema snapshots went stale at `5.db` while migrations reached `7.sqm`; and, once a correct baseline was rebuilt, the check caught a real bug — `5.sqm` added `pattern_id` via `ALTER TABLE ADD COLUMN`, which SQLite cannot use to attach the `REFERENCES PatternRule(id)` foreign key that `AppDatabase.sq` declares, so upgraded databases and fresh installs genuinely had different schemas.
- Because the database rename leaves the seven historical migrations with no users, they were squashed into a single baseline snapshot at `shared/src/commonMain/sqldelight/databases/1.db`. `verifySqlDelightMigration` now runs in CI, and was verified to both pass on a matching schema and fail with a precise column diff on a mismatched one.
- Pinned the Compose Multiplatform resources package via `compose.resources { packageOfResClass = ... }`. It was previously derived from `rootProject.name`, so renaming the project broke every `Res.string.*` import across 12 screen files.
- Moved `bloquea_llamadas_mockups.html` out of the repo root to `design/mockups.html`, and corrected the rule order it documented. It claimed "Allowlist first, then Manual" in two places; the shipped `RulePrecedenceResolver` checks manual block at step 1 and allowlist at step 2, deliberately, so that a manual block outranks a contact match.
- Removed dead files a codebase review turned up: `Greeting.kt`/`GreetingTest.kt` (M0 scaffold whose only caller was its own test — test count 176 → 175), the four `.claude/hooks/*.sh` scripts (unmodified template scaffolding matching a `feature/`/`domain/`/`core/` module layout this repo doesn't have, never referenced from `settings.json`, and weaker than the `corta-spam-verify-build` skill that superseded them), and the tracked `.session_state.md` session scratch file.

**2026-08-02:**
- Architecture review found the app was consistently MVVM (single `StateFlow<UiState>` per ViewModel, Koin-scoped) but not MVI — no ViewModel had a single typed entry point for user actions, just N public setter methods each. Every ViewModel with at least one dispatchable action now exposes a sealed `Intent` type + one `onIntent()` function; the old public methods are private implementation details now. Read-only ViewModels with nothing external to dispatch (`StatsViewModel`) deliberately did not get one — a one-case sealed class with no caller is ceremony, not MVI.
- Fixed 3 ViewModels that had drifted from the app's own "one UiState per ViewModel" rule: `BlockListViewModel` (11 separate `StateFlow`s → one `BlockListUiState` with counts as derived properties), `CallLogViewModel` and `DialerOnboardingViewModel` (two disconnected flows each → one UiState). `CallLogViewModel` also absorbed the call-log date-range filtering logic that used to live inline in the navigation file, with new regression tests it never had before.
- Fixed `DialerOnboardingScreen` taking the ViewModel directly as a Composable parameter — the one place in the app doing that. It now takes state + an intent-dispatch callback like every other screen.
- `BackupViewModel`'s export flow used to hand the exported JSON back via a UI-supplied callback parameter, which doesn't fit cleanly into a plain-data Intent. Replaced with a new `BackupEffect.Exported(json)` case alongside the existing success/failure effects.
- Moved the Privacy Policy and Terms & Conditions body text out of hardcoded Kotlin string literals into string resources (English only for now).

**2026-08-01:**
- **New**: repeated-caller bypass. An unknown number (no rule matched, falling through to `defaultAction = Block`) that retries at least N times within 24h gets let through instead of silently blocked forever — off by default, attempts threshold configurable (2-10) in Settings. Deliberately scoped to only the no-rule-matched path in `RulePrecedenceResolver`: a manual block, pattern, country, spam, action-rule, or schedule match always wins regardless of retry count, since retrying is a robocall hallmark and bypassing those would undo real spam blocking. Shows a "called you N times" hint on the ringing screen and a notification when it fires; reuses the existing attempt-tracking infrastructure built for the mirror-image "block after N attempts" action rule. Required a `CallLogEntry.rule_type` schema migration for the new `REPEATED_ALLOWED` tag.
- Added a "View example format" dialog to the Backup screen showing a sample JSON snippet (with a labeled entry) — the label field was already round-tripped end-to-end in export/import, just undocumented in-app.
- Added a "Test greeting" button to the Auto-responder screen that plays the current script/audio locally through the phone speaker, so you can preview it without triggering a real call.
- Added a "Show notifications" setting that mutes every notification the app posts, including the incoming-call alert — off means calls stay fully silent unless the app is already foregrounded. Also centered the app logo in the empty middle of the in-call screen for all phases (ringing/dialing/active).
- Fixed a crash: tapping "Call back" on a private/restricted call-log entry (blank number, a legitimate case for withheld callers) built an unresolvable `tel:` intent and threw an uncaught `ActivityNotFoundException`. The action is now disabled for blank numbers, and the call-placing code is defensive against any other unresolvable-intent case.
- Architecture cleanup, driven by a full review against clean-architecture/MVVM conventions:
  - All 7 ViewModels are now scoped to their nav route instead of living for the whole app session.
  - The Backup screen's result message was persistent state with nothing ever clearing it, so it re-appeared stale on revisit — replaced with a proper one-shot effect + Snackbar.
  - `MainActivity` no longer injects repositories/ViewModels directly; the audio-picker result and first-run `welcomeShown` flag now flow through the ViewModels that actually own them.
  - Settings/Auto-responder/Home now each expose a single `UiState` instead of several independent state flows.
  - Extracted the incoming-call rule-evaluation logic out of the Android Telecom service into a shared, unit-tested use case — previously the single largest piece of untested logic in the app.
- **New**: optional labels on blocked/allowlisted numbers now also surface in Call Log rows for allowlist matches (manual blocks already showed theirs).
- **New**: Call Log, Block List, and Allowlist rows show the matching phone contact's name instead of the raw number (Android only — iOS contacts access is still a stub).
- Fixed `install_android.sh`'s rebuild check: it compared the APK's timestamp against `./gradlew` (which never changes) instead of the actual sources, so it silently kept installing a stale build after the first run.

**2026-07-31:**
- Added the full incoming-call notification pipeline — previously none existed, so incoming calls only surfaced via a plain `startActivity()` from a background service, which Android silently drops when the screen is off/locked. Now: a full-screen ringing alert with Answer/Decline actions, a persistent "return to call" notification while a call is active, and post-call missed/blocked notifications showing the caller's contact name (via `ContactsContract.PhoneLookup`) and block reason. Three notification channels; all strings localized (en/es/hi/pt) via native Android resources.
- Settings now shows real permission-status warnings — notifications denied, full-screen-intent revoked (Android 14+), Phone permission denied — each with a one-tap link to the right system settings screen. These used to fail silently with no signal to the user.
- `DefaultAction.ASK` is now a real behavior instead of a silent alias for Allow: unmatched calls are let through and tagged "Needs review" in the call log. Home shows a "Pending review" count card; Call Log has a matching filter and a distinct visual state.
- "Call back" in Call Log now actually places the call (`ACTION_CALL` + runtime `CALL_PHONE` permission) instead of just opening the dialer with the number pre-filled.
- Home's blocking toggle now has a caption explaining what it does.
- Auto-responder's experimental warning now explains *why* it may not work — newer Android/OEM audio-routing restrictions (the same anti-wiretapping hardening that affects call-recorder apps) — instead of a vague "may not work on all devices."
- Fixed: Home stats going stale after backgrounding (a resume-refresh had regressed), the contacts-permission prompt nagging forever even after granting (it was checking whether a callback existed, not the actual permission), a dead import that broke `ktlintCheck`, and a dead unused method in `PassthroughInCallService`.

**iOS (2026-07-30):**
- Fixed Koin initialization — `initKoin()` now called in `MainViewController.kt` before Compose UI launches (was only initialized on Android via `BloqueaLlamadasApp.onCreate`)
- Added `CADisableMinimumFrameDurationOnPhone: true` to Info.plist via `project.yml` (required by Compose Multiplatform for high-refresh-rate iPhones)
- Replaced `Dispatchers.IO` with `Dispatchers.Default` throughout shared module (internal API on Kotlin/Native)
- Replaced `Clock.System` with platform `expect/actual currentTimeMillis()` for iOS compatibility
- Added `databaseDispatcher` to `DriverFactory` interface (Android: `IO`, iOS: `Default`)
- Fixed Navigation Compose `arguments?.getString()` → `Map` cast for KMP compatibility
- Removed leftover `import kotlinx.coroutines.IO` (internal on Native)
- **Known**: Metal GPU rendering may hang on certain iOS simulators (e.g., iPhone 16 Pro on macOS 26). Use iPhone SE or physical device. Software rendering fallback pending.

**Android (2026-07-30):**
- Added Call back action in call log (`ACTION_DIAL` intent)
- Added local timestamps to call log entries via `currentTimeMillis()` expect/actual
- Fixed bottom nav double-tap screen reload with section-aware root comparison
- InCallActivity now shows instantly on call arrival (wins race against system UI)
- Added KeyguardManager dismiss for full-screen incoming call takeover
- Hidden spam provider toggle from settings (backend preserved)
- Auto-responder marked as Experimental
- Stats screen de-hardcoded (Loading, blocked count)
- Copy number now wired to clipboard (`ClipboardManager`)
- Phone number normalization for cross-format contact matching (`normalizeForComparison`)

## License

MIT — see [`LICENSE`](LICENSE). Corta Spam is free and open source.
