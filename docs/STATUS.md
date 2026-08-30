# STATUS — Corta Spam

_Last updated: 2026-08-29 · branch `main` · 1 uncommitted file (`.gitignore`)_

## Next action

Ship the keypad caret fix (`e274f7e`, on `main`, unreleased) as **version code 10** — bump the two
vals in `androidApp/build.gradle.kts`, write `docs/store/RELEASE_NOTES_1.6.2.md` in all four
locales, and re-triage the Play advisories against the new build per `docs/PLAY_ADVISORIES.md`.

## State

- **1.6.1 (9) is live in production** — confirmed 2026-08-29. Codes 6–9 are spent; the next build
  takes 10 or higher.
- **The caret fix is the only unreleased code change.** The dial pad now types and deletes at the
  caret (`shared/src/commonMain/kotlin/org/carlospinan/bloqueador/app/keypad/NumberEntry.kt`, nine
  assertions). 740 tests on `main`; build 9 shipped with 731 JVM + 369 iOS.
- **The donation QRs render inline** in `DONATE.md` / `DONATE_ES.md` (`a520916`, pushed): an HTML
  table, both codes at 320 px, full-size files still linked underneath.
- **A dedicated donate page is live**: `https://cpinan.github.io/corta-spam/donate/` — both QRs
  above the fold, Sponsors/Ko-fi/PayPal underneath. It lives in the *other* repo,
  `~/Projects/cpinan.github.io` (`86d4ed9`, pushed, Pages build reports `built`), and the corta-spam
  landing page's two Yape/Plin links now point at it instead of at GitHub.
- **The 1.6.1 LinkedIn post was published 2026-08-29**, in Spanish, with the donate-page link and
  **no attached images** — LinkedIn would not accept uploads in that session, so the post links to
  the QRs rather than showing them.
- The four shipped locales are audited and clean; the store listing is four languages.
- 740 tests, 35 course chapters, 188 quiz questions.

## In flight

- `.gitignore:65-69` — the only uncommitted change. Ignores `docs/LINKEDIN_POST_1_6_1_ES.md` and
  its `.docx`, at the user's instruction: that post stays local and is **not** to be committed.
  The rule is local-only until this file is committed.
- `docs/LINKEDIN_POST_1_6_1_ES.md` + `.docx` — untracked **on purpose**, do not `git add` them.
  The docx is regenerated with
  `pandoc docs/LINKEDIN_POST_1_6_1_ES.md -f gfm -t docx -o docs/LINKEDIN_POST_1_6_1_ES.docx`.
- `CHANGELOG.md:17` — everything from this release cycle sits under `[Unreleased]`; there is still
  no `[1.6.1] (8→9)` heading even though 9 is live. Close it when 10 is cut.
- `docs/LINKEDIN_POST_ES.md` — the original launch post, still carrying two `[NOMBRE]` /
  `[QUÉ HIZO]` placeholders. Tracked, unlike the 1.6.1 one.
- `docs/STORE_LISTING.md:395` — pt-BR and hi-IN listings still inherit the default language's
  graphics, so both show Spanish screenshots. One emulator run per locale:
  `./scripts/seed_screenshots.sh --locale pt-BR` then `./scripts/play_assets.sh`.
- Reporter follow-up unsent: a drafted Spanish reply asks whether they had *Respuesta automática*
  on, which decides whether they hit the bug or the feature.

## Verify

```bash
bash tools/verify.sh
```

Not run this session — the only changes were documentation, a `.gitignore` entry and a page in
another repo. `./scripts/blocked_call_test.sh --device <emulator> auto` is the device half.

## Open questions

- **Nothing in 1.6.1 has run on physical hardware.** The report came from a Redmi Note 13 Pro on
  Android 16 (HyperOS); a real inbound call needs a second phone.
- **Rotation discards every screen's state.** `AdaptiveScaffold.kt:73` calls `content()` from three
  branches of one `when (windowSizeClass)` (lines 86, 132, 164), so a size-class change moves every
  screen to a different composition slot and drops its `remember`/`rememberSaveable`. Pre-existing.
  Fix is `movableContentOf` plus a pass over every screen: its own change, its own verification.
- **The spam-provider toggle is inert.** `SettingsScreen.kt:125` declares `onSetSpamEnabled` and the
  body never calls it. `AppNavHost.kt:423`, the intent, the repository and
  `EvaluateIncomingCallUseCase` are all wired and tested. Wire the row, or delete the feature.
- **Eight dead string keys × 4 locales**: `action_back`, `call_log_just_now`, `nav_settings`,
  `schedule_hour_hint`, `schedule_minute_hint`, `schedule_invalid_time`, `stats_blocked_count`,
  `call_log_time_format`. The `settings_spam_provider` pair stays: it is the evidence above.
- Should the older tracked LinkedIn posts be untracked too, and the ignore widened to
  `docs/LINKEDIN_POST_*`? Asked 2026-08-29, unanswered.
- Whether to generate pt-BR and hi-IN screenshots before the next release.
- **AGP 9 / Gradle 9 is blocked**, not abandoned — see "Do not redo".

## Do not redo

- **Do not commit `docs/LINKEDIN_POST_1_6_1_ES.md` or its `.docx`.** Explicitly refused
  2026-08-29; they are gitignored for that reason, not by accident.
- **Do not re-triage the four Play advisories on 9 (1.6.1).** Verdicts and evidence are in
  `docs/PLAY_ADVISORIES.md`, each with the condition that would overturn it: edge-to-edge is
  already done, the deprecated-API traces are library-internal, PiP is declined, AGP 9 deferred.
- **The "deprecated window APIs" advisory cannot be cleared from this codebase.** Its traces
  resolve, against the uploaded bundle's `mapping.txt`, to `androidx.activity.EdgeToEdgeApi26/29`
  and an R8 outline inside `enableEdgeToEdge()` — the call Google's own advisory recommends.
  Upgrading androidx.activity does not help (checked at 1.13.0).
- **Do not read an obfuscated Play trace as ours without `mapping.txt`.** R8 names are not stable
  across builds, and a synthetic outline is named after whichever class it was *first* outlined from.
- **Picture-in-picture is declined.** No video, and the one full-screen surface is a call screen.
- **AGP 9 needs Gradle 9 first, and Gradle 9.3 fails to configure with either AGP**: it pins
  `org.jetbrains:annotations` to `strictly 13.0` while the Android plugin classpath wants 23.0.0.
  Trialled and reverted 2026-08-27. A `resolutionStrategy` force is the likely fix, as its own change.
- **Do not assert emulator call behaviour by polling `dumpsys telecom`.** Read the Historical Events
  block instead, filtered by a `TC@id` taken before the call.
- **Do not answer a test call with `KEYCODE_CALL`** — it redials. Use `KEYCODE_HEADSETHOOK`.
- **Do not record a version code as unspent.** A code is spent on upload, and an upload leaves no
  local trace.
- **Do not launch the emulator as bare `emulator -avd …`** from a project directory. Use
  `/Users/carlospinan/Library/Android/sdk/emulator/emulator`.
- **Do not conclude a UI defect seen next to a fresh fix is a regression.** Stash the fix, rebuild,
  install, and look again.
- **Do not hand LinkedIn a post whose text promises attached images** unless they are actually
  attachable — link the donate page instead, which is why that page exists.
