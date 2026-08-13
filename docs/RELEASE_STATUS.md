# Release status — Corta Spam internal testing

**As of 2026-08-13.** This is the *current state*; `PLAY_RELEASE_PLAN.md` is the sequence and
`PLAY_INTERNAL_TESTING.md` is the requirements reference.

> **Version code 4 is cut and built (2026-08-13).** It is the answer to the code 3 rejection: the
> declaration form now declines the pre-grant, and the bundle carries the onboarding checklist row
> that gives the user a way to grant the app-op themselves. Nothing has been uploaded yet.

> **Version code 3 was rejected 2026-08-12** under the Full-Screen Intent policy — the same
> finding that took version code 1, *"Permission use is not directly related to your app's core
> purpose."* This time the declaration form **had** been submitted before upload and the listing
> **had** been rewritten to open with "phone app", so the version-code-1 theory (the reviewer never
> saw the form) is dead.
>
> **Resolved by declining the pre-grant, not by removing the permission.** The declaration form's
> second question — *do you want this permission pre-granted at installation?* — was changed from
> **Yes** to **No** on 2026-08-12. Core functionality stays *making and receiving calls*. The
> rejection is a verdict on pre-grant eligibility, so opting out removes the thing being judged.
> The permission stays in the manifest and no code was deleted for policy reasons. See
> `PLAY_FSI_APPEAL.md`.
>
> **What it costs:** every install on Android 14+ now starts with the ringing screen off. The user
> grants the app-op from system settings, so the in-app route to it is load-bearing now rather than
> a fallback — hence the full-screen row added to the onboarding checklist.

> **Codes 1, 2 and 3 are all spent.** 1 rejected under the FSI policy, 2 uploaded and withdrawn
> after Play warned it dropped 6 devices (`RECORD_AUDIO` implying a required microphone), 3
> rejected under the FSI policy again. 1 and 2 sit in `rejected/`.
>
> **The next upload needs `appVersionCode` 4** in `androidApp/build.gradle.kts` — both because 3 is
> spent and because the onboarding change has to ship with it.

---

## ✅ Done — code

- [x] **18 audit defects fixed**, each with tests. Six were features that shipped, passed their
      tests, and never executed — see course Chapter 20 and `find-inert-features` skill.
- [x] Rule engine: international-form country matching, bundled spam list reachable, pattern
      `*` no longer matches every number, repeat-caller scoping reachable, country codes deduped
- [x] Backup restore: no longer disables the wrong rule, atomic, validated, counts honest
- [x] Statistics on local calendar days, DST-aware, indexed
- [x] Telecom: ringtone implemented, call waiting survives, speaker forced for auto-responder
- [x] i18n: structured `BlockReason` rendered per locale; call-log status localized
- [x] Database I/O on the dispatcher `DriverFactory` nominates
- [x] **341 JVM tests** (227 also on the iOS simulator), up from 273
- [x] Android Lint runs for the first time in the project's life, and gates CI
- [x] Release build type with R8 + resource shrinking + ProGuard rules
- [x] 20 commits, all pushed to `origin/main`, working tree clean

## ✅ Done — signing and artifact

- [x] Upload keystore generated (`~/corta-spam-upload.jks`, valid to 2053)
- [x] `androidApp/keystore.properties` wired; gitignored
- [x] Artifacts carry the version: `corta-spam-<versionName>-<versionCode>-release.aab`
- [x] **Signature verified**: `CN=Carlos Pinan, OU=Casa, O=Casa, L=Lima, ST=Lima, C=PE`
- [x] `org.carlospinan.cortaspam`, **versionCode 4**, versionName 0.1.0, targetSdk 36 — built and
      audited 2026-08-13, `jar verified`, `CN=Carlos Pinan`. Codes 1, 2 and 3 are in
      `androidApp/build/outputs/bundle/release/rejected/`; the upload folder holds one file
- [x] Permissions audited on the **built bundle**, not the source manifest: `CALL_PHONE`,
      `READ_CONTACTS`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `RECORD_AUDIO`, `VIBRATE`.
      `READ_PHONE_STATE` was declared with no caller and has been removed (2026-08-11).
      `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` is androidx merge residue, kept deliberately.
      `BIND_INCALL_SERVICE` and `DUMP` in a bundle dump are `android:permission` **guards** on a
      service and on androidx's `ProfileInstallReceiver` — not requests.
- [x] **R8 verified on-device** — app launches, Koin resolves, `kotlinx.serialization` works
      (export produced real JSON). This was the actual risk; the ProGuard rules hold.

## ✅ Done — store assets

- [x] Privacy policy hosted, styled, bilingual ES/EN → `https://cpinan.github.io/corta-spam/privacy.html`
      (canonical, on the `cpinan.github.io` site, folder-per-app like the other apps). A landing and
      support page sits beside it at `https://cpinan.github.io/corta-spam/`.
      **`docs/PRIVACY.html` in this repo is a second copy** still served at the old
      `https://cpinan.github.io/Corta-Spam/PRIVACY`. Two copies of a legally operative document
      drift — this policy already shipped one wrong claim about a network call that did not exist.
      Give the Console the canonical URL, and fold the old one into a redirect when convenient.
- [x] 512×512 icon, 1024×500 feature graphic **in Spanish** (+ `_en` variant)
- [x] 9 screenshots padded to exactly 9:16 (`docs/store/play/`) — raw captures were 9:20 and
      Play would have rejected them
- [x] Listing copy, Spanish default + English translation, all within character limits
- [x] Release notes, both languages, paste-ready → `docs/store/RELEASE_NOTES_0.1.0.md`

## ✅ Done — tooling

- [x] `scripts/verify.sh`, `scripts/device_check.sh`
- [x] Skills: `eod`, `find-inert-features`, `split-interlocking-commits`; three existing ones corrected
- [x] `.gitignore` covers signing material — added *before* a key existed

---

## ⬜ Remaining — you only

- [x] **Contact email** for the store listing — `carlos.pinan@gmail.com`
- [x] Play Console: app created; Category **Communication**, tags `Caller ID, Communication`
- [x] Play Console: **App content** (2026-08-11, amended 2026-08-12)
      - [x] **Full-screen intent declaration** — core functionality *making and receiving calls*;
            install behaviour **No** (do not pre-grant), set 2026-08-12 after the same policy
            rejected version code 3 with the form already in place. Submitting the form first was
            not enough, because the form was asking to be pre-granted
      - [x] Privacy policy URL → `https://cpinan.github.io/corta-spam/privacy.html` (canonical)
      - [x] Data safety → *collects no data*. True by construction: no `INTERNET` permission and
            no HTTP client anywhere in the dependency graph, so nothing can leave the device
      - [x] Ads → No · Content rating (IARC) · Target audience 13+, no band under 13
      - [x] **Permissions declaration** — nothing to declare. That form is triggered by the Call
            Log and SMS permission groups; this app declares neither, reading calls through
            `InCallService` and keeping its own database
- [ ] Play Console: Store listing — **re-paste** the copy. Rewritten 2026-08-06 to lead with
      "phone app", and corrected 2026-08-11 (it claimed the app requests no microphone access,
      which `RECORD_AUDIO` in versionCode 2 makes false). Screenshots are per-language: the `es_*`
      set is the **default** listing, the unprefixed set is the en-US translation
- [x] **Demo video** recorded and uploaded unlisted 2026-08-11 →
      `https://www.youtube.com/watch?v=x15aUnHav6w`. Shows an incoming call arriving on a locked
      screen, which is the full-screen intent permission doing the job the policy auto-grants it
      for. No audio — `screenrecord` captures none, and the description says so
- [x] Play Console: uploaded `corta-spam-0.1.0-3-release.aab` and **submitted to PRODUCTION**
      on 2026-08-11 — **rejected 2026-08-12** under the Full-Screen Intent policy
- [x] `appVersionCode` bumped to 4 and rebuilt (2026-08-13) —
      `androidApp/build/outputs/bundle/release/corta-spam-0.1.0-4-release.aab`, 5.2 MB
- [ ] Play Console: upload `corta-spam-0.1.0-4-release.aab` and resubmit to production. The
      declaration form now declines the pre-grant, so the finding that rejected 1 and 3 has nothing
      left to judge. **Do not touch the declaration form again before uploading** — the No answer
      set on 2026-08-12 is the fix; re-opening it risks resetting it to the default Yes
- [ ] Release notes: reuse `docs/store/RELEASE_NOTES_PROD_0.1.0.md` unchanged. Code 3 was never
      published, so code 4 is still the first release any user sees and the copy still reads true
- [ ] **Accept Play App Signing** when offered (one-way; declining makes a lost key fatal)
- [ ] Add testers, send them the **opt-in link** (they cannot install without it)

## ⚠️ Remaining — the real risk

- [ ] **Prove the ringtone against a real inbound call.**
      Set the app as default dialer on the razr and have someone call you. The manifest declares
      `IN_CALL_SERVICE_RINGING`, meaning the app owns ringing — if `CallRinger` misbehaves on an
      OEM, that phone rings for nothing and the user misses calls silently. Everything else in
      this document has been verified; this has not, and it cannot be verified from a build
      machine.
- [x] **`Call.Details.handle` still arrives without `READ_PHONE_STATE`** — verified 2026-08-11 on
      the API 33 emulator. A simulated call from `+34900123456` logged `BLOCKED` / `MANUAL` /
      `rule_id=1` carrying the real number, with a matching `CallAttempt` row. Had the reading of
      `roles.xml` been wrong, the handle would have been null and no row would exist at all —
      `PassthroughInCallService` skips recording a blank number — so this is a positive assertion,
      not an absence one.
- [x] **The ringtone rings** — verified 2026-08-11, first time. An unmatched number rang through
      and Telecom logged `Ringer: Ending early -- letDialerHandleRinging=true`, standing down
      because of `IN_CALL_SERVICE_RINGING`. `dumpsys audio` then showed the **app's own**
      MediaPlayer (`uid 10180`) `state:started` with `usage=USAGE_NOTIFICATION_RINGTONE`, and
      `dumpsys vibrator_manager` a running RINGTONE vibration with
      `opPkg: org.carlospinan.cortaspam`. Both stopped when the call ended.
      **This was on an emulator.** The OEM case — a razr reserving audio or vibration during a
      call — is what actually decides whether users miss calls, and it is still unproven.
- [ ] Confirm on a second manufacturer before leaving internal testing

## 🔵 Open decisions (not blockers)

- [ ] **Country names are English** in the call log ("País: Morocco / Western Sahara").
      `Countries.kt` holds 224 English names written into the database at rule-creation time.
      Options: translate 224×3 locales (correct, large, a bad translation means a wrong country
      in someone's block list); render the code only (`País: +212`); or leave it (internally
      consistent — the picker is English too).
- [ ] `android.hardware.telephony` is `required="true"` — excludes tablets and ChromeOS from the
      listing. Changing it needs the app to degrade gracefully with no telephony, which is untested.
- [ ] `namespace` is still `org.carlospinan.bloqueador.app` — invisible to users, cosmetic only.
- [ ] Native debug symbols warning on upload: **ignore.** The only `.so` is
      `libandroidx.graphics.path.so` from Compose; there is no first-party native code.
