# STATUS — Corta Spam

_Last updated: 2026-09-18 · branch `main` · 0 uncommitted files (5 untracked `.claude/skills/` dirs, not ours)_

## Next action

**10 (1.6.2) is still in Play review** (submitted 2026-09-16) — nothing to do until the Console
resolves it. Separately: diagnose the ringing-call UI/notification report below once the device is
back — that is the next piece of actual work.

## State

- **10 (1.6.2) uploaded to production 2026-09-16, versionCode 10 spent.** Still in review as of
  2026-09-18. 9 (1.6.1) is the last confirmed-live build.
- **Shipped this session: "add to contacts" from a live call.** `CallScreen` offers "Create new
  contact" (active/dialing/other) whenever the caller has no resolved `displayName`, wired through
  `InCallActivity.addContact` reusing the keypad's own `ContactsContract.Intents.Insert` intent.
  Commit `046ec82`. README/README_ES/course chapter 38 all updated. No device verification yet —
  build/tests/lint only, per the standing no-install-without-asking rule.
- **781 tests** (`:shared` 668, `:androidApp` 113), 38 course chapters.
- iOS still has no call blocking — pending the CallDirectory extension (unchanged).

## In flight

- **Open user report, unconfirmed root cause: ringing call lost the full-screen UI and the
  notification has no hang-up/block actions.** No device attached this session to reproduce.
  Two candidate causes, not yet distinguished — see project memory
  `incoming_call_ui_regression` for full detail:
  1. `USE_FULL_SCREEN_INTENT` special access revoked in system Settings for this app (Android
     14/15 can auto-reset it if unused).
  2. `IncomingCallNotifier.post()` (`androidApp/.../telecom/IncomingCallNotifier.kt:431`)
     deliberately swallows `RuntimeException` from `notify()` — if the ringing `CallStyle`
     notification is being rejected by the platform, this catch would explain both missing
     symptoms at once (same class of bug already hit once for the ongoing-call notification).
  **Next step, once phone is plugged in:** `adb logcat -s IncomingCallNotifier` during a real
  incoming call. A logged "Notification 1001 was rejected by the platform and has been dropped"
  confirms cause #2 with a stack trace; nothing logged points at cause #1 (check the Settings
  toggle first).

## Verify

```bash
bash tools/verify.sh
```

Green as of 2026-09-18 (781 tests, iOS compile included).

## Open questions

- **10 (1.6.2) is in Play review, submitted 2026-09-16.** Outcome not yet known — see
  `docs/RELEASE.md`.
- Release 11 still needs: re-triage the four Play advisories against *that* build's `mapping.txt`.
- `docs/STORE_LISTING.md:395` — pt-BR and hi-IN listings still show Spanish screenshots; those
  languages still aren't added to the store listing itself.
- Reporter follow-up still unsent: ask for their *Acción por defecto*, their repeat-caller bypass
  value, and whether Corta Spam still holds the dialer role. Report 4 is unexplained without those.
- Should the older tracked LinkedIn posts be untracked and the ignore widened to
  `docs/LINKEDIN_POST_*`? Asked 2026-08-29, still unanswered.

## Do not redo

- **Do not try `ndk.debugSymbolLevel=FULL` again for the native-debug-symbols warning.** Confirmed
  it produces an empty `extractReleaseNativeDebugMetadata` output — `androidx.graphics.path`'s
  `.so` ships stripped with nothing embedded to recover.
- **Do not commit `docs/LINKEDIN_POST_1_6_1_ES.md` or its `.docx`.** Explicitly refused 2026-08-29.
- **Do not re-triage the four Play advisories from scratch** — verdicts and evidence are in
  `docs/PLAY_ADVISORIES.md`, re-confirmed against build 10 on 2026-09-16.
- **Do not "fix" the ringer by reading `AudioManager.getRingerMode()` under Do Not Disturb.** It
  reports `RINGER_MODE_SILENT` while `Settings.Global.MODE_RINGER` still reads 2, which silences
  every DND exception. `RingerPolicy.effectiveRingerMode` exists for exactly this.
- **Do not change `NotificationChannel` sound/vibration/bypass in place.** They are frozen at
  creation; needs a new id plus `deleteNotificationChannel`. That is why the id is
  `incoming_calls_v2`.
- **Do not expect an *ordinary* contact to ring under Do Not Disturb on the AVD.** The AOSP default
  is `priorityCallSenders=PRIORITY_SENDERS_STARRED`; `ring_test.sh dnd` stars a contact itself.
- **Do not trust a device result without checking the call arrived.** The AVD modem wedges after
  enough calls and delivers nothing while `adb emu gsm call` still returns OK. Reboot.
