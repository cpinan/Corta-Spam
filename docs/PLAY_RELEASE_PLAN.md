# Publishing Corta Spam to Google Play — step by step

From "nothing on Play" to "live in production", in order. Each step says what to do, how to know
it worked, and what blocks the next one.

`docs/PLAY_INTERNAL_TESTING.md` is the reference for *what Play requires* (asset specs, the Data
Safety answers, the permission declarations). **This** document is the sequence. Read that one
when a step here says "fill in the form"; read this one to know which form and when.

**Track order is not optional.** Play gates production on having shipped to a test track first,
and for a default-dialer app you want the review friction discovered on a track nobody is
depending on. Plan for **internal → closed → production**, and expect the first review to be the
slow one.

---

## Phase 0 — Decisions to make before anything is irreversible

Four choices that cannot be changed later. Make them deliberately.

| Decision | Options | Recommendation for this app |
|---|---|---|
| **Package name** | `org.carlospinan.bloqueador.app` is already published in the APK | Keep. It cannot change after the first upload — a new package name is a new app with zero installs. Note it still says `bloqueador`, the pre-rename name; that is cosmetic and not worth the cost of never being able to update. |
| **Free or paid** | Free / Paid | **Free.** A free app can never become paid. |
| **Play App Signing** | Enrol / opt out | **Enrol** (the default). Google holds the release key; your `.jks` is only the *upload* key and can be rotated by support if lost. Opting out means losing that file ends the app permanently. |
| **Developer account type** | Personal / Organisation | Personal is fine for an individual open-source project. Organisation needs a D-U-N-S number and takes longer. |

> **Do Phase 0 and Phase 1 now.** Everything else can be done in an afternoon; account
> verification and the first policy review cannot be hurried.

---

## Phase 1 — Account (start immediately, it has the longest lead time)

### 1.1 Create the developer account

<https://play.google.com/console/signup> — USD 25, one-off, non-refundable.

### 1.2 Complete identity verification

Google requires a government ID and, for personal accounts created since 2023, **a verified
phone number and address**. This can take **anywhere from a day to two weeks**. Nothing else in
this plan is blocked by it, so start it and carry on.

### 1.3 Set up a payments profile

Required even for a free app with no in-app purchases.

**Done when:** the Console shows the account as verified and lets you create an app.

---

## Phase 2 — Signing (do this once, correctly, and back it up)

### 2.1 Generate the upload key

```bash
keytool -genkeypair -v \
  -keystore corta-spam-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias upload
```

`-validity 10000` is ~27 years. A key that expires while the app is live is a problem you cannot
fix without Google's help.

### 2.2 Store it somewhere you will still have in five years

The keystore file **and** its two passwords. A password manager entry with the file attached is
the minimum. This is the single least recoverable artifact in the project.

**Do not put it in the repository.** `.gitignore` already covers `*.jks`, `*.keystore`,
`keystore.properties`, `*.p12`, `*.pepk` and `play-service-account*.json` — that was added before
any key existed, on purpose.

### 2.3 Point the build at it

Create `androidApp/keystore.properties` (gitignored):

```properties
storeFile=/absolute/path/to/corta-spam-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

The build already reads this file if it exists and skips signing if it doesn't, so a clean
checkout still builds. Verified working: with the file present, `assembleRelease` produces
`androidApp-release.apk`; without it, `androidApp-release-unsigned.apk`, which Play rejects.

### 2.4 Confirm the artifact is actually signed by your key

```bash
./gradlew :androidApp:bundleRelease
$(find ~/Library/Android/sdk/build-tools -name apksigner | sort -V | tail -1) \
  verify --print-certs androidApp/build/outputs/apk/release/androidApp-release.apk
```

**Done when:** the printed certificate DN is yours and not `CN=Android Debug`.

---

## Phase 3 — The things Play asks for that aren't code

Do these in parallel with Phase 1's waiting.

### 3.1 Host the privacy policy

Play needs a **public URL**, not the in-app screen. `docs/PRIVACY.md` is written and matches the
in-app text. Publish it to GitHub Pages on the existing `cpinan.github.io` setup and keep the two
in sync — the in-app copy was recently corrected because it described a network call the app does
not make, and a privacy policy that overclaims is as wrong as one that underclaims.

### 3.2 Capture screenshots

```bash
adb -s <serial> shell screencap -p /sdcard/s.png
adb -s <serial> pull /sdcard/s.png ./shot.png
```

Six screens, listed in `PLAY_INTERNAL_TESTING.md` §1. Use the `Pixel_8_Pro_API_33` AVD rather
than the razr — a foldable produces awkward aspect ratios.

### 3.3 Make the icon and feature graphic

512×512 icon (no alpha) and a 1024×500 feature graphic. The adaptive launcher icon is not a
substitute for either.

### 3.4 Write the listing copy

Short description (≤80 chars) and full description (≤4000). Derive from `README.md` Features.
**Do not use the words "block spam calls automatically" in a way that promises detection
accuracy** — this app blocks by *your* rules plus a small bundled list, and an over-promising
listing is both a review risk and untrue.

**Done when:** every row in `PLAY_INTERNAL_TESTING.md` §1 has an asset.

---

## Phase 4 — Create the app and fill in App Content

### 4.1 Create the app

Console → **Create app**. Name, default language English, type **App**, **Free**.

### 4.2 Work through every App Content section

All of these gate release. `PLAY_INTERNAL_TESTING.md` §2–3 has the exact answers.

- Privacy policy URL (from 3.1)
- **Data safety** — the answer is "collects no data"; be ready to justify it against
  `READ_CONTACTS`
- Ads — **No**
- Content rating questionnaire (IARC)
- Target audience — **not** child-directed
- Government apps, financial features, health — **No**
- **Permissions declaration** — this is the one that matters. See 4.3.

### 4.3 The dialer permission declaration

The app requests `READ_PHONE_STATE` and `CALL_PHONE` and holds `ROLE_DIALER`. Expect a
questionnaire. Answers that are true and help you:

- Core functionality is **default phone app / call screening**. The app cannot do its job without
  the dialer role, because screening a call before it rings is only possible for the default
  dialer.
- It requests **no** `READ_CALL_LOG`, `WRITE_CALL_LOG` or SMS permissions. Verify before
  submitting and say so explicitly:
  ```bash
  grep -nE "CALL_LOG|READ_SMS|RECEIVE_SMS" androidApp/src/main/AndroidManifest.xml
  ```
  (Currently: no matches. It keeps its own log rather than reading the system one.)
- Record the demo video (§3.2 of the other doc): default-dialer setup → an incoming call ringing
  → a blocked call rejected → the call log entry with its reason. Unlisted YouTube, ~60s.

**Done when:** App Content shows no outstanding items.

---

## Phase 5 — Internal testing

### 5.1 Pre-flight

```bash
./scripts/verify.sh --release
```

All green, including both lint tasks and iOS.

### 5.2 Install the *release* build on a real device and use it

```bash
./install_android.sh --release --device <serial>
./scripts/device_check.sh --device <serial>
```

This is the first time R8 runs against a build anyone depends on. Serialization, Koin and the
Telecom entry points are all shrink-sensitive; `proguard-rules.pro` covers them but has never
been exercised on a device. **Specifically exercise on the release build:** an incoming call,
a blocked call, the auto-responder, and backup export/import.

### 5.3 Upload

```bash
./gradlew :androidApp:bundleRelease
# androidApp/build/outputs/bundle/release/androidApp-release.aab
```

Console → **Internal testing → Create new release** → upload the `.aab` → release notes → roll out.

### 5.4 Add testers

Create an email list (up to 100). **Testers must open the opt-in URL before they can install** —
this is the step people forget and then report "I can't find the app".

### 5.5 What to ask testers to check

Give them a short list, because "try it out" produces nothing useful:

1. Set it as your default phone app.
2. **Does the phone actually ring?** (Highest-risk item — see Phase 8.)
3. Block a number, have it call you: does it stay silent?
4. Does the call log say *why* it was blocked, in your language?
5. Anything crash?

**Done when:** the track is live and at least two testers on different manufacturers have
confirmed 5.5 #2 and #3.

---

## Phase 6 — Closed testing

Play wants evidence of testing before production, and for a personal developer account created
recently there is a **12-tester / 14-day continuous closed-testing requirement** before you can
apply for production access. Check the current rule in the Console — it has changed twice — but
plan for it rather than discovering it at the end.

Same upload flow as Phase 5, with a larger tester list. Use the time to widen device coverage;
the ringtone is the thing most likely to behave differently per OEM.

**Done when:** the closed track has run for the required period with the required tester count.

---

## Phase 7 — Production

### 7.1 Apply for production access

If your account requires it (see Phase 6), this is a separate form describing your testing.

### 7.2 Create the production release

Bump `versionCode` (**every** upload needs a higher one, even a re-upload of identical code) and
`versionName` to something meaningful — `1.0.0` if this is the real first release.

```kotlin
versionCode = 2
versionName = "1.0.0"
```

Upload the `.aab`, write real release notes, then **staged rollout** — start at 20%, not 100%.

### 7.3 Watch it

Console → Quality → **Android vitals**. Crash rate and ANR rate. Play collects these natively;
it is not analytics and does not change your Data Safety answer. If the ringer misbehaves on an
OEM you did not test, this is where it shows up. Halt the rollout rather than shipping over it.

**Done when:** rollout at 100% with vitals inside Play's bad-behaviour thresholds.

---

## Phase 8 — Known risks specific to this app

Read this before Phase 5, not after.

- **The ringtone is the highest-risk change in the codebase and has never been proven against a
  real inbound call.** The app declares `IN_CALL_SERVICE_RINGING`, which tells Telecom *it* rings
  — so if `CallRinger` fails on a given OEM, that device rings for nothing and the user misses
  calls silently. Test on at least two manufacturers before leaving internal testing.
- **`android.hardware.telephony` is declared `required="true"`.** Honest for a dialer, and it
  removes tablets and ChromeOS from your Play availability. If you want tablet testers or tablet
  users, change it to `required="false"` **and** make the app degrade gracefully with no
  telephony — right now that path is untested.
- **Default-dialer apps get closer review.** The "live in minutes" promise for internal testing
  assumes an app nobody looks at twice. Budget days for the first submission.
- **No crash reporting by design.** Play's own vitals are your only signal, and they are
  aggregate. Ask testers for `adb logcat` output directly.
- **The auto-responder is experimental and may be inaudible to the caller.** It reaches them only
  by acoustic coupling through the handset speaker. Do not describe it in the listing as a
  feature that works reliably.
- **The package name still says `bloqueador`.** Cosmetic, unchangeable after upload, not worth
  fixing at the cost of a permanent inability to update.

---

## Quick reference — the whole sequence

```
0. Decide: package name, free/paid, Play App Signing, account type    (irreversible)
1. Create account, verify identity, payments profile                  (longest lead time)
2. Generate upload key, back it up, wire keystore.properties, verify signature
3. Host privacy policy; screenshots; icon; feature graphic; listing copy
4. Create app; complete every App Content section; permissions declaration + demo video
5. ./scripts/verify.sh --release; test the RELEASE build on a device; upload; add testers
6. Closed testing for the required tester count / duration
7. Apply for production; bump versionCode; staged rollout from 20%; watch vitals
```

Blocking today: **Phase 1** (no account verified yet) and **Phase 2.1** (no key exists). Nothing
in Phases 3–4 depends on either, so they can start now.
