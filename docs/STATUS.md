# STATUS — Corta Spam

_Last updated: 2026-10-09 · branch `main` · 6 modified + 4 new source files uncommitted (backup export, add-contact account), plus `docs/store/PASTE_1.7.0.md` and 5 `.claude/skills/` dirs, not ours_

## Next action

Run `bash tools/verify.sh`, then commit the uncommitted backup-export and add-contact work as separate
one-purpose commits with README + README_ES + a course chapter each (see In flight). 11 (1.7.0) is in
Play review since 2026-10-08; when it resolves, re-triage the four advisories (`docs/PLAY_ADVISORIES.md`).

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

- **Uncommitted, built, unit-tested, installed on the razr (debug) — `verify.sh` NOT run, README/course NOT updated:**
  - Backup export now saves a `cortaspam-backup-<yyyyMMdd-HHmmss>.json` via `CreateDocument`; import picker
    accepts `*/*` (old `.txt` exports). Was: share-as-`text/plain` -> `.txt`, import filter JSON-only, so the
    app could not read its own export. `androidApp/.../MainActivity.kt` (`exportFileLauncher`,
    `importFileLauncher`), `BackupFileName.kt`, `BackupFileNameTest.kt`; removed
    `share_backup_chooser_title` from 4 `values*/strings.xml`.
  - Add-contact intent preselects the Google account holding most contacts
    (`AddContactIntent.kt`, `PickSyncAccountTest.kt`; used by `MainActivity.addContact` and
    `InCallActivity.addContact`). Not yet seen working in the editor UI on the razr.
- **Original report unresolved: "number is in my agenda but shows no name" on the motorola razr.** Pixel
  (58281FDCQ005US) just has `READ_CONTACTS` denied (`granted=false`). On the razr (ZY22JZJDNH, was 1.6.2)
  permission was granted but none of the bare call-log numbers (`961710293`, `017066208`, `913934060`,
  `906029213`, `016219313`, `901420967`, `+447424819612`) exist in its contacts provider; `961710293` is an
  explicit allowlist entry. Razr now runs the **debug** build (release uninstalled 2026-10-09, rules were
  exported to Downloads `cortaspam.txt`/`.json`); import of that file not confirmed. Next: ask which
  number is "in the agenda" as saved, then compare against `PhoneNumberParser.comparisonKeys`. Possible
  gap: contact saved `+00971…` vs call `+971…` (`toE164OrNull` strips `00`, but `Uber Dubai Waseem` is
  `+00971…` — unverified).
- **Denied-twice contacts permission:** the button at `MainActivity.kt` (`onRequestContactsPermission`)
  re-launches a system dialog that Android no longer shows; should open app settings. Offered, not built.
- **Open user report, unconfirmed root cause: ringing call lost the full-screen UI and the
  notification has no hang-up/block actions.** Two candidates: (1) `USE_FULL_SCREEN_INTENT` revoked;
  (2) `IncomingCallNotifier.post()` (`androidApp/.../telecom/IncomingCallNotifier.kt:431`) swallowing a
  `RuntimeException`. Next: `adb logcat -s IncomingCallNotifier` during a real call; memory
  `incoming_call_ui_regression`.

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

- **Do not try to back up the release app's data over adb.** `allowBackup=false`, not debuggable: no `run-as`,
  no `adb backup`. Only the in-app Backup screen exports it, and uninstall wipes the rest.
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
