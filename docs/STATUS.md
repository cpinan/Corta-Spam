# STATUS — Corta Spam

_Last updated: 2026-10-14 · branch `main` · 0 source files uncommitted (7 `.claude/skills/` dirs, not ours)_

## Next action

Confirm which search the user meant (keypad contact-match popup vs on-screen keyboard covering the agenda
list), then fix it; before that, get an OK to uninstall the emulator's release build and run
`./scripts/stall_test.sh auto --device emulator-5558`.

## State

- **11 (1.7.0) is in Play review since 2026-10-08, versionCode 11 spent.** Bundle
  `androidApp/build/outputs/bundle/release/11-1.7.0-release.aab`. Status of 10 (1.6.2) unknown to this repo.
- **Pushed 2026-10-14 (not in 1.7.0, so unreleased):** backup export as timestamped `.json` + import `*/*`
  (`3a2e11a`); add-contact preselects the Google account (`4f521ca`); agenda haptic tick (`97888f7`);
  "Waste their time" button on the ringing screen (`1ddae1b`). `main` now tracks `origin/main`.
- **804 tests** (`:shared` 679, `:androidApp` 125), 43 course chapters, `verify.sh` green on the final tree.
  None of the four features has been run on a real phone.
- Stall design: `StallPolicy` (cap 5 min, 3-7 s gaps, two silent lines end it), `StallLines`, loop in
  `PassthroughInCallService.startStall/runStall`, button via `CallScreen(onStall)`. Android only; hidden with
  another call live; refuses before rules decide and during emergency callback.
- iOS still has no call blocking — pending the CallDirectory extension.

## In flight

- `scripts/stall_test.sh` — written and `bash -n` checked, never run. Needs the DEBUG build on
  `emulator-5558`; the emulator has a release-signed build, so `adb uninstall org.carlospinan.cortaspam`
  is required first (wipes its data; the harness denied it, so ask the user).
- **Agenda search overlap — not fixed, target unclear.** `AgendaScreen.kt` has no dropdown. Candidates: the
  keypad's contact-match popup (`keypad/KeypadScreen.kt`), or the keyboard covering the list because
  `adaptive/AdaptiveScaffold.kt` pads `safeDrawing` Top+Horizontal only (no `imePadding`). Needs a screenshot.
- `AddContactIntent.kt` `readContactAccounts` scans all raw contacts on the main thread; suspected
  stall, not timed.
- Stall button is hidden for another live call but not for an emergency callback (guard only refuses).
- **Original report unresolved: contact "in my agenda" shows no name on the motorola razr** (ZY22JZJDNH,
  debug build now; bare call-log numbers `961710293`, `017066208`, `913934060`, `906029213`,
  `016219313`, `901420967`, `+447424819612` absent from its contacts provider). Next: ask which number is
  "in the agenda" as saved, compare with `PhoneNumberParser.comparisonKeys`; possible `+00971…` vs `+971…`.
- **Denied-twice contacts permission:** `MainActivity.kt` `onRequestContactsPermission` re-launches a
  dialog Android no longer shows; should open app settings. Offered, not built.
- **Open report, cause unconfirmed: ringing call lost the full-screen UI and notification actions.**
  Candidates: `USE_FULL_SCREEN_INTENT` revoked; `IncomingCallNotifier.post()`
  (`telecom/IncomingCallNotifier.kt:431`) swallowing a `RuntimeException`. Next:
  `adb logcat -s IncomingCallNotifier` on a real call.

## Verify

```bash
bash tools/verify.sh
```

Green as of 2026-10-14 (804 tests, iOS compile included).

## Open questions

- Does the stall voice reach a real caller audibly? Only a call to a real phone answers it.
- Outcome of 10 (1.6.2) and 11 (1.7.0) reviews — `docs/RELEASE.md` says stage built-not-uploaded and
  needs updating to production-review.
- Were the new screenshots uploaded to the listing? pt-BR and hi-IN still reuse the Spanish set and are
  not added to the store listing at all (`docs/STORE_LISTING.md:395`).
- Kris Gead's credit has no contribution line — ask the maintainer what to write.
- Reporter follow-up still unsent: their *Acción por defecto*, repeat-caller bypass value, and whether
  Corta Spam still holds the dialer role (more relevant now that the default changed).
- Should older tracked LinkedIn posts be untracked? Asked 2026-08-29, unanswered.

## Do not redo

- **Do not try to back up the release app's data over adb.** `allowBackup=false`, not debuggable: no `run-as`,
  no `adb backup`. Only the in-app Backup screen exports it, and uninstall wipes the rest.
- **No audio injection into calls on Android or iOS** — stall speaks via loudspeaker into the mic; do not
  look for a Telecom API for it.
- **`&apos;` is invalid in `androidApp` strings** (aapt) — use `\'`.
- **Android `Account` is a stub in JVM unit tests** (fields null) — keep `pickSyncAccount` returning a Pair.

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
