# Corta Spam — Product & Technical Spec

Open-source call blocking app (TrueCaller-style capability, zero social graph, zero ads). Android + iOS via Kotlin Multiplatform Mobile (KMM) with Compose Multiplatform UI.

## 1. Platform capability matrix (read this first)

Call blocking is not symmetric across platforms. The OS, not the app, sets the ceiling. Every milestone below is scoped against this table — do not plan a feature without checking it here first.

| Capability | Android | iOS |
|---|---|---|
| Block by exact number | ✅ Full (ConnectionService / CallScreeningService) | ✅ Via `CXCallDirectoryProvider` block list (local, app-supplied) |
| Block by pattern/regex | ✅ Full (evaluated in-process before call reaches user) | ⚠️ Only if pattern can be pre-expanded into concrete numbers at sync time — CallDirectory extension takes a static sorted list, not a regex evaluator. Country-prefix patterns expand fine; open-ended regex does not. |
| Block by country | ✅ Full (libphonenumber parse + rule) | ✅ Same expansion caveat as above |
| Action-based blocking (block until N attempts) | ✅ Full — as the default dialer, the app's `CallScreeningService` sees every call at ring-time and can hold local per-number counters to make a live allow/reject decision | ❌ Not possible. iOS never invokes your code at ring-time for a decision; `CallDirectory` is a static list synced ahead of time. **This feature is Android-only, permanently, not just for v1.** |
| Caller ID / spam label display | ✅ Full | ✅ Via `CXCallDirectoryProvider` identification entries, or iOS 18+ `Live Caller ID Lookup` extension (network call at ring-time, Apple-gated entitlement) |
| Voice assistant / scripted auto-responder | ✅ Full — as the default dialer's `InCallService`, the app can answer a call itself and drive `AudioManager`/in-call audio routing to play a TTS/recorded greeting and record the caller | ❌ Not possible. No third-party API touches call audio on iOS, ever. **Android-only, permanently.** |
| App can become the default phone app | ✅ Yes (`RoleManager.ROLE_DIALER` or legacy default-dialer intent) | ❌ Does not exist as a concept on iOS for GSM/carrier calls |

**Decision on record (per user, 2026-07-25, architecture corrected 2026-07-26):** Android builds on the full default-dialer tier (`RoleManager.ROLE_DIALER` + `InCallService`) from Milestone 1 onward, not the lighter `CallScreeningService`-only tier available to non-default apps. This is what makes action-based blocking and the voice assistant possible at all.
Correction: the original spec wording called this the "self-managed `ConnectionService`" tier — that was a factual error. Android's `PhoneAccount.CAPABILITY_SELF_MANAGED` is explicitly incompatible with `ROLE_DIALER` eligibility (self-managed accounts are for calling apps that deliberately do *not* become the default dialer, e.g. VoIP apps). The real default-dialer stack is: `InCallService` (mandatory call UI, bound by Telecom, cannot return null), an `ACTION_DIAL` activity, and `RoleManager.ROLE_DIALER`. Real SIM/GSM calls continue to be furnished by Android's own built-in telephony `ConnectionService` — the app does not reimplement call handling for cellular calls, only the UI and screening/audio layers around it. `CallScreeningService` (available to the default dialer automatically, or standalone via the lighter `ROLE_CALL_SCREENING` for non-default apps) is still the mechanism for the ring-time allow/reject decision; the default-dialer tier adds `InCallService` call/audio ownership on top, which is what M6's auto-responder needs. A custom **managed** (non-self-managed, `CAPABILITY_CALL_PROVIDER`) `ConnectionService` is only a future concern if the app ever originates its own VoIP-style calls — not needed for M1–M6.
The "voice assistant" is scoped as a **scripted auto-responder** (canned TTS/recorded greeting + optional caller message recording), not a live conversational AI — this keeps it buildable without an STT/LLM pipeline while still using the audio ownership the default-dialer role grants.

iOS ships full blocking, pattern-expansion blocking, country blocking, and caller ID — everything except action-based blocking and the voice assistant, which are structurally impossible there. The UI must say so plainly (see NAVIGATION.md, Settings screens) rather than hiding the buttons silently — users should never wonder if it's a bug.

## 2. Gaps found in the original feature list (brainstorm output)

Beyond the 9 requested features, an app doing what TrueCaller does needs these to not feel broken:

1. **Contact allowlist** — anyone in the user's local contacts must bypass all block rules by default (toggleable). Without this, pattern/country rules will block friends who happen to share a prefix.
2. **Withheld/private/unknown number handling** — a dedicated rule class, since these numbers have no digits to pattern-match against.
3. **Quiet hours / schedule-based rules** — e.g. block everything except allowlist between 22:00–08:00. Common TrueCaller-adjacent feature, cheap to build on top of the rules engine already needed for feature 3.
4. **Local bundled spam heuristics** — a small on-device list of known spam prefixes/patterns shipped with the app (updated via app releases, not a live network service), so the app has day-one usefulness before feature 5's external API is ever wired up.
5. **Block/allow conflict resolution order** — explicit precedence (allowlist > manual block > pattern > country > quiet hours, or whatever order is decided) must be a documented, tested rule, not implicit.
6. **Call log with reasons** — every blocked call should record *why* it was blocked (which rule fired), or debugging "why did it block my mom" becomes impossible.
7. **Local backup/restore** — export/import block lists and settings as a file (encrypted optional). No cloud account, consistent with "no social network."
8. **Blocked-call stats** — simple on-device counter (calls blocked this week/month), no network, no leaderboard — this is the TrueCaller-ish payoff without the social layer.
9. **Import from common formats** — CSV/JSON import for block lists, so users migrating from another blocker aren't stuck.
10. **Permission/role onboarding flow** — acquiring default-dialer role on Android is an unusual, scary-looking permission ask; a dedicated explainer flow is a hard requirement, not a nice-to-have, or users will bounce or leave 1-star reviews thinking it's malware.
11. **Two-party call recording consent** — if the auto-responder records the caller's voicemail-style message, several jurisdictions require notifying the caller they're being recorded. The scripted greeting must be able to include a recording-consent line, and recording must be an explicit toggle, off by default.
12. **Play Store policy compliance** — apps requesting `CALL_SCREENING`/default-dialer-adjacent permissions must declare a core "Caller ID and spam blocking" purpose via Google's declaration form or face rejection. This is a submission-time task, not code, but must be tracked as a milestone deliverable.
13. **Data never leaves the device by default** — every feature above is local-only; features 5 (spam API) and iOS Live Caller ID Lookup are the only two designed network touchpoints, and both must be explicit opt-in toggles, off by default, clearly labeled.

## 3. Non-functional requirements

- **Privacy-first**: no telemetry, no analytics SDK, no ad SDK. Network access is limited to the two opt-in integrations above.
- **Performance**: a blocking decision for an incoming call must resolve in well under the ~2s window before the OS would otherwise show the native call UI.
- **Testability**: all rule-evaluation, counter, and repository logic lives in `commonMain` (KMM shared module) and is covered by `kotlin.test`/MockK/Turbine unit tests. Platform glue (the actual `ConnectionService`, `CallDirectory` extension) is kept as thin as possible specifically so it stays outside the coverage requirement — it delegates to shared, tested logic immediately.
- **No dead permissions**: the app must not request any Android/iOS permission that a currently-shipped feature doesn't use.

## 4. Tech stack & architecture

- **Shared module (`/shared`)**: Kotlin Multiplatform `commonMain` — domain models, rules engine (blocking precedence, pattern matcher, country matcher via a multiplatform libphonenumber port, action-based counters), repository interfaces, use-cases, ViewModels/state holders. `androidMain`/`iosMain` hold `expect/actual` for telephony, TTS, and persistence drivers only.
- **Persistence**: SQLDelight (KMM-native, generates typed Kotlin from SQL, testable with an in-memory driver).
- **UI**: Compose Multiplatform, one shared UI module consumed by both `androidApp` and `iosApp` shells.
- **Android app module**: hosts the Compose UI, the `InCallService` (call UI + audio ownership), the `CallScreeningService` (ring-time allow/reject decision), the `ACTION_DIAL` activity, `RoleManager` default-dialer request flow, TTS engine (Android `TextToSpeech`) for the auto-responder.
- **iOS app module**: Xcode project wrapping Compose Multiplatform for the main UI, plus two small native extension targets (`CallDirectory`, and optionally `Live Caller ID Lookup`) that read a pre-synced snapshot of the shared block list — extensions run out-of-process and cannot host Compose or open the main SQLDelight database directly, so the app writes a compact snapshot file to the shared App Group container on every rule change. This snapshot format is a named open design spike in Milestone 2 (see MILESTONES.md).
- **CI**: GitHub Actions — one job builds/tests the shared module + Android app on every push; a second job builds the iOS app (macOS runner) and runs shared-module tests on Kotlin/Native.

## 5. Feature-to-milestone map

| Feature (as requested) | Where it's built |
|---|---|
| 1. Block by number | M2 |
| 2. Block by pattern | M3 |
| 3. Block by call-attempt action | M5 (Android-only, see capability matrix) |
| 4. Block by country | M4 |
| 5. Pluggable spam API | M7 (interface + stub only, no live integration) |
| 6. Customizable voice assistant | M6 (Android-only, scripted auto-responder, see capability matrix) |
| 7. TrueCaller-parity gaps | Distributed — see MILESTONES.md backlog items M8+ |
| 8. No social network | Design constraint, not a milestone — enforced by the "never build a shared/community list without explicit re-scoping" rule |
| 9. No ads | Design constraint — no ad SDK dependency is ever added, enforced by not adding the dependency |

See `MILESTONES.md` for the actionable, agent-assignable breakdown, and `NAVIGATION.md` for the screen map and Claude design prompt.

## 6. Conventions locked in at M1.5 (codebase hygiene)

Milestone 1.5 was a codebase hygiene pass to establish shared patterns before M2 (repositories/resolvers/more screens) expands the codebase. All new feature work from M2 onward must follow these conventions.

### 6.1 Package structure: feature-based, not layer-based

Packages are organized by feature (`onboarding/`, `call/`, `telecom/`, `db/`, `di/`, etc.), not by architectural layer. Each feature package holds whatever mix of UI, state, domain, and platform code that feature requires.

```
shared/src/commonMain/kotlin/org/carlospinan/bloqueador/app/
  onboarding/
    OnboardingScreens.kt         (Compose UI: explainer/denied/requesting screens)
    DialerOnboardingViewModel.kt (state)
    DialerOnboardingState.kt     (enum)
    DefaultDialerGateway.kt      (interface)
  call/
    CallScreen.kt                (Compose UI; commonMain even though Android-only for now)
  db/
    DriverFactory.kt             (interface; SQLDelight schema/queries are generated, not hand-authored)
  di/
    KoinInit.kt
    PlatformModule.kt
```

The androidApp module mirrors this by feature too: `telecom/` holds `PassthroughInCallService.kt`, `InCallActivity.kt`, `InCallState.kt` (the platform-specific half of the `call` feature).

Avoid global buckets like `ui/`, `data/`, `viewmodel/`, or `presentation/`. This keeps feature-specific concerns colocated and makes it easier to navigate the codebase without context-switching across layers.

### 6.2 Platform abstraction: interfaces, not expect/actual classes

Define a plain Kotlin `interface` in `commonMain` for any logic that needs platform-specific implementations:

```kotlin
// shared/src/commonMain/kotlin/.../db/DriverFactory.kt
interface DriverFactory {
  fun createDriver(): SqlDriver
}
```

Each platform implements the interface as a concrete class in its source set:

```kotlin
// shared/src/androidMain/kotlin/.../db/AndroidDriverFactory.kt
class AndroidDriverFactory(private val context: Context) : DriverFactory {
  override fun createDriver(): SqlDriver = /* Android impl */
}

// shared/src/iosMain/kotlin/.../db/IosDriverFactory.kt
class IosDriverFactory : DriverFactory {
  override fun createDriver(): SqlDriver = /* iOS impl */
}
```

Do not use `expect class`/`actual class`. The expect/actual mechanism triggers Kotlin's Beta `-Xexpect-actual-classes` compiler warning, and Android implementations often require a constructor parameter (e.g., `android.content.Context`) that `expect`/`actual` cannot cleanly vary across platforms. Instead, let Koin (see 6.3) construct and bind the concrete implementation to the interface.

### 6.3 Dependency injection: Koin

Use Koin (`io.insert-koin:koin-core` in `commonMain`, `koin-android` in `androidMain`) for all dependency resolution.

**Common setup:**

```kotlin
// shared/src/commonMain/kotlin/.../di/KoinInit.kt
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
  startKoin {
    appDeclaration()
    modules(platformModule())
  }
}
```

**Platform module declaration:**

```kotlin
// shared/src/commonMain/kotlin/.../di/PlatformModule.kt
expect fun platformModule(): Module

// shared/src/androidMain/kotlin/.../di/PlatformModule.android.kt
actual fun platformModule(): Module = module {
  single<DriverFactory> { AndroidDriverFactory(androidContext()) }
  single<DefaultDialerGateway> { AndroidDefaultDialerGateway(androidContext()) }
  factory { DialerOnboardingViewModel(get()) }
}

// shared/src/iosMain/kotlin/.../di/PlatformModule.ios.kt
actual fun platformModule(): Module = module {
  single<DriverFactory> { IosDriverFactory() }
}
```

Note iOS's module only binds `DriverFactory` — default-dialer onboarding (`DefaultDialerGateway`, `DialerOnboardingViewModel`) is Android-only (§1), so iOS has no gateway implementation and doesn't need one. Bind interface types explicitly (`single<DriverFactory> { ... }`, not bare `single { ... }`) so Koin resolves by the interface, not the concrete class.

**Android entry point:**

```kotlin
// androidApp/src/main/kotlin/.../CortaSpamApp.kt
class CortaSpamApp : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin {
      androidLogger()
      androidContext(this@CortaSpamApp)
    }
  }
}

// androidApp/.../MainActivity.kt
class MainActivity : ComponentActivity() {
  private val viewModel: DialerOnboardingViewModel by inject()
  // ...
}
```

Resolve dependencies via Koin's `by inject()` property delegate, not manual constructor wiring.

### 6.4 UI strings: no hardcoded literals

All UI text must live in Compose Multiplatform string resources at `shared/src/commonMain/composeResources/values/strings.xml`, never hardcoded in Composables:

```xml
<!-- shared/src/commonMain/composeResources/values/strings.xml -->
<string name="onboarding_title">We need to become your default phone app</string>
<string name="action_continue">Continue</string>
```

```kotlin
@Composable
fun PermissionExplainerScreen(onContinue: () -> Unit, onNotNow: () -> Unit) {
  Column {
    Text(stringResource(Res.string.onboarding_title))
    Button(onClick = onContinue) { Text(stringResource(Res.string.action_continue)) }
  }
}
```

**Important:** Apostrophes in string content do not need backslash-escaping in Compose Multiplatform resource files — unlike Android's classic aapt, CMP's parser renders a literal backslash if you escape. Write `don't` directly, not `don\'t`.

### 6.5 Testing: hand-written fakes, no mocking libraries

Implement test doubles as hand-written fake classes in both `commonTest` and `androidUnitTest`, not via mocking frameworks like MockK (not Kotlin-Multiplatform-safe).

```kotlin
// shared/src/commonTest/kotlin/.../onboarding/DialerOnboardingViewModelTest.kt
private class FakeGateway(var isDefault: Boolean) : DefaultDialerGateway {
  override fun isDefaultDialer(): Boolean = isDefault
}

@Test
fun grantedResultMovesToGranted() {
  val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false))
  viewModel.onRequestStarted()
  viewModel.onRequestResult(granted = true)
  assertEquals(DialerOnboardingState.GRANTED, viewModel.state.value)
}
```

**Naming gotcha:** `androidUnitTest` compiles together with `commonTest` (they share a compilation unit), so a private top-level class with the same simple name in both — e.g. two files each declaring `private class FakeGateway` — fails with a JVM "Redeclaration" error, since top-level `private` in Kotlin restricts *visibility*, not the class's binary name. Give the `androidUnitTest`-side fake a distinct name (this project uses `FakeScreenTestGateway` in `DialerOnboardingScreenTest.kt`).

**Android Compose UI tests** that require rendering and visibility assertions (e.g., full onboarding screen flow) live in `shared/src/androidUnitTest` and use Robolectric + `androidx.compose.ui.test.junit4.createComposeRule()` with `@RunWith(AndroidJUnit4::class)` and `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. Run via `./gradlew :shared:testDebugUnitTest` (no emulator needed).

### 6.6 Lint: ktlint with EditorConfig

**Gradle:** Use the `org.jlleitschuh.gradle.ktlint` plugin. Run via `./gradlew ktlintCheck` (check) or `./gradlew ktlintFormat` (auto-fix). Wire into CI.

**EditorConfig (.editorconfig):** Set `ktlint_function_naming_ignore_when_annotated_with = Composable` so Compose's PascalCase function names (a deliberate exception to camelCase) are not flagged. Disable standard rules for build-generated directories since ktlint-gradle's per-source-set excludes don't reliably reach KMP-generated sources (EditorConfig rule disabling is applied per-file by ktlint's own engine regardless of Gradle's file list):

```
[**/build/**]
ktlint_standard = disabled
```

This keeps generated SQLDelight and Compose-resources code from ever failing `ktlintCheck`, without needing a plugin-level filter per source set.
