# STATUS — Corta Spam

_Last updated: 2026-08-28 · branch `main` · working tree clean_

## Next action

Watch the Play Console for the 1.6.1 (9) review result — uploaded to production on 2026-08-27, no
verdict as of 2026-08-28. The keypad caret fix (`e274f7e`) is on `main` and unreleased; it needs
version code **10** whenever the next release goes out.

## State

- **Codes 6, 7, 8 and 9 are all spent.** 1.6.1 (9) is in review; 1.6.0 (8) has been live at 100%
  since 2026-08-21. The next build takes 10 or higher.
- **1.6.1 answers the blocked-call report**: `Call.reject()` only applies to a ringing call, so
  anything that answered first turned blocking into a silent no-op. `BlockedCallPolicy` picks the
  action from the call state, a watchdog verifies it, and an unknown state hangs up. The
  auto-responder can no longer hold a call open (three silent TTS exits closed, 10s/60s deadlines).
- **The dial pad types at the caret now** (`e274f7e`, unreleased). The field was a `String`, which
  carries no caret, so the pad, `+` and delete could only append or drop the last character —
  a country code could not be inserted in front of a number already typed. Arithmetic lives in
  `shared/src/commonMain/kotlin/org/carlospinan/bloqueador/app/keypad/NumberEntry.kt`, tested by
  nine assertions; the composable keeps three adapters. A/B'd on an API 36 AVD against both binaries.
- **The Console's four advisories against 9 (1.6.1) are triaged and need no code**, written up with
  their evidence in `docs/PLAY_ADVISORIES.md` — including the `mapping.txt` recipe for resolving an
  obfuscated trace and a re-triage checklist for release 10.
- **The four shipped locales are audited and clean** (2026-08-27). Key parity holds across all three
  resource trees, and no user-facing string is hardcoded: every suspicious Kotlin literal is a log
  line, a JSON field name, a brand name, a symbol, or country data that resolves through
  `platformCountryName`. Credits contributions and the backup example JSON are English on purpose.
- **The store listing is four languages**, each with its own name; the launcher label stays
  `Corta Spam` everywhere, deliberately.
- 740 tests, 35 course chapters, 188 quiz questions.

## In flight

- `docs/LINKEDIN_POST_ES.md` + `.docx` — committed at last, still a draft: two `[NOMBRE]` /
  `[QUÉ HIZO]` placeholders near the end, plus the donation-links question below.
- Reporter follow-up unsent. A drafted Spanish reply asks the one open question — whether they had
  *Respuesta automática* switched on — which decides whether they hit the bug or the feature.
- `docs/STORE_LISTING.md:395` — pt-BR and hi-IN listings inherit the default language's graphics, so
  both show Spanish screenshots. Fix is one emulator run per locale:
  `./scripts/seed_screenshots.sh --locale pt-BR` then `./scripts/play_assets.sh`, needing the debug
  build reinstalled. No pt/hi feature graphic exists at all.

## Verify

```bash
bash tools/verify.sh
```

Not run this session — the only changes were documentation. `./scripts/blocked_call_test.sh
--device <emulator> auto` is the device half; it needs an emulator and reboots it when the virtual
modem wedges.

## Open questions

- **Play review of 1.6.1 (9), submitted 2026-08-27.** No result yet.
- **Nothing in 1.6.1 has run on physical hardware.** The report came from a Redmi Note 13 Pro on
  Android 16 (HyperOS). A Pixel 10 Pro XL is attached to this machine; a real inbound call needs a
  second phone.
- **Rotation discards every screen's state.** `AdaptiveScaffold.kt:73` calls `content()` from three
  branches of one `when (windowSizeClass)` (lines 86, 132, 164), so a size-class change moves every
  screen to a different composition slot and drops its `remember`/`rememberSaveable`. Pre-existing —
  the pre-fix binary loses the keypad number identically. Fix is `movableContentOf` plus a pass over
  every screen: its own change, its own verification.
- **The spam-provider toggle is inert.** `SettingsScreen.kt:125` declares `onSetSpamEnabled` and the
  body never calls it, so no user can switch it on. `AppNavHost.kt:423`, the intent, the repository
  and `EvaluateIncomingCallUseCase` are all wired and tested. Wire the row, or delete the feature.
- **Eight dead string keys × 4 locales**: `action_back`, `call_log_just_now`, `nav_settings`,
  `schedule_hour_hint`, `schedule_minute_hint`, `schedule_invalid_time`, `stats_blocked_count`, and
  `call_log_time_format` (dead by design — `formatCallTimestamp` replaced it). The
  `settings_spam_provider` pair is left in place deliberately: it is the evidence for the finding above.
- Should `LINKEDIN_POST_ES.md` carry donation links? The 1.6.0 post carries all three.
- Whether to generate pt-BR and hi-IN screenshots before the next release.
- **AGP 9 / Gradle 9 is blocked**, not abandoned — see "Do not redo".

## Do not redo

- **Do not re-triage the four Play advisories on 9 (1.6.1).** Verdicts and evidence are in
  `docs/PLAY_ADVISORIES.md`, each with the condition that would overturn it. Short version:
  edge-to-edge is already done, the deprecated-API traces are library-internal, PiP is declined,
  AGP 9 is deferred.
- **The "deprecated window APIs" advisory cannot be cleared from this codebase.** Its traces
  `c.w.b`, `c.y.b` and `a4.b.t` resolve, against the uploaded bundle's `mapping.txt`, to
  `androidx.activity.EdgeToEdgeApi26.setUp`, `EdgeToEdgeApi29.setUp` and an R8 outline called from
  `EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode` — the inside of `enableEdgeToEdge()`, which is
  the call Google's own advisory recommends. Nothing in `androidApp/` or `shared/` touches those
  APIs. Upgrading androidx.activity does not help (checked at 1.13.0).
- **Do not read an obfuscated Play trace as ours without `mapping.txt`.** R8 names are not stable
  across builds — check the mapping's mtime against the upload date — and a synthetic outline is
  named after whichever class it was *first* outlined from, which is why `a4.b` says
  `androidx.emoji2.text.ConcurrencyHelpers$…` while its caller is in `androidx.activity`.
- **Picture-in-picture is declined.** No video, and the one full-screen surface is a call screen that
  must not shrink into a corner.
- **AGP 9 needs Gradle 9 first, and Gradle 9.3 fails to configure with either AGP**: it pins
  `org.jetbrains:annotations` to `strictly 13.0` for its embedded Kotlin while the Android plugin
  classpath wants 23.0.0. Trialled and reverted 2026-08-27. A `resolutionStrategy` force on the
  buildscript classpath is the likely fix — as its own change, with its own verification.
- **Do not assert emulator call behaviour by polling `dumpsys telecom`.** One dump costs most of a
  second against calls decided in one; it reported a call rejected at 0.9s as ringing for fifteen.
  Read the Historical Events block instead, filtered by a `TC@id` taken before the call.
- **Do not answer a test call with `KEYCODE_CALL`** — it redials when there is nothing to answer,
  and that outgoing call looks exactly like the bug. Use `KEYCODE_HEADSETHOOK`.
- **Do not record a version code as unspent.** Three times running the belief was wrong. A code is
  spent on upload, and an upload leaves no local trace.
- **Do not launch the emulator as bare `emulator -avd …`** from a project directory — it resolves
  `../emulator/lib64/qt` relative to the cwd and dies. Use
  `/Users/carlospinan/Library/Android/sdk/emulator/emulator`.
- **Do not conclude a UI defect seen next to a fresh fix is a regression.** Stash the fix, rebuild,
  install, and look again — that is what showed the rotation state loss is pre-existing.
