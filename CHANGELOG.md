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

Not in any uploaded bundle. `corta-spam-1.3.0-5-release.aab` was built before these landed, so
shipping them needs a new version code — see [`docs/store/`](docs/store/) for the release-notes
format.

### Fixed

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
- **`course/build_from_scratch.html`** — a build-along tutorial, nine steps from an empty directory to a
  working call blocker. Distinct from the existing course, which documents finished code and cannot be
  followed. Steps 1–4 were executed from zero on an emulator; the document states which later steps were
  not.

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
