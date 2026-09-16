# STATUS — Corta Spam

_Last updated: 2026-09-16 · branch `main` · 0 uncommitted files (5 untracked `.claude/skills/` dirs, not ours)_

## Next action

Run the three rounds of on-device verification listed below, then upload `10-1.6.2-release.aab` to
Play production — nothing else is blocking the upload.

## State

- **9 (1.6.1) is live in production.** Codes 6–9 are spent.
- **10 (1.6.2) is built and ready, not yet uploaded.** `androidApp/build/outputs/bundle/release/
  10-1.6.2-release.aab`, versionCode 10 confirmed free on the Console before the bump (`220e3cb`).
  Manifest audited against 1.6.1's baseline (7 permissions, unchanged), same signing key
  (`4CA33D30…46E0`), installed and launched clean on a physical device (razr 50 ultra,
  `ZY22JZJDNH`).
- **Release notes written in all four locales** — `docs/store/RELEASE_NOTES_1.6.2.md`, all under
  Play's 500-char cap.
- **The four recurring Play Console "Recommended actions" advisories were re-checked against 10**
  and need no new work — verdicts unchanged from 9, see `docs/PLAY_ADVISORIES.md`.
- **New upload-time warning triaged: "no debug symbols uploaded"** — not fixable from this repo.
  The only native code in the bundle is `androidx.graphics.path`'s prebuilt `.so` (transitive via
  Compose UI), shipped pre-stripped with nothing to extract. Confirmed by trying
  `ndk.debugSymbolLevel=FULL` and observing an empty output, then reverting it. Written up in
  `docs/PLAY_ADVISORIES.md`'s new "Upload-time warnings" section so it isn't re-investigated on 11.
- **779 tests** (`:shared` 666, `:androidApp` 113), 36 course chapters, 194 quiz questions.
- iOS still has no call blocking — pending the CallDirectory extension (unchanged, not in scope
  for 1.6.2).

## In flight

Nothing in flight. 1.6.2's code, notes and bundle are done; what's left is verification and upload,
not development.

## Verify

```bash
bash tools/verify.sh
```

Green as of 2026-09-16 (779 tests, iOS compile included).

## Open questions

- **None of 1.6.2's fixes have run through their actual scenario on physical hardware.** Emulator
  and unit tests only. Three rounds still owed before upload:
  1. `./scripts/ring_test.sh watch --device <serial>` with Do Not Disturb on, called from a second
     phone (Redmi Note 13 Pro / HyperOS, where the original DND report came from).
  2. `./scripts/call_test.sh` / `ring_test.sh` for the full-screen gate and auto-responder trigger,
     plus manually toggling "Notifications" off and confirming the return-to-call notification
     still appears.
  3. "Hang up & block" during real call waiting (a second call arriving while the button is on
     screen), to confirm `blockAndEndAction` leaves the promoted call alone; also exercise the
     call-log/keypad dialog-reopening fixes by tapping an entry, cancelling, switching tabs and
     back.
- Release 11 still needs: re-triage the four Play advisories against *that* build's `mapping.txt`
  (obfuscated names differ per build, per `docs/PLAY_ADVISORIES.md`'s own checklist).
- `docs/STORE_LISTING.md:395` — pt-BR and hi-IN listings still show Spanish screenshots; those
  languages still aren't added to the store listing itself, so their release notes reach no one
  yet.
- `docs/RELEASE.md` (the release-tracking state file `play-release-run` expects) does not exist —
  create it once 10 actually uploads, not before.
- Reporter follow-up still unsent: ask for their *Acción por defecto*, their repeat-caller bypass
  value, and whether Corta Spam still holds the dialer role. Report 4 is unexplained without those.
- Should the older tracked LinkedIn posts be untracked and the ignore widened to
  `docs/LINKEDIN_POST_*`? Asked 2026-08-29, still unanswered.

## Do not redo

- **Do not try `ndk.debugSymbolLevel=FULL` again for the native-debug-symbols warning.** Confirmed
  it produces an empty `extractReleaseNativeDebugMetadata` output — `androidx.graphics.path`'s
  `.so` ships stripped with nothing embedded to recover. Only revisit if that AAR ships unstripped
  in a future version, or this project adds its own NDK code.
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
