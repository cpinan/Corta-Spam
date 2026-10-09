# STATUS — Corta Spam

_Last updated: 2026-10-08 · branch `main` · 1 untracked file (`docs/store/PASTE_1.7.0.md`, copy-paste helper) + 5 untracked `.claude/skills/` dirs, not ours_

## Next action

**11 (1.7.0) was submitted to Play for review on 2026-10-08** — nothing to do until the Console
resolves it. When it does, re-triage the four Play advisories against its `mapping.txt`
(`docs/PLAY_ADVISORIES.md`). Separately, the ringing-call UI/notification report below still needs a
physical device.

## State

- **11 (1.7.0) submitted to production 2026-10-08, versionCode 11 spent.** Bundle
  `androidApp/build/outputs/bundle/release/11-1.7.0-release.aab`, same upload key as 10. The status
  of 10 (1.6.2, submitted 2026-09-16) is not known to this repo.
- **New in 1.7.0:** a fresh install blocks unmatched numbers by default (existing installs that never
  chose keep ALLOW; `setWelcomeShown` pins the fresh-install value); a once-only first-run Settings
  walkthrough (`SettingsTutorialDialog`, all four locales); add-to-contacts from the call screen;
  Christopher (Kris) Gead in Credits, name only.
- **787 tests** (`:shared` 674, `:androidApp` 113), 39 course chapters. Release (R8) build checked on
  the API 36 emulator from a fresh install: tutorial shows once, no crash. Not run on a physical device.
- Listing screenshots retaken 2026-10-08 (en-US + es-419) in `docs/store/play/`. Whether they were
  swapped in the Console this session was not confirmed — check Store presence → Main store listing.
- iOS still has no call blocking — pending the CallDirectory extension (unchanged).

## In flight

- **Open user report, unconfirmed root cause: ringing call lost the full-screen UI and the
  notification has no hang-up/block actions.** Two candidates: (1) `USE_FULL_SCREEN_INTENT` special
  access revoked in system Settings; (2) `IncomingCallNotifier.post()`
  (`androidApp/.../telecom/IncomingCallNotifier.kt:431`) swallowing a `RuntimeException` from
  `notify()`. Next step with a phone attached: `adb logcat -s IncomingCallNotifier` during a real
  incoming call; project memory `incoming_call_ui_regression` has the detail.

## Verify

```bash
bash tools/verify.sh
```

Green as of 2026-10-08 (787 tests, iOS compile included).

## Open questions

- Outcome of 10 (1.6.2) and 11 (1.7.0) reviews — `docs/RELEASE.md` says stage built-not-uploaded and
  needs updating to production-review.
- Were the new screenshots uploaded to the listing? pt-BR and hi-IN still reuse the Spanish set and are
  not added to the store listing at all (`docs/STORE_LISTING.md:395`).
- Kris Gead's credit has no contribution line — ask the maintainer what to write.
- Reporter follow-up still unsent: their *Acción por defecto*, repeat-caller bypass value, and whether
  Corta Spam still holds the dialer role (more relevant now that the default changed).
- Should older tracked LinkedIn posts be untracked? Asked 2026-08-29, unanswered.

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
