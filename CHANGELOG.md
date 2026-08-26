# Changelog

Notable changes to Corta Spam, newest first. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html), with the Play `versionCode` in
parentheses because that is the number an upload is accepted or rejected on.

This file is the scannable index: one line per change, with the commit that made it. The reasoning
— what the bug actually was, why a fix was chosen over its alternatives, and what was deliberately
left alone — lives in **[Recent Fixes](README.md#recent-fixes)** in the README, and in
[`README_ES.md`](README_ES.md#cambios-recientes). Where the two disagree, the README is the source
of truth; this is the table of contents for it.

English only, like [`LEARNINGS.md`](LEARNINGS.md) and [`AGENTS.md`](AGENTS.md) — the user-facing
docs (README, in-app strings, store listing) keep their Spanish parity, developer notes do not.

## [Unreleased]

### Added

- Donation options, in the repository only — [`DONATE.md`](DONATE.md) / [`DONATE_ES.md`](DONATE_ES.md)
  list Ko-fi, PayPal and Yape/Plin, plus the free ways to help that are worth more than
  any of them. `.github/FUNDING.yml` drives the repo's Sponsor button, with every platform but
  `custom` commented out until the corresponding account exists — a donate button that 404s is worse
  than no button.
- **Neither GitHub Sponsors nor Liberapay is offered**, and both rejections come down to the same
  missing piece. Sponsors wants a Stripe Connect account whose country matches the bank account's;
  its bank-free alternative is a fiscal host (Open Source Collective, 10%) that can only be chosen
  at signup. Liberapay on PayPal makes the donor confirm every single payment, so its recurrence is
  nominal, and it exposes donor and recipient names and emails to each other. Reasoning is recorded
  in both DONATE files and in `.github/FUNDING.yml`.
- Yape and Plin QR images in [`docs/donate/`](docs/donate/), trimmed, downscaled to 760 px and
  palette-quantised — 340 KB of phone screenshots down to 69 KB. Both were **decoded before being
  committed**, not trusted: each is an EMVCo payload carrying an opaque account token, and neither
  encodes a phone number (checked against the Peruvian mobile pattern with and without the country
  code). Each re-decoded to an identical byte count after processing, so no step broke scannability.
  [`docs/donate/README.md`](docs/donate/README.md) carries the decoder script for re-checking a
  replacement.
- A "Support this project" / "Apoya el proyecto" section in both READMEs, linking to the above.

### Changed

- The repo URL in both Play full descriptions is now written `https://github.com/cpinan/Corta-Spam`
  rather than bare, and each `OPEN SOURCE` paragraph says what else the repository holds. Character
  counts recomputed: es-419 3,609, en-US 3,323, both under the 4,000 limit.
- A GitHub social preview card, [`docs/store/social_preview_1280x640.png`](docs/store/), rebuilt from
  the English feature graphic at GitHub's exact 1280×640 and padded with the graphic's own background
  so the seam is invisible (Spanish variant kept as `_es`). Without one, every share of the repo
  renders a grey Octocat tile instead of the app's name.
- [`docs/STORE_LISTING.md`](docs/STORE_LISTING.md) records where the repo link actually appears in
  the Console — the **Website** field is the only always-visible one, since Play truncates the full
  description behind *Read more* and does not hyperlink URLs inside it — and why donation links stay
  out of the listing and out of the app.

## [1.6.0] (8) — 2026-08-21, internal testing

Release notes: [`docs/store/RELEASE_NOTES_1.6.0.md`](docs/store/RELEASE_NOTES_1.6.0.md).
Bundle `8-1.6.0-release.aab` built 2026-08-21, **5,429,071 bytes**,
`sha256 5cdfdabbc183701a8ed196a95060d6a57af76a7fb5b6211416e9993a5e01e5c3`.

**Artifacts are named `<versionCode>-<versionName>-<buildType>` from this release on.** The code
leads because it is the field an upload is accepted or rejected on, and the one that may only ever
increase — a folder of these sorts into upload order by filename alone, which the old
`corta-spam-<name>-<code>` form did not. Earlier artifacts keep their old names: renaming them
would falsify the record of what was built under which filename.

**Code 8, because 7 was refused at upload.** This release was built on code 7 first, on this
file's own claim that 7 had never been uploaded. Play answered `Version code 7 has already been
used`. So it had been — and nothing local could have known, because a code is spent on upload and
an upload leaves no trace on the machine that built the artifact. It is the same mistake this file
made about code 6 four days earlier, and the correction is the same: a code is *unknown* until the
Console says otherwise, never *unspent*.

Both code-7 bundles, 1.5.0 (7) and 1.6.0 (7), are in `superseded/` and can never be uploaded. The
name stayed at 1.6.0 because nothing about the app changed between the two builds.

The dialer half of the app, rebuilt from user feedback: the keypad looked like it had a rendering
fault, and there was no screen that simply listed the people in the phone. Verified on a razr 50
ultra with a real 940-contact address book and on a Pixel 8 Pro API 36 emulator.

### Added

- **An Agenda tab: the address book, read through this app's rules.** Search, a favourites strip
  from the platform's own `STARRED` flag, pull-to-refresh, and filter chips for All / Starred /
  Blocked / Allowed. The last two answer the question the block list cannot — it stores numbers, so
  "which of the people I know have I silenced" had no screen — matched with `sameNumber`, so a rule
  saved from a national-format call still matches a card saved internationally. Tapping a row opens
  the same Call / Block / Allow / Copy actions the call log offers, one button per meaning.
  ([`3a028f5`](../../commit/3a028f5))
- **Settings left the navigation bar to make room, and gained a way in.** Material's bar tops out
  at five items. Settings is reached from a gear in Home's header, because Home's existing text
  link is the last of four below the fold — a destination whose only route is off screen is one the
  user has to already know about. `routeSection` returns `NO_SECTION` for those routes rather than
  falling back to Home. ([`3a028f5`](../../commit/3a028f5), [`96fd70f`](../../commit/96fd70f))
- **Pull to refresh the Agenda and the call log**, past the address book's five-minute cache. On
  the call log the calls need no refreshing — they arrive as a database flow — but the contact names
  beside them do. ([`4a3b814`](../../commit/4a3b814))
- **The keypad's empty band now holds the people most likely to be dialled**: starred contacts, or
  the four most recent callers when nothing is starred, or one line saying what will appear there.
  A tap fills the number rather than dialling. ([`e13a3d3`](../../commit/e13a3d3),
  [`a1f3a30`](../../commit/a1f3a30))
- **A setting for that strip.** Recent callers are call history on the screen a phone gets handed
  to someone on, so `showRecentCallersOnKeypad` gates them — on by default, and not covering starred
  contacts, which are a choice the user made. Off, the recents leave the ViewModel state rather than
  being hidden by the view. ([`2a7c9ea`](../../commit/2a7c9ea))

### Fixed

- **The keypad reserved 132 dp of blank space under its search box.** It was the fix for the moving
  dial pad wearing the wrong shape: a constant gap keeps the keys still, and on an empty query
  nothing is ever drawn in it. The results now float in a `Popup` — a window of its own, outside the
  layout — the number field sits on the pad where a dialer puts it, and the column packs from the
  bottom. ([`e49fb8d`](../../commit/e49fb8d), [`bf9e0c6`](../../commit/bf9e0c6),
  [`e13a3d3`](../../commit/e13a3d3))
- **The dial pad left a third of a tall screen blank.** Twelve keys cannot fill 1000 dp at any size
  that still reads as a dial key, so the pad is sized to the window and the rest of the band is
  used rather than stretched. The space it may take is measured from the screen's own bounds; the
  estimate it replaced was about 90 dp out — a whole row of key height.
  ([`baf92f9`](../../commit/baf92f9), [`2a7c9ea`](../../commit/2a7c9ea))
- **The floating results were clipped through a row of text, twice.** A popup taller than its space
  is pinned to the edge of the screen and sliced. The arithmetic now lives in `matchesPlacement`,
  in pixels and free of Compose types, with the rule made explicit: never exceed the space given —
  the minimum height is a preference, not a floor. Both defects needed a device to notice, because
  semantics do not change when one window overlaps another. ([`ce80392`](../../commit/ce80392))
- **Recent callers kept their bare numbers after the contacts permission was granted.** Names were
  resolved only when the call log emitted, so granting contacts from the keypad's own button
  relabelled nothing until somebody rang — the third time this project has shipped work done once
  at construction against a permission that arrives afterwards. ([`ce80392`](../../commit/ce80392))
- **The whole call log was sorted on the main dispatcher on every logged call.** The strip needs
  four callers and `allEntries()` is unbounded. It reads `recentEntries(50)` and deduplicates by
  `comparisonKeys`, so one caller cannot hold two slots by having been dialled in two formats.
  ([`ce80392`](../../commit/ce80392))
- **Two ViewModel tests hung for a minute on the iOS simulator while passing on the JVM.**
  `viewModelScope` dispatches on `Dispatchers.Main`, which on Kotlin/Native needs a run loop no unit
  test spins. Installing a test dispatcher then exposed a real defect underneath: a test that
  asserted before the work it was waiting for had started. `verify.sh` compiles `commonTest` for
  Native but does not run it — `:shared:iosSimulatorArm64Test` is what catches this.
  ([`3822a1a`](../../commit/3822a1a))
- **`KeypadViewModel` lost the contacts-permission flag** once it had two coroutines writing its
  state: `_state.value = _state.value.copy(...)` is a read-modify-write. Every write uses `update()`.
  ([`ce80392`](../../commit/ce80392))

### Changed

- **The navigation bar is Home, Keypad, Agenda, Log, Lists.** Every index after Keypad shifted.
  ([`3a028f5`](../../commit/3a028f5))
- **`FavouritesRow` moved into the contacts package** and is shared by the Agenda and the keypad, so
  both draw the same strip from the same platform flag. ([`e13a3d3`](../../commit/e13a3d3))
- 652 → 721 automated tests (`:shared` 629, `:androidApp` 92, plus 369 on the iOS simulator).
- Version 1.5.0 → 1.6.0, version code 7 → 8. ([`HEAD`](../../commits/main))

## [1.5.0] (7) — 2026-08-19, internal testing

Release notes: [`docs/store/RELEASE_NOTES_1.5.0.md`](docs/store/RELEASE_NOTES_1.5.0.md).

**Never shipped under this name, and its code is gone.** The bundle was built on 2026-08-20 and
this file recorded code 7 as unspent. Play later refused an upload on code 7 outright, so the code
was taken — by this bundle or by something else, and no local record can say which. It moved to
`superseded/`, and 1.6.0 above carries the same changes on code 8. They are kept under this
heading because this is where they were written and reviewed.

**1.4.0 (6) is retired.** Its `.aab` was built on 2026-08-14 and uploaded — Play attaches its
quality advisories to "Release name: 6 (1.4.0)" — so **version code 6 is spent** and could not be
reused regardless. Twenty-one commits landed after it, so that binary stopped describing the tree,
and it predates the emergency-dialling fix below, which is what makes it unshippable rather than
merely stale.

This corrects a claim this file and the release notes both carried until 2026-08-20: that 6 was
never uploaded. It was. Nothing depended on the mistake — 1.5.0 took code 7 — but "spent on upload,
not on publication" is the rule this project keeps getting caught by, and the record now says which
happened.

### Fixed

- **The default dialer could not dial an emergency number.** Tapping Call on `112` from the app's
  own keypad placed no call: Telecom cancelled it and launched the stock dialer with the number
  pre-filled. `CALL_PHONE` was granted by the role — the problem was identity, not permission.
  `startActivity(ACTION_CALL)` travels through Telecom's `UserCallActivity` trampoline, where
  `getCallingPackage()` is null, so `ROLE_DIALER` is invisible exactly when the emergency check
  reads it. Both call sites moved to `TelecomManager.placeCall`. ([`2b34dd5`](../../commit/2b34dd5))
- **The dial pad moved under the finger while a number was being typed.** The contact-match list
  sized itself to its contents, so one keystroke that matched five contacts pushed the `1` key
  882 px down — and the match list, whose rows replace the whole typed number, took the position
  the key had just left. The results now sit in a region of constant height.
  ([`6c90b8e`](../../commit/6c90b8e))
- **The navigation bar had no dark mode.** `AdaptiveScaffold` opened no theme at all and inherited
  Material 3's light baseline, so dark content sat above a white bar. The dark-mode check hunts for
  `MaterialTheme { }` and so could only catch chrome using the wrong theme, never chrome using
  none. ([`5ca7127`](../../commit/5ca7127))

### Added

- **Long-press the keypad's delete key to clear the whole number.** It removed one digit however
  long it was held — the gesture was not missing from the handler, it was missing from the widget:
  Material's `TextButton` exposes no `onLongClick`. ([`6ffca9f`](../../commit/6ffca9f))
- **The Credits screen names its testers** — Sig Mandel, Faride Altamirano, Jose Arellano and
  Augusto Piñán. ([`8302583`](../../commit/8302583))
- **Translations are asserted to take the same format arguments, not just the same keys.** Plurals
  are compared as distinct sets across quantity forms, because which form carries the count is a
  property of the language. ([`9ab2b6d`](../../commit/9ab2b6d))
- **The emergency-callback exemption is time-boxed on the platform's signal too.** It trusted
  `PROPERTY_EMERGENCY_CALLBACK_MODE` outright, so a platform that never clears the flag disabled
  every blocking rule permanently. ([`3f4281a`](../../commit/3f4281a))
- **`device_check.sh` polls for the schema instead of sleeping three seconds**, and
  `rule_matrix_test.sh` now prints the `rule_detail` that explains a failure it previously
  mis-attributed to the rule engine. ([`7b29c71`](../../commit/7b29c71))
- **Country names are translated.** They were 223 hardcoded English strings, written into the rule
  row when it was created, so a Spanish call log read "País: Morocco / Western Sahara" and no
  language change could fix it. `Countries.kt` now carries ISO regions and the name is resolved at
  render time, so existing rows localize with no migration. ([`44463ac`](../../commit/44463ac))
- **All ten store screenshots retaken** — the shipped set predated the theme and showed a purple
  app — with `scripts/seed_screenshots.sh` to reproduce the state.
  ([`d6ec64a`](../../commit/d6ec64a), [`c4406ac`](../../commit/c4406ac))
- **Call waiting stranded the surviving call.** `InCallState` held one `Call` in one field, so a
  second call overwrote the first — and when that second call ended, the state was cleared while
  the first was still connected. `InCallActivity` finished, and the ongoing-call notification's
  Hang up action routed to a null field and did nothing. Three defects of the same single field
  went with it: a background call rewrote the on-screen call's phase, caller names and
  repeat-attempt counts landed on the wrong call, and a held DTMF tone was stopped against
  whichever call was on screen when the handler fired. ([`0b2bbcb`](../../commit/0b2bbcb))
- **Four call states rendered no buttons at all.** A held call, a call being torn down, a dual-SIM
  call waiting for a phone account, and Telecom's simulated ringing all fell to `CallUiPhase.OTHER`,
  which showed a caller's name and nothing to press. `HOLDING` and `DISCONNECTING` are now real
  phases, `HOLDING` offers Resume, and `OTHER` keeps a hang-up button as the escape hatch.
  ([`66ed139`](../../commit/66ed139))
- **Back threw away the call screen.** It called `finish()` while the call carried on. It now
  backgrounds the task, and Home shows a "Return to call" card — the ongoing-call notification was
  the only route back and is not posted at all when notifications are off.
  ([`52e3b95`](../../commit/52e3b95))
- **Four Home tiles all landed on the unfiltered call log.** "Blocked today", "This week", "This
  month" and "Pending review" each navigate to `call_log/<filter>`, and the destination read that
  argument by casting `NavBackStackEntry.arguments` to `Map<String, *>` — a type it has never been.
  The compiler flagged it on every build ("this cast can never succeed"); `as?` turned the
  impossibility into null and the elvis replaced it with `"all"`. `CallLogViewModel.applyFilter`
  handled all four values the whole time, and its unit tests passed, because they call it directly.
  ([`70c8bea`](../../commit/70c8bea))
- **A contact in the address book could still be blocked.** `AndroidContactsGateway` put contact
  numbers through `normalizeForComparison` before handing them to `PhoneNumberParser.sameNumber`,
  which strips the `+` that `sameNumber` reads to decide whether the national form may bridge two
  numbers. A contact saved `+34611998877` therefore stated no country, and a call delivered as
  `611998877` stopped matching it — so the contact was never allowlisted and the next rule down
  blocked them. Their *name* displayed correctly throughout, which is what made it look
  inexplicable. ([`78bc794`](../../commit/78bc794))
- **A custom auto-responder greeting was never played on a real call.** The picker used
  `GetContent()`, whose read grant dies with the picking activity's task; by the time
  `PassthroughInCallService` read the URI the grant was gone, the exception was swallowed, and the
  blocked caller was answered and hung up on in silence. Now `OpenDocument` +
  `takePersistableUriPermission`, with a fallback to the spoken script if the audio still cannot be
  opened. ([`1151b31`](../../commit/1151b31))
- **A second call could steal the first call's recording.** `AutoResponderRecorder` lived on the
  service, so whichever call ended first stopped the other's recording and filed its audio under
  its own call-log row. Now owned per call. The greeting's completion callback also mutated
  per-call state from a TTS/MediaPlayer thread; it hops back onto the service scope first.
  ([`fa92bab`](../../commit/fa92bab))
- **The duplicate-number dialog showed a literal backslash** — `won\'t override the block`. Compose
  Multiplatform resources are read as plain XML, where Android's `\'` apostrophe escape means
  nothing. Nothing could catch it: it compiles, the translation test only compares key sets, and
  Android Lint cannot see that resource tree. Now covered by `ComposeStringEscapingTest`.
  ([`43b9ae7`](../../commit/43b9ae7))
- **The iOS CI job could not compile `commonTest`.** Kotlin/Native rejects a comma inside a
  backticked test name (three tests, green on the JVM for months), and `viewModelScope` work never
  runs in a Native unit test without `Dispatchers.setMain`, so `CallLogViewModelTest` waited out
  `runTest`'s full one-minute timeout. ([`0f7bf4c`](../../commit/0f7bf4c))
- **`rule_matrix_test.sh` phase E could never skip.** Its `grep -v '^+'` exits 1 when it filters
  everything out — exactly the case it exists to detect — which under `set -e -o pipefail` killed
  the script mid-phase with no result, no reason and no summary. **`ring_test.sh` reported a false
  ringing failure** after any rule-matrix run, because the matrix leaves `default_action=BLOCK`,
  under which the ringer correctly stays silent. ([`3298d48`](../../commit/3298d48))

### Added

- **Dark mode.** Every screen opened a bare `MaterialTheme { }` — Material 3's baseline light
  palette, whatever the system was set to — and both activities named
  `Theme.Material.Light.NoActionBar` directly, so even the window behind Compose was white. The
  colours are transcribed from `design/mockups.html`, which has carried a light and dark pair since
  the UI was designed. Each screen themes itself: `InCallActivity` draws `CallScreen` outside the
  nav host, so a root-only theme would have missed the one screen that most needs this.
  ([`1ef10a6`](../../commit/1ef10a6))
- **An emergency callback exemption, on by default.** For 30 minutes after the user calls the
  emergency services, every incoming call is let through — checked before every rule, including a
  manual block. Without it, a callback from a number not in the address book was blocked by the
  default action, quiet hours or a country rule, and with the auto-responder on it was answered,
  read a greeting and hung up on. Uses `PROPERTY_EMERGENCY_CALLBACK_MODE` plus the last emergency
  number this app saw dialled, so it needs no new permission. Also makes `RuleDecision.isBlocked`
  an exhaustive `when`, since the old `!is` chain would have classified the new decision as
  blocked. ([`7ad9fcb`](../../commit/7ad9fcb))
- **A DTMF keypad on the in-call screen.** Holding `ROLE_DIALER` means this is the only call screen
  the user has, and without a pad every automated menu ended at the first prompt. Offered on
  `ACTIVE` calls only, because Telecom drops a tone played on a ringing or dialling call. The tone
  is held 250 ms — below ~40 ms an ITU Q.24 receiver need not recognise it — and the digits already
  sent are shown, because nothing on the line echoes them back. The twelve keys are now a `DialPad`
  shared with the dialer screen. ([`578b779`](../../commit/578b779))
- **The installed version in Settings**, `1.4.0 (6)`, read from `PackageManager` on Android and the
  bundle's `Info.plist` on iOS rather than from a constant that could disagree with the artifact.
  Both numbers, because a version name does not say which upload the user is on.
  ([`acbe02c`](../../commit/acbe02c))
- **Credits is no longer empty.** The maintainer, the AI pair used to write the code, and the
  open-source libraries the app ships with — each with its SPDX identifier and project home. People
  and licences are separate sections: one is a courtesy, the other an obligation. Test-only and
  build-only dependencies are omitted, because none of them reach a device.
  ([`81921bd`](../../commit/81921bd))
- **Block state in the call log.** A row shows whether its number is on the block list or allowlist
  *right now*, and tapping it offers Unblock / Remove from allowlist instead of the action already
  taken. Kept separate from the call's own outcome: a call blocked by a rule since deleted still
  reads "Blocked call" and carries no badge. ([`607ca60`](../../commit/607ca60))
- **The auto-responder says why recording will not run** — responder off, greeting invalid,
  microphone not granted — and a "How this works" card states the limits it cannot predict: only
  blocked calls are answered, the greeting reaches the caller acoustically through the loudspeaker,
  and recording captures the microphone rather than the call, so some phones capture nothing.
  ([`b221837`](../../commit/b221837))
- **`rule_matrix_test.sh` phase F**, covering the contact direction that broke: saved
  internationally, called from a domestic line. Phase E covers the mirror image and passed against
  the broken build, which is why F had to exist. ([`3298d48`](../../commit/3298d48))
- **Course chapter 28**, "The Fix That Was Only Half Applied". ([`850688b`](../../commit/850688b))
- **`docs/build-from-scratch.html`** — a build-along tutorial, nine steps from an empty directory to a
  working call blocker. Distinct from the existing course, which documents finished code and cannot be
  followed. Steps 1–4 were executed from zero on an emulator; the document states which later steps were
  not. Published on GitHub Pages: https://cpinan.github.io/Corta-Spam/build-from-scratch.html

### Changed

- `ContactsGateway.contactNumbers` now documents that it returns numbers **as saved**, not
  normalised — `sameNumber` is only correct if every caller respects that.
  ([`78bc794`](../../commit/78bc794))
- `verify.sh` compiles `commonTest` for Kotlin/Native (`:shared:compileTestKotlinIosSimulatorArm64`),
  which it never did while CI ran `:shared:iosSimulatorArm64Test`. ([`0f7bf4c`](../../commit/0f7bf4c))
- 528 → 571 automated tests (`:shared` 406 → 494, `:androidApp` 66 → 77); 306 of them also run on
  the iOS simulator.

## [1.3.0] (5) — 2026-08-14, internal testing

Release notes: [`docs/store/RELEASE_NOTES_1.3.0.md`](docs/store/RELEASE_NOTES_1.3.0.md).
Version set in [`3abc4e9`](../../commit/3abc4e9). Built and validated; **not uploaded** as of this
entry, so version code 5 is not yet spent.

### Fixed

- **The app crashed the moment any call was answered.** Android 14 refuses a `CallStyle`
  notification without a foreground service, user-initiated job or full-screen intent, and refuses
  it by throwing out of `notify()` on the main thread inside a `Call.Callback`. Three of the four
  reported symptoms — the platform phone app "taking over", the app's own call screen vanishing,
  the missing missed-call notification — were that one process death.
  ([`f82c8c8`](../../commit/f82c8c8))
- Telecom's own missed-call notification is now suppressed by declaration rather than by accident.
  ([`9c7652d`](../../commit/9c7652d))
- Call-log timestamps were assembled in `commonMain` from an English enum constant and never
  localized. ([`3e2ebfc`](../../commit/3e2ebfc))

### Added

- Call-log filters: search by name or number, direction/outcome chips, date chips.
- Outgoing calls in the log (schema v4), labelled as outgoing rather than "allowed".
- Caller identity on the call screen; contact search on the keypad; notification actions that open
  the caller's actions when tapped ([`308bd10`](../../commit/308bd10)).

## [1.2.0] (4) — 2026-08-13, internal testing

Release notes: [`docs/store/RELEASE_NOTES_1.2.0.md`](docs/store/RELEASE_NOTES_1.2.0.md). Version
set in [`147469f`](../../commit/147469f), same day it superseded 1.1.4 on the same version code
([`ff57066`](../../commit/ff57066)). **This is the build the internal testers have**, and it
carries the answered-call crash fixed above.

## [0.1.0] — 2026-08, internal testing (2) and production (3)

Release notes: [`docs/store/RELEASE_NOTES_0.1.0.md`](docs/store/RELEASE_NOTES_0.1.0.md)
(versionCode 2, [`69d264a`](../../commit/69d264a)) and
[`RELEASE_NOTES_PROD_0.1.0.md`](docs/store/RELEASE_NOTES_PROD_0.1.0.md) (versionCode 3).
Version code 4 was first cut for a declined pre-grant declaration
([`d25a287`](../../commit/d25a287)) before being reused for 1.2.0.

---

This file starts where the versioned uploads do, and the entries above 1.3.0 are summarised from
the release-notes documents rather than written at the time. For anything earlier — or for the
reasoning behind any entry here — the README's [Recent Fixes](README.md#recent-fixes) section runs
back to the first milestone and is the complete record.
