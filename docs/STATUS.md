# STATUS — Corta Spam

_Last updated: 2026-09-16 · branch `main` · 0 uncommitted files (5 untracked `.claude/skills/` dirs, not ours)_

## Next action

Run **all three** rounds of unverified fixes on physical hardware — none of this has touched real
silicon yet, only an emulator/unit tests:
- 2026-09-02: `./scripts/ring_test.sh watch --device <serial>` with Do Not Disturb on, called from
  a second phone (Redmi Note 13 Pro / HyperOS is where that report came from).
- 2026-09-15: `./scripts/call_test.sh` / `./scripts/ring_test.sh` again for the full-screen gate
  and the auto-responder trigger, plus manually toggling the "Notifications" setting off and
  confirming the ongoing-call notification still appears (see In flight / State below).
- 2026-09-16: exercise "Hang up & block" during real call waiting (a second call arriving while
  the button is on screen) to confirm `blockAndEndAction` actually leaves the promoted call alone;
  tap a call-log entry, cancel the dialog, switch tabs and back; tap an `ACTION_DIAL` `tel:` link,
  edit the number, switch tabs and back.

## State

- **1.6.1 (9) is live in production.** Codes 6–9 are spent; the next build takes 10 or higher.
  Version vals are still unbumped — 1.6.1 in `androidApp/build.gradle.kts`.
- **Four user reports handled** (`a4c583b`, `0d237c3`, `cf21606`, `34953c3`, docs `9a951ff`):
  Do Not Disturb is honoured when ringing; the auto-responder mutes the uplink once the greeting
  ends; Compose format args are positional so `%d` no longer reaches the user; the contact matcher
  is stressed with 100 near-miss numbers.
- **Four more reports handled** (`f9d7606`, `dde6e4c`, `5263fd0`, `8f37fef`, docs `49aa899`):
  1. Auto-responder trigger gate extracted to `AutoResponderTriggerPolicy` (shared/commonMain) so
     it's unit-testable outside Telecom; default stays **off** (deliberate — recording consent),
     wiring itself was already correct, just untested. New: `AutoResponderTriggerPolicyTest`.
  2. Full-screen call UI gate — see In flight.
  3. `IncomingCallNotifier.notifyOngoingCall` ("return to call") was gated behind the general
     notifications toggle; removed that gate so turning notifications off can't remove the only
     way back to a live call (matches `HomeScreen`'s own `ReturnToCallCard`, which was already
     built this way).
  4. New "Hang up & block" / "Decline & block" action on `CallScreen` (RINGING and
     ACTIVE/DIALING/OTHER phases, hidden for a blank number) — ends the call via `InCallState` and
     writes the block rule through the existing `CallActionReceiver.ACTION_BLOCK_NUMBER` path.
- **A partial code review of the 2026-09-15 batch found 4 more bugs, all fixed 2026-09-16**
  (`9354a93`, `be89c5b`, `37479c6`, `4a6b33c`):
  1. Call log: a dismissed caller-actions dialog reopened on returning to the screen —
     `MainActivity.callLogRequest` was never cleared once applied; `CallLogScreen`'s own "already
     applied" guard is `remember`, local to one composition, and this screen is discarded and
     recreated by navigation (no `saveState`/`restoreState` anywhere in this nav graph).
  2. `PassthroughInCallService`: a `callLogRepository.logCall` exception could leave a call the
     gate had already hidden the full-screen UI for never actually marked `blockedByRules` —
     ringer, notification and auto-responder all skipped, call rang until the caller gave up.
     New `tolerateWriteFailure` (shared/commonMain) isolates the log write from the decision.
  3. "Hang up & block" could end a *different* call than the one shown, if call waiting promoted a
     second call onto the screen between the render and the tap. New `blockAndEndAction`
     re-derives the termination action from live state at tap time, by number.
  4. Keypad had the identical bug as (1) for `ACTION_DIAL` — `dialRequest` also never cleared.
     Proved with a test that reproduces `NavHost`'s real saved-state discard (`SaveableStateHolder`
     + `removeState`), since a bare recomposition does not catch this class of bug.
  The review itself was cut short — several of its sub-agents hit an account-wide API session
  limit mid-run (conventions/CLAUDE.md check, reuse-angle, line-by-line diff scan, removed-behavior
  audit, cross-file tracer all failed outright) — so treat it as a partial pass, not exhaustive.
  Lower-severity findings **not yet acted on**: the full-screen gate's racer coroutine still isn't
  stored on `CallState`/cancelled in `onCallRemoved` like its siblings; `FULL_SCREEN_GATE_TIMEOUT_
  MILLIS` (700 ms) is an unmeasured guess; `NotificationPolicy` doesn't document the return-to-call
  exception it now has to know about; the block-button layout and the decline-vs-hangup choice are
  each duplicated in two places (`CallScreen.kt` and `InCallActivity.kt`).
- **779 tests** (`:shared` 666, `:androidApp` 113), 36 course chapters, 194 quiz questions.
- **Unreleased since 1.6.1**: the keypad caret fix (`e274f7e`), the four 2026-09-02 fixes, the four
  2026-09-15 changes, and the four 2026-09-16 fixes above. Release notes for 1.6.2 in four locales
  are still unwritten.
- iOS still has no call blocking — pending the CallDirectory extension. `verify.sh` compiles
  `commonTest` for Native but **runs no iOS test**; `:shared:iosSimulatorArm64Test` is CI-only.

## In flight

- **Fixed 2026-09-15** (`dde6e4c`): `PassthroughInCallService.onCallAdded` no longer calls
  `startActivity(InCallActivity)` unconditionally for an incoming call. It now waits up to
  `BlockedCallPolicy.FULL_SCREEN_GATE_TIMEOUT_MILLIS` (700 ms) for the block decision
  (`CallState.blockedSignal`, a `CompletableDeferred<Boolean>` completed once `evaluate()`
  resolves); a call already known to be blocked never reaches the screen at all, and a call still
  undecided past the window shows anyway (fail-open, same posture as the rest of this service).
  Outgoing calls are unaffected — `showInCallActivity()` still fires immediately for them, no gate.
  `announce()`'s DND-silence path deliberately still does **not** suppress the full-screen UI — a
  DND-silenced call is not a rejection, only a quiet one, and Android's own dialer shows it too.
  Unit-tested only (`AutoResponderTriggerPolicyTest` covers the related auto-responder gate); **not
  yet run on physical hardware** — see Next action.
- `androidApp/.../telecom/NotificationPolicy.kt:18` — `notifyUnknownCallers` gates only the *result*
  notification (blocked/missed/repeated). The ringing notification does not go through it at all, so
  "don't notify me about unknown callers" does not describe what the setting does. Decide whether the
  ringing alert should be in scope, then make the setting's label match.
- **A small iOS test** — nothing under `shared/src/iosTest`. Cheapest first one: `PhoneNumberParser` /
  `RulePrecedenceResolver` already run on Native via `commonTest`, so add an actual
  `:shared:iosSimulatorArm64Test` run to `verify.sh` rather than new test code, and confirm it is green
  locally before trusting CI. Watch the two known Kotlin/Native traps: no commas in backticked test
  names, and `viewModelScope` needs `Dispatchers.setMain` or the test hangs for a minute.
- Reporter follow-up still unsent: ask for their *Acción por defecto*, their repeat-caller bypass value,
  and whether Corta Spam still holds the dialer role. Report 4 is unexplained without those.

## Verify

```bash
bash tools/verify.sh
```

Green as of 2026-09-16 (779 tests, iOS compile included). Device halves (`ring_test.sh`,
`blocked_call_test.sh`) were last run green on an API 36 AVD on 2026-09-02 — **not re-run since**,
see Next action.

## Open questions

- **iOS parity was scoped 2026-09-16, not started.** All four 2026-09-15 changes are Android-only
  or unreachable on iOS — `CallScreen` (shared) is only ever instantiated by Android's
  `InCallActivity`, and CallKit gives a third-party app no in-call phase or custom UI for a regular
  cellular call at all, so the full-screen gate and "hang up & block" have no iOS equivalent to
  build. `AutoResponderScreen` is already live in iOS's nav graph with zero effect (pre-existing,
  not from this session). Real parity ceiling: a `CXCallDirectoryProvider` extension target +
  App Group entitlement (neither exists yet — `iosApp/project.yml` has one target) feeding the
  existing `RuleRepository`, for block/identify only. Auto-responder and custom call UI are not
  portable to regular iOS calls under Apple's model — only reachable by rebuilding as a VoIP app
  (`CXProvider`/PushKit), a different product with its own recording-consent App Store questions.
- **Nothing from this session has run on real hardware.** The emulator cannot settle OEM ringing, and
  HyperOS is where the Do Not Disturb report came from.
- **The contact matcher's one hole is documented, not fixed** — a contact saved without a country code
  is matched by the same national number under any known foreign dialling code (`611998877` is met by
  `+51611998877`). Pinned by `ContactMatchingStressTest`. It defeats every rule below step 2. Closing it
  needs a home-region read (expect/actual over SIM/locale); two of the three original objections are now
  stale — `Country` carries ISO regions, and the trunk-prefix rule can be sidestepped by emitting both
  candidates. **Decide whether to close it before 1.6.2.**
- Release 10 still needs: version bump, `docs/store/RELEASE_NOTES_1.6.2.md` in all four locales, and a
  re-triage of the Play advisories against the new build.
- `docs/STORE_LISTING.md:395` — pt-BR and hi-IN listings still show Spanish screenshots.
- Should the older tracked LinkedIn posts be untracked and the ignore widened to `docs/LINKEDIN_POST_*`?
  Asked 2026-08-29, still unanswered.
- **Rotation still discards every screen's state** (`AdaptiveScaffold.kt:73`) — pre-existing, its own change.
- **The spam-provider toggle is still inert** (`SettingsScreen.kt:125`): `onSetSpamEnabled` is declared
  and never called. Wire the row or delete the feature.

## Do not redo

- **Do not commit `docs/LINKEDIN_POST_1_6_1_ES.md` or its `.docx`.** Explicitly refused 2026-08-29.
- **Do not re-triage the four Play advisories on 9 (1.6.1)** — verdicts and evidence are in
  `docs/PLAY_ADVISORIES.md`.
- **Do not "fix" the ringer by reading `AudioManager.getRingerMode()` under Do Not Disturb.** It reports
  `RINGER_MODE_SILENT` while the user's own `Settings.Global.MODE_RINGER` still reads 2, which silences
  every DND exception. `RingerPolicy.effectiveRingerMode` exists for exactly this.
- **Do not change `NotificationChannel` sound/vibration/bypass in place.** They are frozen at creation;
  it needs a new id plus `deleteNotificationChannel`. That is why the id is `incoming_calls_v2`.
- **Do not expect an *ordinary* contact to ring under Do Not Disturb on the AVD.** The AOSP default is
  `priorityCallSenders=PRIORITY_SENDERS_STARRED`; `ring_test.sh dnd` reads the device policy and stars a
  contact itself. A "silent" result there was the app being right and the test being wrong.
- **Do not trust a device result without checking the call arrived.** The AVD modem wedges after enough
  calls and delivers nothing while `adb emu gsm call` still returns OK; the call-log table is empty. Reboot.
- **Do not shorten `GREETING_MAX_MILLIS`** to reduce auto-responder mic exposure. It is only reached when
  the TTS completion callback is lost, and it would truncate legitimately long recorded greetings. The
  mute after the greeting is the fix.
