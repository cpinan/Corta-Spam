# Milestones

> **Current status (2026-07-28):** M0-M10 are implemented and verified (rules engine, all rule types with UI, call screening via PassthroughInCallService, auto-responder, schedule rules, action rules with optional pattern linking, call log with number actions, home dashboard with filtered call log navigation, welcome screen, blocking toggle, contacts permission request, audio file picker, bundled spam heuristics, backup/restore, stats). Android APK builds. All unit tests pass. 
>
> **Adaptive layout phase (2026-07-28):** 3-tier window size detection (Compact/Medium/Expanded), adaptive content capping (max 600dp centered on wider screens), navigation rail on tablet/landscape, FlowRow grid on block list hub. See `docs/ADAPTIVE_PLAN.md` for details. 
>
> iOS is not yet wired (CallDirectoryProvider extension pending). Pending on-device QA per `QA_M2_M8_MANUAL.md`.

Read `SPEC.md` first, especially §1 (platform capability matrix) — it explains why several milestones below are Android-only. Every milestone must end in a state where the app **builds and the new behavior is demonstrably working**, not just "code written." Each milestone lists its acceptance test.

**Standing on-device check, every milestone that touches Android UI:** verify edge-to-edge layout — status bar and navigation bar insets specifically. `targetSdk` 36 enforces edge-to-edge (an app can no longer opt out), so content can draw underneath the status bar or nav bar if `WindowInsets` padding isn't applied where it needs to be. Check on-device, not just in a build: confirm top content (titles, app bars) isn't obscured by the status bar, and bottom content (buttons, bottom sheets) isn't obscured by the navigation bar — on both a 3-button-nav device and a gesture-nav device if both are available, since the nav bar's inset size differs between the two.

Task tags:
- **Agent** — which subagent type fits the task shape (see system's available agent list).
- **Model** — cost/capability tier suggested: `haiku` (mechanical, low ambiguity), `sonnet` (standard feature work), `opus` (architecture decisions, ambiguous tradeoffs, cross-cutting design).

Tasks within a milestone are independent enough to hand to different agents/models in parallel unless marked "depends on."

---

## M0 — Repo & KMM scaffold

**Goal:** empty app builds and launches on both platforms; CI is green.
**Acceptance test:** `./gradlew build` succeeds; Android app installs and shows a blank Compose screen on an emulator; iOS app builds and shows a blank Compose Multiplatform screen in the simulator; a trivial shared-module unit test runs in CI on both the JVM and Kotlin/Native test targets.

| Task | Agent | Model |
|---|---|---|
| Gradle multi-module scaffold: `/shared`, `/androidApp`, `/iosApp`, version catalog | general-purpose | opus |
| SQLDelight setup in `/shared` with empty schema, in-memory test driver wired | cavecrew-builder | sonnet |
| Compose Multiplatform "Hello" screen shared, rendered from both app shells | general-purpose | sonnet |
| GitHub Actions: JVM+Android job, macOS iOS job | general-purpose | sonnet |
| README with build/run instructions | cavecrew-builder | haiku |

---

## M1 — Android default-dialer foundation

**Goal:** app can become the default phone app and behaves identically to the stock dialer (pure pass-through, no blocking logic yet). This is the foundation feature 3 and feature 6 depend on — get it right before adding rules.
**Acceptance test:** on a real device or emulator with telephony, set the app as default dialer via the in-app onboarding flow; place and receive real calls; call quality/behavior is unchanged from the stock dialer. Onboarding flow has UI tests covering grant/deny/already-granted states.

| Task | Agent | Model |
|---|---|---|
| Research spike: `InCallService` + `RoleManager.ROLE_DIALER` request flow, current API constraints on target min SDK (done 2026-07-26 — see SPEC §1 correction: self-managed `ConnectionService` is ineligible for `ROLE_DIALER`) | Explore | opus |
| Implement minimal `InCallService` pass-through (call UI stub, no blocking logic) + `ACTION_DIAL` activity (depends on spike) | general-purpose | opus |
| Default-dialer permission onboarding screen (explainer + request + fallback states) | general-purpose | sonnet |
| Unit tests for onboarding view-model state transitions | general-purpose | sonnet |
| Manual QA script for real-call regression (documented steps, run by a human, not automatable) | cavecrew-builder | haiku |

---

## M1.5 — Codebase hygiene: KMP architecture, DI, testing, comment noise

**Goal:** pay down the technical debt M0/M1 accumulated while the surface area is still small, and lock in conventions before M2 adds repositories/resolvers/more screens on top. No user-visible behavior change — this milestone touches structure, not features. Audited 2026-07-26 against KMP architecture/DI/testing idiom and general software practice; findings below are concrete, not speculative.

**Acceptance test:** `./gradlew ktlintCheck` (or detekt, whichever is chosen) passes clean; `DriverFactory` no longer triggers the `expect class ... Beta` compiler warning; `MainActivity` resolves `DialerOnboardingViewModel`/`DefaultDialerGateway` via DI instead of constructing them inline; new Compose UI tests (Robolectric-based, run on the JVM via `:shared:testDebugUnitTest` — no emulator needed) cover the NOT_REQUESTED/REQUESTING/DENIED/GRANTED-or-ALREADY_DEFAULT states of the onboarding screen, closing the gap against M1's own acceptance test ("UI tests covering grant/deny/already-granted states"), which was only ever verified manually on-device; onboarding UI strings live in Compose Multiplatform string resources, not literal Kotlin strings; comment-to-code ratio in `PassthroughInCallService.kt`/`InCallActivity.kt`/`InCallState.kt`/`OnboardingScreens.kt` drops to WHY-only comments (no restated-code comments, matching the project's own stated comment policy).

| Task | Agent | Model |
|---|---|---|
| Adopt Koin (KMP-native DI) — define an onboarding module (gateway + view-model), wire into `MainActivity`/an `Application` class instead of inline construction. *Recommended over manual DI given M2 is about to add repositories/resolvers/multiple screens; flag to the user if a different choice is preferred before implementing.* | general-purpose | sonnet |
| Replace `expect class DriverFactory` with an `expect fun` factory returning a plain interface (drops the Beta expect/actual-class gate, standard KMP idiom) | general-purpose | sonnet |
| Add Robolectric + Compose UI test infra to `shared`; write the onboarding UI tests described in the acceptance test above | general-purpose | sonnet |
| Add ktlint or detekt, wire into CI, fix whatever the initial baseline run flags | general-purpose | sonnet |
| Comment-noise pass across M0/M1 files: strip comments that restate code or repeat what's already in README/commit history; keep only non-obvious WHY comments | general-purpose | haiku |
| Extract hardcoded onboarding UI strings (`OnboardingScreens.kt`, `CallScreen.kt`) into Compose Multiplatform string resources | cavecrew-builder | haiku |
| Document the resulting conventions in `docs/SPEC.md` (feature-based package structure, interface-in-commonMain + actual-in-platform pattern, prefer hand-written fakes over mocking frameworks in commonTest since MockK etc. aren't multiplatform) | cavecrew-builder | haiku |

---

## M2 — Manual number blocking + rules precedence + call log

**Goal:** first real blocking feature end-to-end, and the precedence engine every later milestone builds on.
**Acceptance test:** add a number to the block list; a test call from that number (via fake `ConnectionService` input in unit tests, and a real test SIM/VoIP call manually) is silently rejected; the same number added to contacts allowlist overrides the block; the call log records the block with the firing rule's reason. Precedence order (allowlist > manual block > pattern > country > quiet hours) is enforced by a unit-tested resolver, not scattered conditionals.

| Task | Agent | Model |
|---|---|---|
| Design the rule precedence resolver (data model + evaluation order) | Plan | opus |
| Implement resolver in `commonMain` + unit tests (depends on design) | general-purpose | sonnet |
| Contacts allowlist: read local contacts (Android `ContactsContract`, iOS `Contacts` framework), match against resolver | general-purpose | sonnet |
| Manual block/unblock list UI (Compose, shared) | general-purpose | sonnet |
| Call log screen with block-reason display | general-purpose | sonnet |
| Wire resolver into Android `ConnectionService` from M1 | general-purpose | opus |
| **iOS spike:** design the App-Group snapshot file format the main app writes and the `CallDirectory` extension reads (SQLDelight can't be opened cross-process safely — needs its own format) | Explore | opus |
| Implement `CallDirectoryProvider` extension reading the snapshot (depends on spike) | general-purpose | sonnet |
| Code review pass on resolver + ConnectionService wiring | cavecrew-reviewer | sonnet |

---

## M3 — Pattern blocking

**Goal:** regex/prefix-based rules layered onto the M2 resolver.
**Acceptance test:** a pattern rule (e.g. `+34900*`) blocks matching numbers and does not block non-matching ones, unit-tested with edge cases (partial matches, malformed input). On iOS, patterns that can be expanded into a concrete number list within `CallDirectory`'s static-list constraints are supported; patterns that can't are flagged as "Android only" in the UI at rule-creation time, not silently dropped.

| Task | Agent | Model |
|---|---|---|
| Pattern matcher (glob/regex subset chosen deliberately, not full regex, to keep iOS expansion tractable) | general-purpose | sonnet |
| iOS pattern-to-concrete-list expansion logic + the "unsupported on iOS" UI guard | general-purpose | opus |
| Pattern rule creation/edit UI | general-purpose | sonnet |
| Unit tests: matcher edge cases | general-purpose | haiku |

---

## M4 — Country blocking

**Goal:** block/allow by country code.
**Acceptance test:** toggling a country blocks numbers whose parsed country matches, verified with a table of real-world number formats (unit test fixture), including numbers with no discoverable country (rejected gracefully, not crashed).

| Task | Agent | Model |
|---|---|---|
| Integrate a multiplatform libphonenumber port into `commonMain` | Explore then general-purpose | sonnet |
| Country rule + resolver integration | general-purpose | sonnet |
| Country picker/toggle UI | cavecrew-builder | sonnet |
| Unit test fixture of real-world number formats per country | general-purpose | haiku |

---

## M5 — Action-based blocking (Android-only)

**Goal:** "let it through after N attempts within T minutes" rule.
**Acceptance test:** simulated repeated calls from the same number in unit tests cross the threshold and flip the resolver's decision; counters persist across app restart; counters expire/reset correctly after the time window. iOS shows this feature as explicitly unavailable with a one-line explanation (see SPEC §1), not a hidden/missing menu item.

| Task | Agent | Model |
|---|---|---|
| Persistent attempt-counter store (SQLDelight) + expiry logic | general-purpose | sonnet |
| Resolver integration (depends on M2 resolver) | general-purpose | sonnet |
| Per-rule threshold/window configuration UI | general-purpose | sonnet |
| iOS "not available" explainer UI | cavecrew-builder | haiku |
| Unit tests: window boundaries, restart persistence, concurrent calls | general-purpose | sonnet |

---

## M6 — Scripted voice assistant / auto-responder (Android-only)

**Goal:** on reject, optionally play a customizable TTS/recorded greeting; optionally record the caller's message. Off by default; recording requires an explicit second toggle plus a consent line in the script (see SPEC §2 item 11 — legal).
**Acceptance test:** a rejected call with the auto-responder enabled plays the configured greeting (manual audio verification, since call audio isn't mockable in unit tests) and, if recording is on, saves the caller's message locally; the consent line is present and non-removable when recording is enabled, unit-tested at the script-validation layer.

| Task | Agent | Model |
|---|---|---|
| Wire TTS/audio playback into the `InCallService`-owned call audio (self-answer + `AudioManager` routing, depends on M1) | general-purpose | opus |
| Script editor UI + mandatory consent-line validation when recording is on | general-purpose | sonnet |
| Caller message recording + local storage | general-purpose | sonnet |
| Unit tests: script validation rules | general-purpose | sonnet |
| Manual QA script for real-call audio verification | cavecrew-builder | haiku |

---

## M7 — Pluggable spam-provider interface (stub only)

**Goal:** feature 5 as requested — "let the code be prepared," not a live integration.
**Acceptance test:** a `SpamProviderClient` interface exists in `commonMain` with a no-op default implementation; a settings toggle exists (off by default) with copy explaining no data leaves the device unless a provider is configured; unit tests cover the interface contract with a fake implementation.

| Task | Agent | Model |
|---|---|---|
| Define `SpamProviderClient` interface + no-op impl | cavecrew-builder | sonnet |
| Settings toggle UI + explanatory copy | cavecrew-builder | sonnet |
| Contract unit tests with a fake provider | general-purpose | haiku |
| Doc: how a future real provider plugs in | cavecrew-builder | haiku |

---

## M8 — Quiet hours / schedule rules

**Acceptance test:** a schedule rule blocks everything except the allowlist within its configured window, unit-tested across midnight-crossing windows and timezone changes.

| Task | Agent | Model |
|---|---|---|
| Schedule rule model + resolver integration | general-purpose | sonnet |
| Schedule config UI | cavecrew-builder | sonnet |
| Unit tests: midnight crossing, DST/timezone edge cases | general-purpose | sonnet |

---

## M9 — Bundled spam heuristics + blocked-call stats

**Acceptance test:** a shipped local list of known spam prefixes/patterns blocks matching test numbers out of the box with no configuration; a stats screen shows blocked-call counts (day/week/month) computed from the M2 call log, no network calls involved.

| Task | Agent | Model |
|---|---|---|
| Curate + ship bundled spam pattern list as a resource file | general-purpose | sonnet |
| Stats aggregation logic + unit tests | general-purpose | sonnet |
| Stats screen UI | cavecrew-builder | sonnet |

---

## M10 — Backup/restore + import

**Acceptance test:** export produces a file that a fresh install can import to reach an identical rule state (round-trip unit test); CSV/JSON import from at least one common competitor export format is unit-tested against a sample fixture.

| Task | Agent | Model |
|---|---|---|
| Export/import format design + round-trip unit tests | general-purpose | opus |
| Optional encryption of the export file | general-purpose | sonnet |
| Import/export UI | cavecrew-builder | sonnet |
| CSV/JSON competitor-format import parser | general-purpose | sonnet |

---

## M11 — Store compliance pass

Process milestone, mostly non-code — track as deliverables, not features.

| Task | Agent | Model |
|---|---|---|
| Google Play "Core purpose" declaration form draft (CALL_SCREENING/default-dialer justification) | general-purpose | sonnet |
| Privacy policy doc reflecting SPEC §3 (no telemetry, opt-in network touchpoints only) | general-purpose | sonnet |
| App Store review notes for the CallDirectory/Live Caller ID Lookup extensions | general-purpose | sonnet |

---

## Suggested execution order

M0 → M1 → M1.5 → M2 are strictly sequential (each is the foundation for the next). M1.5 changes no runtime behavior, so it's low-risk to slot in, but do it before M2 — M2 is exactly where the DI/testing conventions it establishes start paying off (repositories, resolver, more screens). From M3 onward, M3/M4/M7 can run in parallel (independent of each other, all depend only on M2's resolver). M5 and M6 both depend on M1's `InCallService` and M2's resolver but not on M3/M4, so they can also run in parallel with those. M8–M11 are backlog, no hard ordering, pull opportunistically.

## M12 — Adaptive landscape / tablet support

**Goal:** app layout adapts to window size — bottom nav on phone, nav rail on tablet/landscape, capped content width on wide screens.

**Acceptance test:** visual verification on phone portrait (nav bar visible), phone landscape (nav rail visible), tablet landscape (nav rail visible, wider layout). Content is capped at 600dp on wider screens and centered.

| Task | Agent | Model |
|---|---|---|
| Window size detection (3-tier: Compact <600dp, Medium 600-839dp, Expanded >=840dp) | builder | sonnet |
| Adaptive content wrapper (capped width, centered) | builder | sonnet |
| Navigation rail / bottom bar integration (AdaptiveScaffold) | builder | sonnet |
| Screen adaptations (Home, CallLog, BlockListHub, Settings, Stats, AutoResponder, Backup) | builder | sonnet |
| Android configChanges manifest update | builder | haiku |

**Status:** Implemented 2026-07-28. Pending on-device QA. Two-pane list-detail layouts (CallLog Expanded, Settings Expanded) deferred for follow-up.

## M13 — Store compliance

**Status:** Created 2026-07-28. See `docs/STORE_COMPLIANCE.md`.

| Task | Agent | Model |
|---|---|---|
| Google Play declaration form draft | builder | sonnet |
| Privacy policy doc | builder | sonnet |
| App Store review notes (iOS placeholder) | builder | sonnet |
