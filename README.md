# Corta Spam

Open-source call blocking app. No ads. No tracking. No data leaves your device.

Screens incoming calls before your phone rings. Checks every number against your rules — manual blocklist, pattern matching, country blocking, quiet hours — plus an optional spam list bundled inside the app (no network, ever). Blocks, allows, or answers with a custom greeting.

**i18n**: English, Spanish (LATAM), Portuguese (Brazil), Hindi.

## Status

M0–M13 complete, including M12's adaptive layout (both tablet list-detail panes now in). 4-language i18n. Open source under MIT License. 373 automated tests pass. Android APK builds. iOS shell builds and runs, but call blocking there is still pending the CallDirectory extension.

- [`docs/SPEC.md`](docs/SPEC.md) — product spec, platform capability matrix, architecture, tech stack
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — milestone breakdown with acceptance tests
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — landscape/tablet layout plan
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — Google Play declaration + privacy policy
- [`LICENSE`](LICENSE) — MIT License

## Features

- **Manual blocking** — block or allow specific numbers, with an optional label shown in the list and in matching Call Log rows
- **Pattern rules** — block by prefix, suffix, or wildcard (`+34900*`, `*1234`)
- **Country blocking** — block all numbers from a country code, matched only against numbers actually written in international form (`+34…` or `0034…`), so blocking Morocco never blocks a Manhattan `212` number
- **Repeat-caller rules** — block a number after it tries N times inside a window, optionally scoped to a pattern
- **Quiet hours** — silence all calls on a schedule (TimePicker with presets: Night, Siesta, Work)
- **Auto-responder (Experimental)** — answer blocked calls with TTS greeting or custom audio; "Test greeting" button previews it locally, no real call needed. The default greeting and the recording-consent phrase are localized, and playback forces the speaker so the caller can actually hear it
- **Caller message recording (Experimental, off by default)** — records what a blocked caller says after the greeting, capped at 60s, playable and deletable from its call-log entry. Gated on both a consent phrase in your own greeting and the Android microphone permission. Records through the microphone, because Android reserves the actual call-audio sources for privileged apps — so on phones whose manufacturer locks the mic during a call it captures nothing
- **Repeated-caller bypass** — opt-in: an unknown number that would otherwise be silently blocked gets let through once it retries enough times, with a heads-up on the ringing screen and a notification. Never applies to numbers matched by a manual block, pattern, country, spam, or schedule rule
- **Call log** — every call with local timestamp, outcome, rule detail, and the contact's name when it matches one (list-detail two-pane on tablet)
- **Call back** — tap any number in the call log to return the call
- **Copy number** — copy phone numbers to clipboard from the call log
- **Stats** — blocked-call counts by day/week/month, bucketed on *local* midnight and DST-aware
- **Backup/restore** — export/import all rules as JSON, with labels preserved; an in-app "View example format" dialog shows the JSON shape
- **Adaptive layout** — bottom bar on phone, nav rail on tablet/landscape, content capped at 600dp
- **Duplicate warnings** — warns when adding a number already present in the other list
- **Precedence engine** — manual block overrides contacts and allowlist
- **Contact normalization** — matches formatted contact numbers against raw incoming call numbers
- **Permission checklist on first run** — after the default-dialer explainer, one screen lists every permission the app asks for and the single thing each is used for, with its system dialog behind an explicit Allow. Nothing on it is mandatory, and the microphone is named but only ever requested when call recording is switched on
- **Permission warnings on Home** — if the app loses the dialer role, notifications, full-screen intent, or the call permission, a card says so above the blocked-call counters, with a button that fixes that specific thing. The same warnings appear in Settings
- **Privacy & Terms** — in-app privacy policy and MIT license terms
- **Ringing** — the app plays the ringtone and vibration itself (as the default dialer contract requires), honouring the system ringer mode, and silences it the moment a block decision lands
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
./gradlew :shared:testDebugUnitTest          # 365 tests, commonTest + androidUnitTest (Robolectric)
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

A complete 22-module HTML course walks through every layer of the app — Gradle, KMM, Compose, Navigation, adaptive layouts, SQLDelight, Koin DI, permissions, Telecom/InCallService, rule engine, i18n, testing, CI, iOS debugging, call notifications/permission UX, extending the precedence engine safely, state management (MVVM + MVI done right, including when *not* to force the pattern), test doubles at scale (consolidating duplicated fakes across KMP test source sets), testing the persistence layer against a real SQLite engine, auditing for code that ships but never runs, platform policy and the claims your app makes, and (newest) permission UX as a design problem — asking, explaining, and noticing when a permission is taken back.

Open [`course/corta_spam_course.html`](course/corta_spam_course.html) in any browser. Dark mode, progress tracking, code snippets from real project files, SVG diagrams, and 88 quiz questions included.

## Recent Fixes

**2026-08-08:** the first thing a new user sees.

- **Home told the user they were protected while telling them screening was off.** Found by installing the release build on a razr 50 ultra and refusing every permission — something no test looked at, because each half was individually correct. The banner said *"Call screening is off until Corta Spam is set as your default phone app"*, and the toggle caption directly beneath it said *"Blocking is on — spam and blocked calls are filtered"*. Without the dialer role no call reaches the app at all, so the second one was simply false, and it is the line a user reads to decide whether they are covered. There is now a third caption for "switched on, but inert", rendered in the error colour. The same install showed four stacked warning cards pushing the toggle and every counter below the fold, so Home now renders only the most severe one — the dialer role outranks a permission that degrades a single feature — followed by a link to Settings, which still lists them all.
- **The warnings were on a screen nobody opens.** `PermissionWarnings` lived privately inside `SettingsScreen`, so an app that had silently stopped working looked identical to one that was working unless the user went looking. It is now shared, rendered on Home as well, and deliberately placed *above* the counters — someone whose blocking has stopped needs to see why before reading a "0 blocked today" that looks like good news. It also gained the warning that matters most, the missing dialer role, whose fix button re-runs the system role request rather than dumping the user in Settings. Contacts and microphone are deliberately still absent: both are optional and explained where the feature that needs them lives, and a banner for a permission you have not asked to use is a nag, not a warning.
- **Giving the dialer role away left the app claiming it still had it.** `DialerOnboardingViewModel.refresh()` runs on every resume so that switching the default phone app from system Settings is picked up — but it only ever upgraded, to `ALREADY_DEFAULT`. Revoke the role and the state stayed `GRANTED` forever: a fully functional-looking app, blocking toggle on, that could no longer screen a single call, with nothing anywhere saying so. It now moves in both directions, and deliberately leaves `REQUESTING` alone, since the system role dialog is on screen and its result — not a resume poll — decides what happens next. Three of the five new tests were watched failing against the old body first.
- **The app's first act was a permission dialog nobody had explained.** `MainActivity.onCreate` fired the `POST_NOTIFICATIONS` request unprompted, on top of a welcome screen that said nothing about permissions — indistinguishable from an app grabbing whatever it can, and the fastest possible way to earn a permanent "Deny". Onboarding now has a checklist step between the dialer explainer and the app: one row per permission, naming the single thing it is used for, with its system dialog behind an explicit **Allow**. Nothing is mandatory; the continue button is always enabled and only changes label. The microphone is listed but never requested there — it is asked for at the moment recording is switched on, because a dialer asking for the mic during onboarding, for a feature that ships off, reads as overreach. Below API 33 the notifications row is omitted entirely rather than shown as a button that opens nothing.

**2026-08-06:** Play policy fallout, and a recording toggle that never recorded.

- **"Record caller message" was a switch wired to nothing.** The flag was persisted, validated by a consent gate, rendered as a Switch, and covered by a passing repository test — and no code anywhere recorded audio. There was no `RECORD_AUDIO` permission, no `MediaRecorder`, no `AudioRecord`. Worse, the published privacy policy carried a whole "Call recording" section describing the capability in both languages, and onboarding promised never to record "unless you separately turn that on". Four user-facing surfaces asserting a feature that did not exist. Now implemented for real: `AutoResponderRecorder` captures the caller's message after the greeting on an auto-answered blocked call, into app-private storage, capped at 60 seconds. **It records the microphone, not the call.** Android gates `VOICE_CALL`/`VOICE_DOWNLINK`/`VOICE_UPLINK` behind `CAPTURE_AUDIO_OUTPUT`, a `signature|privileged` permission no third-party app can hold — holding the dialer role does not grant it — so this is acoustic capture through the earpiece and several manufacturers reserve the mic during calls, in which case nothing is captured and the call simply ends as before.
- **A recording had nowhere to live and no way out.** New `CallLogEntry.recording_path` (migration `2.sqm`) ties audio to the call it came from, so playback and delete appear on that call's row rather than in a disconnected file list, and `clearAll` now deletes the audio files *before* dropping the rows — clearing the log used to be able to leave a stranger's voice on disk with nothing left pointing at it. `logCall` returns its inserted row id inside a transaction with `last_insert_rowid()`; connection-scoped as that is, a second call arriving mid-insert (call waiting makes this real) would otherwise file one caller's recording under another caller's entry.
- **The microphone warning is on the Auto-responder screen, not in Settings.** Warning about a permission for a feature that ships off would be the same permanent nag `showGrantContacts` used to be, so it appears only once recording is actually switched on.
- **Play rejected `USE_FULL_SCREEN_INTENT` as "not directly related to your app's core purpose".** The permission stays: the policy auto-grants it to apps whose core function is receiving phone calls, and this app declares `IN_CALL_SERVICE_UI` *and* `IN_CALL_SERVICE_RINGING`, meaning Telecom stops ringing and hands the job here. What was missing was the Play Console declaration form, which no document in this repo mentioned. Store listing copy, which led with call blocking and named the dialer role once in a closing caveat, now opens with "phone app" in both languages. See `docs/PLAY_FSI_APPEAL.md`.
- **`STORE_COMPLIANCE.md` described a network call that does not exist.** It claimed the optional spam provider sent numbers to a public database. There is no HTTP client in the dependency graph and no `INTERNET` permission; the only bound implementation is an on-device list. The privacy policy had already been corrected for the same error; the compliance doc had not, and it feeds a legally binding Data Safety form.

**2026-08-05 (audit):** a review of the whole codebase, and the fixes for what it found.

- **The phone rang silently.** The manifest declares `IN_CALL_SERVICE_RINGING`, which tells Telecom that this app rings for itself — so Telecom didn't. Nothing in the app ever played a ringtone or vibrated, meaning that as the default dialer it took every call in silence. New `CallRinger` plays the user's ringtone and vibration, honours the system ringer mode, and is stopped the instant a block decision lands — which is what keeps blocked calls silent and is why the declaration was kept rather than handing ringing back to the system.
- **Backup restore disabled the wrong rule.** Importing any disabled rule wrote a default (enabled) row and then looked for "the one I just inserted" as the *last* element of a `created_at DESC, id DESC` query — the oldest row. Restoring a backup containing one disabled pattern switched off an unrelated pattern the user still wanted, silently, on every restore. Rows now carry their real `enabled` and `created_at` in a single insert, the whole import runs in one transaction, counts come from affected rows rather than a loop counter (re-importing your own backup used to claim it added everything twice), `created_at` survives the round trip instead of being restamped, an action rule's `patternId` scope is re-linked to the pattern's new id instead of dangling onto whatever row holds it now, and entries the UI could never produce — `attempts = 0`, which matches every caller — are rejected at the door.
- **The bundled spam list could never match anything.** The resolver normalised the number to bare digits before the lookup, stripping the `+`, while every entry in the list is `+E.164`. All 32 prefixes were inert in the shipping app, and the tests didn't catch it because they exercised the provider directly rather than through `evaluate`. Providers now receive the canonical `+` form. The shape-based pattern list, which was separately unreachable, was removed rather than made live: a "contains a run of zeros" heuristic that has never been measured against real traffic doesn't belong on a path that silently rejects calls.
- **Blocking Morocco blocked Manhattan.** Same root cause: with the `+` stripped, a national-format number is indistinguishable from an international one, so `2125551234` parsed as Morocco (+212) and `912345678` as India (+91). `PhoneNumberParser` now reports a country only for numbers actually written in international form (`+` or the `00` access code), and canonicalises formatting characters on the way.
- **Repeat-caller rules had no UI at all.** The table, repository, resolver branch, `CallAttempt` tracking and backup fields had existed since M2; the only way to create one was hand-editing a backup JSON. There is now a Repeat callers screen with an optional pattern scope. Pattern-scoped rules were also unreachable in the engine — the scope was resolved against *enabled* patterns, and an enabled pattern already blocks those numbers two steps earlier — so scopes now resolve against every pattern, which is what makes a disabled "scope-only" pattern useful.
- **A pattern of `*` blocked the entire phone.** Matching compares digits, so a pattern with none has an empty core, and an empty core satisfies `contains`/`startsWith`/`endsWith` for every number alive. The add dialog accepted it. Rejected now in the matcher, the ViewModel, and backup import. The test that had asserted the opposite behaviour under the name `patternMatch_caseInsensitive` was observing this bug — pattern matching has no notion of case.
- **Four countries could not be added.** `CountryRule` is `UNIQUE(country_code)` with `INSERT OR IGNORE`, and `COUNTRIES` listed codes `1`, `7`, `212` and `590` twice. The second entry appeared in the picker, did nothing when tapped, and never showed up in the list. Merged into one entry per code, pinned by a test.
- **Statistics reset at UTC midnight.** "Blocked today" rolled over at 19:00 for a reader in New York, and the chart's Today row held calls from two different local dates. Boundaries are now computed locally with kotlinx-datetime, and the day buckets are consecutive local midnights rather than fixed 86 400 000 ms steps — which also fixes the 23- and 25-hour days around a DST change putting calls in the wrong bucket twice a year. The seven-day chart used to load every column of every row the call log had ever held; it now reads only blocked timestamps back to the oldest bucket, over a new index.
- **English text in a four-locale app.** `RuleDecision` built its reason as an English sentence *and* wrote it into `CallLogEntry.rule_detail`, so every user read English and every historical row stayed frozen that way. Reasons are now structured data rendered per locale at display time, with a codec that degrades an unrecognised row to its raw text rather than blanking it. Same treatment for the stats day labels and the backup messages. The recording-consent gate only accepted an English phrase, so a user writing their greeting in Spanish, Hindi or Portuguese could never enable recording; all four phrases are accepted now. The default greeting comes from the platform's own resources instead of an English constant.
- **All database I/O ran on `Dispatchers.Default`.** `DriverFactory.databaseDispatcher` had been declared, implemented on both platforms, and read by nobody — 43 blocking SQLite calls sat on the CPU-sized pool Compose also uses. Wired up, with tests that fail if any call site drifts back.
- **A second call corrupted the first.** `onCallAdded` cancelled the whole service scope and overwrote single-field state, so a call arriving during another silently killed the first call's in-flight evaluation: never blocked, never logged. State is per-call now, on a service-lifetime scope.
- Also: the auto-responder never forced the speaker, so the caller heard silence; every incoming call bound a text-to-speech engine whether or not the auto-responder was on; the contacts provider was fully scanned on every ring; the settings repository did its first synchronous disk read on the main thread mid-ring; and history notification ids could collide with the live call's.
- **The privacy policy described something the app doesn't do** — it claimed the optional spam check sends numbers to a public database. The bundled provider is entirely on-device and makes no network calls at all. Corrected, and translated into all four locales along with the two other user-visible strings (`about_open_source`, `terms_conditions_body`) that were English-only. Eight dead string keys and one Spanish-only orphan were removed. A `TranslationCompletenessTest` now covers all three resource trees in both directions, because this project has two independent string systems and Android Lint can only see one of them.
- **Android Lint had never run.** AGP 8.7.3's UAST frontend reads Kotlin 2.0 metadata only and died on every lint task against our 2.2.20 output — which is why it wasn't in CI. AGP 8.13.2 / Gradle 8.14.3 fixes that and also retires two workarounds it had forced (`android.suppressUnsupportedCompileSdk` and the `androidx.activity` downgrade). `kotlin-stdlib` is pinned to the toolchain's own version; SQLDelight was dragging the graph to 2.3.10, i.e. compiling against a newer stdlib than the compiler. Lint found three missing permission guards, a dead pre-API-26 branch, `StateFlow.value` read inside composition, plural forms that are wrong in Hindi and Portuguese, and an adaptive-icon monochrome layer that had been drawn but never referenced — all fixed, and lint now runs in CI.
- **`androidApp` had no release build type**, so `release` was AGP's default: unminified, unshrunk, debug-signed. Added, with ProGuard rules for the reflective corners (serializers whose `@SerialName` ends up in the database, Telecom entry points named from the manifest), and `assembleRelease` runs in CI so R8 is exercised before a release depends on it. `android:dataExtractionRules` now states explicitly what `allowBackup="false"` already meant: the call log never leaves the device, by cloud backup or device transfer.
- 273 → 339 tests (JVM); commonTest also runs 227 of them on the iOS simulator.

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
