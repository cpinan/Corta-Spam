# BloqueaLlamadas — Product & Technical Spec

Open-source call blocking app (TrueCaller-style capability, zero social graph, zero ads). Android + iOS via Kotlin Multiplatform Mobile (KMM) with Compose Multiplatform UI.

## 1. Platform capability matrix (read this first)

Call blocking is not symmetric across platforms. The OS, not the app, sets the ceiling. Every milestone below is scoped against this table — do not plan a feature without checking it here first.

| Capability | Android | iOS |
|---|---|---|
| Block by exact number | ✅ Full (ConnectionService / CallScreeningService) | ✅ Via `CXCallDirectoryProvider` block list (local, app-supplied) |
| Block by pattern/regex | ✅ Full (evaluated in-process before call reaches user) | ⚠️ Only if pattern can be pre-expanded into concrete numbers at sync time — CallDirectory extension takes a static sorted list, not a regex evaluator. Country-prefix patterns expand fine; open-ended regex does not. |
| Block by country | ✅ Full (libphonenumber parse + rule) | ✅ Same expansion caveat as above |
| Action-based blocking (block until N attempts) | ✅ Full — app owns the call via self-managed `ConnectionService`, can hold local per-number counters and make a live decision per incoming call | ❌ Not possible. iOS never invokes your code at ring-time for a decision; `CallDirectory` is a static list synced ahead of time. **This feature is Android-only, permanently, not just for v1.** |
| Caller ID / spam label display | ✅ Full | ✅ Via `CXCallDirectoryProvider` identification entries, or iOS 18+ `Live Caller ID Lookup` extension (network call at ring-time, Apple-gated entitlement) |
| Voice assistant / scripted auto-responder | ✅ Full — self-managed `ConnectionService` owns call audio, can play TTS/recorded greeting and record the caller | ❌ Not possible. No third-party API touches call audio on iOS, ever. **Android-only, permanently.** |
| App can become the default phone app | ✅ Yes (`RoleManager.ROLE_DIALER` or legacy default-dialer intent) | ❌ Does not exist as a concept on iOS for GSM/carrier calls |

**Decision on record (per user, 2026-07-25):** Android builds on the full self-managed `ConnectionService` (default-dialer) tier from Milestone 1 onward, not the lighter `CallScreeningService`. This is what makes action-based blocking and the voice assistant possible at all. The "voice assistant" is scoped as a **scripted auto-responder** (canned TTS/recorded greeting + optional caller message recording), not a live conversational AI — this keeps it buildable without an STT/LLM pipeline while still using the audio ownership the default-dialer role grants.

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
- **Android app module**: hosts the Compose UI, the self-managed `ConnectionService`, `RoleManager` default-dialer request flow, TTS engine (Android `TextToSpeech`) for the auto-responder.
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
