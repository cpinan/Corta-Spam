# Release status — Corta Spam internal testing

**As of 2026-08-05.** This is the *current state*; `PLAY_RELEASE_PLAN.md` is the sequence and
`PLAY_INTERNAL_TESTING.md` is the requirements reference.

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
- [x] `org.carlospinan.cortaspam`, versionCode 1, versionName 0.1.0, targetSdk 36
- [x] **R8 verified on-device** — app launches, Koin resolves, `kotlinx.serialization` works
      (export produced real JSON). This was the actual risk; the ProGuard rules hold.

## ✅ Done — store assets

- [x] Privacy policy hosted, styled, ES/EN toggle → `https://cpinan.github.io/Corta-Spam/PRIVACY`
- [x] 512×512 icon, 1024×500 feature graphic **in Spanish** (+ `_en` variant)
- [x] 9 screenshots padded to exactly 9:16 (`docs/store/play/`) — raw captures were 9:20 and
      Play would have rejected them
- [x] Listing copy, Spanish default + English translation, all within character limits
- [x] Release notes for `es-419`

## ✅ Done — tooling

- [x] `scripts/verify.sh`, `scripts/device_check.sh`
- [x] Skills: `eod`, `find-inert-features`, `split-interlocking-commits`; three existing ones corrected
- [x] `.gitignore` covers signing material — added *before* a key existed

---

## ⬜ Remaining — you only

- [ ] **Contact email** for the store listing (public on the page)
- [ ] Play Console: create app with package `org.carlospinan.cortaspam`, es-419, App, Free
- [ ] Play Console: **App content** — every section green
      - [ ] Privacy policy URL
      - [ ] Data safety → *collects no data*
      - [ ] Ads → No · Content rating (IARC) · Target audience (not child-directed)
      - [ ] **Permissions declaration** for the dialer role
      - [ ] **Demo video** (~60s, unlisted YouTube) — reviewers ask for one on dialer apps
- [ ] Play Console: Store listing — paste copy, upload `docs/store/play/` assets
- [ ] Play Console: upload `corta-spam-0.1.0-1-release.aab`
- [ ] **Accept Play App Signing** when offered (one-way; declining makes a lost key fatal)
- [ ] Add testers, send them the **opt-in link** (they cannot install without it)

## ⚠️ Remaining — the real risk

- [ ] **Prove the ringtone against a real inbound call.**
      Set the app as default dialer on the razr and have someone call you. The manifest declares
      `IN_CALL_SERVICE_RINGING`, meaning the app owns ringing — if `CallRinger` misbehaves on an
      OEM, that phone rings for nothing and the user misses calls silently. Everything else in
      this document has been verified; this has not, and it cannot be verified from a build
      machine.
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
