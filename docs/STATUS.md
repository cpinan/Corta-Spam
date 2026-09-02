# STATUS — Corta Spam

_Last updated: 2026-09-02 · branch `main` · 0 uncommitted files (5 untracked `.claude/skills/` dirs, not ours)_

## Next action

Run the 2026-09-02 fixes on **physical hardware** — `./scripts/ring_test.sh watch --device <serial>`
with Do Not Disturb on, called from a second phone. Everything this session was verified on an API 36
emulator only, and the original report came from a Redmi Note 13 Pro on HyperOS.

## State

- **1.6.1 (9) is live in production.** Codes 6–9 are spent; the next build takes 10 or higher.
  Version vals are still unbumped — 1.6.1 in `androidApp/build.gradle.kts`.
- **Four user reports handled** (`a4c583b`, `0d237c3`, `cf21606`, `34953c3`, docs `9a951ff`):
  Do Not Disturb is honoured when ringing; the auto-responder mutes the uplink once the greeting
  ends; Compose format args are positional so `%d` no longer reaches the user; the contact matcher
  is stressed with 100 near-miss numbers.
- **762 tests** (`:shared` 649, `:androidApp` 113), 36 course chapters, 194 quiz questions.
- **Unreleased since 1.6.1**: the keypad caret fix (`e274f7e`) plus all four fixes above. Release
  notes for 1.6.2 in four locales are still unwritten.
- iOS still has no call blocking — pending the CallDirectory extension. `verify.sh` compiles
  `commonTest` for Native but **runs no iOS test**; `:shared:iosSimulatorArm64Test` is CI-only.

## In flight

- `androidApp/.../telecom/PassthroughInCallService.kt:250` — `onCallAdded` calls `startActivity(InCallActivity)`
  **unconditionally**, before the rule decision exists. So an unknown/blocked caller still gets the
  full-screen call UI thrown up, and `InCallActivity` is `showWhenLocked` + `turnScreenOn`, meaning a
  spam call lights the screen at 3am even when Do Not Disturb silenced it. Deciding *not* to launch it
  needs care: the launch currently doubles as the reliable path to the in-call UI, and Telecom's
  full-screen intent is the fallback. Also check whether `announce()`'s new silent path should suppress
  it too.
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

Green as of 2026-09-02 (762 tests, iOS compile included). Device halves:
`./scripts/ring_test.sh auto|dnd` and `./scripts/blocked_call_test.sh auto` — all green on an
API 36 AVD this session.

## Open questions

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
