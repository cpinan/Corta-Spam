# Play Store internal testing — what Corta Spam needs

Everything required to get an internal-testing track live, in the order it has to happen.
Written for this app specifically: open source, no analytics, no ads, no accounts, no network.

Internal testing is the lightest Play track — up to 100 testers by email, no review queue for
most updates, live in minutes rather than days. It is still a real Play release, so the app must
be signed, complete a Data Safety form, and satisfy the policies below.

The hard part for this app is **not** the store listing. It is that a default-dialer app requests
`READ_PHONE_STATE` and `CALL_PHONE`, which Google treats as sensitive, plus `READ_CONTACTS`.
Start the declaration work early; the listing takes an afternoon.

---

## 0. Blockers — the app cannot be uploaded until these are done

| # | Item | Where | Status |
|---|------|-------|--------|
| 0.1 | An upload keystore, backed up somewhere you will still have in five years | local / password manager | **missing** |
| 0.2 | `signingConfig` wired into the release build, reading from `keystore.properties` (gitignored) | `androidApp/build.gradle.kts` | **missing** — deliberately omitted, no key exists yet |
| 0.3 | `versionCode` / `versionName` strategy | `androidApp/build.gradle.kts` | currently `1` / `0.1.0` — fine for the first upload, needs to increment every upload after |
| 0.4 | An `.aab`, not an `.apk` — Play requires App Bundles | `./gradlew :androidApp:bundleRelease` | **works** — verified 2026-08-05, 5.0 MB, but debug-signed until 0.1/0.2 are done |
| 0.5 | A Google Play Console developer account (one-off USD 25) with identity verification completed | Play Console | **check** — verification can take days |

Everything else in this document can be done in parallel with 0.5.

### 0.1 / 0.2 — signing

```bash
keytool -genkeypair -v \
  -keystore corta-spam-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias upload
```

Then `androidApp/keystore.properties` (**add to `.gitignore` before creating it**):

```properties
storeFile=/absolute/path/to/corta-spam-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

...and read it in `androidApp/build.gradle.kts`. Enrol in **Play App Signing** (the default) so
Google holds the release key and this keystore is only the *upload* key — if it is lost, support
can rotate it. If you opt out and lose the key, the app can never be updated again.

---

## 1. Store listing assets

Internal testing still requires a complete listing.

| Asset | Spec | Status |
|-------|------|--------|
| App name | ≤30 chars — "Corta Spam" | ready |
| Short description | ≤80 chars | **to write** |
| Full description | ≤4000 chars | derive from `README.md` "Features" |
| App icon | 512×512 PNG, 32-bit, no alpha | derive from `ic_launcher` — the adaptive icon is not a substitute |
| Feature graphic | 1024×500 PNG/JPG, no alpha | **to make** |
| Phone screenshots | 2–8, min 320px, max 3840px, 16:9 or 9:16 | **to capture** |
| 7" / 10" tablet screenshots | required only if you declare tablet support | app *is* adaptive — worth including |
| Category | Tools (not Communication — this is a utility, and Communication invites stricter dialer scrutiny) | decide |
| Contact email | must be a real monitored address | decide |
| Privacy policy URL | **publicly hosted**, not the in-app screen | **to publish** |

**Screenshots to take** (all four locales exist, so consider one set per locale later — English
only is acceptable to start):

1. Home with the blocking toggle and the blocked-today counters
2. Block Lists hub showing all six categories
3. Pattern rules or Repeat callers, mid-add, so the rule model is visible
4. Call log with a blocked entry and its reason
5. Settings showing the default-action choice
6. Quiet hours

Capture on the Pixel_8_Pro_API_33 AVD (448dp portrait) rather than the razr — foldables produce
awkward aspect ratios and its displays are hard to screencap.

### Privacy policy hosting

The in-app policy is not enough; Play needs a URL. Cheapest correct option: publish
`docs/PRIVACY.md` to GitHub Pages on the existing `cpinan.github.io` setup, matching the text
now in `values/strings.xml` (`privacy_policy_body`). **Keep the two in sync** — the in-app copy
was recently corrected because it described a network call the app does not make.

---

## 2. Data Safety form

This is where an app with nothing to declare still has to say so explicitly. Play does not
accept silence.

| Question | Answer for this app |
|---|---|
| Does your app collect or share any user data? | **No** |
| Is all user data encrypted in transit? | N/A — no data leaves the device |
| Do you provide a way to request data deletion? | Yes — clearing app data / uninstalling; also the in-app "clear call log" |

Be ready to justify "No" against the permissions:

- `READ_CONTACTS` — read on-device only, to match callers against the allowlist. Never uploaded,
  never written to any file, cached in memory for five minutes and discarded.
- `READ_PHONE_STATE` / `CALL_PHONE` — required for the dialer role; no number leaves the device.
- The call log is stored in a local SQLite database, excluded from cloud backup and device
  transfer via `android:allowBackup="false"` and `res/xml/data_extraction_rules.xml`.

**This is a genuine advantage of the app's design — the honest answer is also the simplest one.**
Do not overclaim in the other direction either: the Data Safety form is legally binding.

---

## 3. Permissions and policy declarations

### 3.1 Default dialer / Call Log permissions

The app holds `ROLE_DIALER`. Google's Permissions policy restricts `CALL_LOG` and `SMS` groups
to apps whose *core functionality* requires them, and requires a **Permissions Declaration Form**
in Play Console for those groups.

- This app does **not** request `READ_CALL_LOG` or `WRITE_CALL_LOG` — verify before submitting:
  ```bash
  grep -rn "CALL_LOG\|READ_SMS\|RECEIVE_SMS" androidApp/src/main/AndroidManifest.xml
  ```
  (Currently: no matches. The app keeps its *own* log rather than reading the system one — say
  this in the declaration, it is the strongest possible answer.)
- `READ_PHONE_STATE` + `CALL_PHONE` are still likely to trigger a questionnaire. Answer:
  *default phone app / dialer replacement*, and record a demo video (below).

### 3.2 Demo video

Reviewers assessing a dialer will ask for one. Record a ~60s screen capture showing:
setting the app as default dialer → an incoming call ringing → a blocked call being rejected →
the call log entry with its reason. Upload unlisted to YouTube; the Console wants a URL.

### 3.3 Other declarations

- **Ads**: No.
- **Content rating** questionnaire (IARC): answer honestly; this lands at Everyone / PEGI 3.
- **Target audience**: 18+ or 13+; **not** child-directed — a child-directed dialer would drag in
  Families policy requirements you do not want.
- **Government apps / financial features / health**: No to all.
- **Foreground service types**: none declared, and `InCallService` does not need one. Confirm
  before upload — Android 14+ requires a declared type for any foreground service.

---

## 4. Technical release checklist

```bash
# 1. everything green
./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin \
          ktlintCheck :shared:testDebugUnitTest :androidApp:testDebugUnitTest \
          :shared:verifySqlDelightMigration :androidApp:lintDebug :shared:lintDebug \
          :shared:compileKotlinIosSimulatorArm64

# 2. the artefact Play actually wants
./gradlew :androidApp:bundleRelease
# -> androidApp/build/outputs/bundle/release/androidApp-release.aab
```

Before the first upload:

- [ ] **Install the release build and use it.** R8 is on for the first time. Serialization,
      Koin reflection and the Telecom entry points are all shrink-sensitive; `proguard-rules.pro`
      covers them but has never been exercised on a device.
      `./install_android.sh --release --device <serial>`
- [ ] Verify the auto-responder, backup export/import, and an actual incoming call on the release
      build specifically — not the debug one.
- [ ] Confirm `minSdk 26` / `targetSdk 36` still meets Play's target-API requirement for the
      upload month (Play raises the floor every August; 36 is current).
- [ ] Check the `.aab` size and that all four locales are inside:
      `bundletool build-apks` or Play Console's post-upload language list.
- [ ] Set `versionCode 1`, `versionName "0.1.0"` for the first upload; increment `versionCode`
      on **every** subsequent upload, even a re-upload of the same code.

---

## 5. Console setup, in order

1. Create the app — name, default language (English), app-or-game: **App**, free-or-paid: **Free**.
   *Free cannot be changed to paid later.*
2. Complete **App content**: privacy policy URL, ads, content rating, target audience, data
   safety, government apps, financial features.
3. **Internal testing → Create new release** → upload the `.aab`.
4. Add testers: create an email list (up to 100). Testers must **opt in via the returned URL**
   before they can see the app.
5. Roll out. The track is usually available within minutes.
6. Send testers the opt-in link plus a short note on what to check — for this app: set it as the
   default dialer, then confirm the phone actually rings, that a blocked number does not, and
   that the call log explains why.

---

## 6. Things that will specifically bite this app

- **`android.hardware.telephony` is declared `required="true"`.** That is the honest declaration
  for a dialer, and it excludes tablets and ChromeOS from the Play listing. If you want tablet
  testers, this must change to `required="false"` — and then the app has to degrade gracefully on
  a device with no telephony rather than crash.
- **Default-dialer apps get closer review.** Expect the first submission to take longer than the
  "minutes" internal testing usually promises.
- **The ringtone is new and unverified on a real call.** It is the single highest-risk change in
  the current build: the app declares `IN_CALL_SERVICE_RINGING`, so if `CallRinger` fails on a
  given OEM, that device rings for nothing. Test on more than one manufacturer before widening
  past internal.
- **No crash reporting.** A deliberate choice, consistent with "no analytics" — but it means the
  only signal from testers is what they tell you. Ask them explicitly to send
  `adb logcat` output or a Play Console crash report (Play collects ANRs/crashes natively,
  which is not analytics and does not change the Data Safety answer).
- **Play App Signing enrolment is one-way.** Decide before the first upload.

---

## 7. Not required for internal testing

Skipping these is fine now and needed before production:

- Localised store listings for es / hi / pt (the *app* is localised; the *listing* need not be yet)
- A promo video
- Pre-launch report device coverage tuning
- Closed/open testing tracks
- Store listing experiments
