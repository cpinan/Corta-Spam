# STATUS — Corta Spam

_Last updated: 2026-08-27 (later) · branch `main` · 2 uncommitted files (a LinkedIn draft + its .docx)_

## Next action

Watch the Play Console for the 1.6.1 (9) review result — it was uploaded to production on
2026-08-27 and nothing here proceeds until it lands.

## State

- **1.6.1 (versionCode 9) was uploaded to production on 2026-08-27.** 1.6.0 (8) had been live at
  100% since 2026-08-21. **Codes 6, 7, 8 and 9 are all spent** — the next build takes 10 or higher.
- **The release answers one bug report**: with the default action set to Block, the app showed
  "Blocked call" and then the call answered itself. `Call.reject()` only applies to a ringing call,
  so anything that answered first turned it into a silent no-op. `BlockedCallPolicy` now picks the
  action from the call state and a watchdog verifies it happened; an unknown state hangs up.
- **The auto-responder can no longer hold a call open.** Three text-to-speech paths reported nothing
  at all (failed init left non-null, missing voice for the device language, `speak()` returning
  ERROR); all three now report completion, backed by a 10s no-sound and 60s never-finished deadline.
- **Switching the auto-responder on is confirmed through a dialog** in all four locales. The default
  was already off and still is.
- **The store listing is four languages now** — pt-BR and hi-IN listings were created 2026-08-27,
  and every locale has its own name (`Corta Spam: Call Blocker` and so on). The launcher label stays
  `Corta Spam` everywhere, deliberately.
- **`scripts/blocked_call_test.sh` is new** and is the only thing in this repo that can reach a
  blocked call which something else answered. It asserts from Telecom's Historical Events, never by
  polling.
- 731 tests, 34 course chapters, 183 quiz questions. `android.r8.optimizedResourceShrinking=true`
  took the bundle from 5.43 MB to 4.95 MB.

## In flight

- `docs/LINKEDIN_POST_ES.md` (untracked) + `docs/LINKEDIN_POST_ES.docx` — two `[NOMBRE]` /
  `[QUÉ HIZO]` placeholders near the end, and the donation-links question below. Untouched this
  session.
- Reporter follow-up unsent. A drafted Spanish reply asks the one open question — whether they had
  *Respuesta automática* switched on — which decides whether they hit the bug or the feature.
- `docs/STORE_LISTING.md:395` — pt-BR and hi-IN listings inherit the **default language's**
  graphics, so both currently show Spanish screenshots. Fix is one emulator run per locale:
  `./scripts/seed_screenshots.sh --locale pt-BR` then `./scripts/play_assets.sh`, needing the debug
  build reinstalled. No pt/hi feature graphic exists at all.

## Verify

```bash
bash tools/verify.sh
```

Green this session, including `--release`. `./scripts/blocked_call_test.sh --device <emulator> auto`
is the device half; it needs an emulator and reboots it when the virtual modem wedges.

## Open questions

- **Play review of 1.6.1 (9), submitted 2026-08-27.** No result yet.
- **Nothing in 1.6.1 has run on physical hardware.** The report came from a Redmi Note 13 Pro on
  Android 16 (HyperOS) — the one platform whose Telecom behaviour actually prompted the fix. A
  Pixel 10 Pro XL is attached to this machine; a real inbound call needs a second phone.
- **Did the reporter have the auto-responder on?** Unanswered, and it decides whether they saw the
  bug or the feature working as designed.
- Should `LINKEDIN_POST_ES.md` carry donation links? The 1.6.0 post carries all three.
- **AGP 9 / Gradle 9 is blocked**, not abandoned — see "Do not redo".
- Whether to generate pt-BR and hi-IN screenshots before the next release.

## Do not redo

- **Do not try to clear the Play "deprecated window APIs" advisory.** `setStatusBarColor`,
  `setNavigationBarColor` and `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` are called unconditionally
  by `androidx.activity`'s own `EdgeToEdgeApi35.setUp` — disassembled at 1.13.0 on 2026-08-27 to
  confirm — which is the API Google's own advisory recommends calling. Upgrading the library does
  not help. Nothing this app writes removes them short of dropping edge-to-edge below API 35.
- **Edge-to-edge itself is already done**: both activities call `enableEdgeToEdge()` and every
  screen pads with `safeDrawing`. Verified on an API 36 emulator.
- **Picture-in-picture is declined.** No video, and the one full-screen surface is a call screen
  that must not shrink into a corner.
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
