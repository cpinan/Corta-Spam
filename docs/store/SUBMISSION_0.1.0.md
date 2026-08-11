# Submission pack — 0.1.0 (versionCode 3), internal testing

Everything needed to publish, in the order the Console wants it. Field values are paste-ready.
`RELEASE_STATUS.md` is the running state; this file is the submission itself.

**Prepared 2026-08-11.** versionCode 1 was rejected; versionCode 2 was uploaded and withdrawn
after Play warned it dropped 6 devices. **versionCode 3 is the one to publish.**

---

## 0. The artifact

| | |
|---|---|
| File | `androidApp/build/outputs/bundle/release/corta-spam-0.1.0-3-release.aab` |
| Size | 5.2 MB |
| applicationId | `org.carlospinan.cortaspam` |
| versionCode / versionName | **3** / `0.1.0` |
| minSdk / targetSdk | 26 / 36 |
| Signature | `jar verified` — `CN=Carlos Pinan, OU=Casa, O=Casa, L=Lima, ST=Lima, C=PE` (upload key) |
| Locales in bundle | en, es, hi, pt |

> **Two other bundles sit in the same tree and neither is the upload:** `-1-` was rejected under the
> Full-Screen Intent policy, `-2-` was uploaded and withdrawn after Play warned it dropped 6 devices.
> The filenames differ by one digit. `-1-` is already moved to `rejected/`; move `-2-` too.

**Hardware features** (`aapt2 dump badging`, on the artifact):

- `android.hardware.telephony` **required** — deliberate. An app that cannot receive calls has no
  function, and this costs tablet and ChromeOS availability.
- `android.hardware.microphone` **not required** — declared explicitly in versionCode 3. Without
  that line, `RECORD_AUDIO` makes the tooling imply the feature as *required*, which is what dropped
  6 devices from versionCode 2. Confirm on any future build that badging says
  `uses-feature-not-required`, not `uses-implied-feature`.

**Permissions in the built bundle** (audited with `unzip -p … base/manifest/AndroidManifest.xml`,
not read off the source manifest):

`CALL_PHONE` · `READ_CONTACTS` · `POST_NOTIFICATIONS` · `USE_FULL_SCREEN_INTENT` · `RECORD_AUDIO` ·
`VIBRATE` · `org.carlospinan.cortaspam.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

- `READ_PHONE_STATE` was removed 2026-08-11 — declared with no caller. Verified gone from the
  artifact and from the installed app on device.
- The androidx permission is merge residue from `androidx.core`, app-private, grants nothing. Kept
  deliberately: stripping a permission a library needs fails as a runtime `SecurityException`.
- `BIND_INCALL_SERVICE` and `DUMP` appear in a raw bundle dump. Those are `android:permission`
  **guards** on our `InCallService` and on androidx's `ProfileInstallReceiver` — not requests. Do
  not "explain" them on any form.

**Native code:** only `libandroidx.graphics.path.so` (Compose), four ABIs. The
*"contains native code, no debug symbols"* warning on upload is noise — there is no first-party
native code and no symbol file that could exist.

---

## 1. App content — COMPLETE (2026-08-11)

| Section | Value | Note |
|---|---|---|
| Full-screen intent declaration | Core functionality **receiving phone / video calls**, pre-grant **Yes** | Submitted **first**, deliberately. Its absence — not the permission — caused the v1 rejection |
| Privacy policy | `https://cpinan.github.io/corta-spam/privacy.html` | Canonical. **Not** the old `…/Corta-Spam/PRIVACY` copy |
| Data safety | **Collects no data** | True by construction: no `INTERNET` permission, no HTTP client in the dependency graph |
| Ads | **No** | No ad SDK in any build file |
| Content rating | IARC, category *Utility / Productivity / Communication / Other*, all content questions No | "Users interact with each other" answered **No** — calls are carried by the network, there is no in-app messaging surface |
| Target audience | **13-15, 16-17, 18+** | No band under 13 — that triggers the Families programme and a separate review |
| Sensitive permissions | **Nothing to declare** | That form is triggered by the Call Log and SMS permission groups. This app declares neither — it reads calls through `InCallService` and keeps its own database |

---

## 2. Store listing

Full copy in [`../STORE_LISTING.md`](../STORE_LISTING.md). **Re-paste it** — it was corrected on
2026-08-11 (it claimed the app requests no microphone access, which `RECORD_AUDIO` makes false).

**Default language is `es-419`.** English is a translation.

| Field | es-419 (default) | en-US |
|---|---|---|
| App name | `Corta Spam` (10) | `Corta Spam` (10) |
| Short description | 75 chars | 73 chars |
| Full description | 3,472 chars | 3,196 chars |

Category **Communication**, tags **Caller ID, Communication**. Do not switch to Tools — a Tools app
is not a calling app, and calling-app classification is the entire full-screen-intent argument.

Contact email `carlos.pinan@gmail.com` · Website `https://github.com/cpinan/Corta-Spam` ·
Support `https://cpinan.github.io/corta-spam/`

### Graphics — these fall back to the DEFAULT language, not English

| Asset | es-419 (default) | en-US |
|---|---|---|
| Icon 512×512 | `store/ic_play_512.png` | same (common) |
| Feature 1024×500 | `store/play/feature_1024x500.png` (**Spanish**) | `store/play/feature_1024x500_en.png` |
| Screenshots | `es_01_home`, `es_02_calllog`, `es_03_lists`, `es_04_settings` | `01_home`, `02_lists`, `03_patterns`, `04_calllog`, `05_settings` |

All screenshots are 1683×2992, exactly 9:16, padded by edge replication. **Upload from
`docs/store/play/`, never `docs/store/`** — the originals there are 1344×2992 (9:20) and Play
rejects them.

---

## 3. Release notes

Paste-ready both languages in [`RELEASE_NOTES_0.1.0.md`](RELEASE_NOTES_0.1.0.md).
Counted, not estimated: **es-419 450 / 500**, **en-US 426 / 500**.

Written for a first-time installer, not an upgrader — neither versionCode 1 nor 2 ever reached a
tester, so versionCode 3 is the first build anyone sees.

---

## 4. Demo video

Reviewers ask for one on dialer apps, and it is the strongest exhibit for the full-screen-intent
declaration. Recorded 2026-08-11 on the API 33 emulator (the only place a phone can be made to ring
on demand), app locale forced to `en-US` since the declaration is in English.

Shot order: Home → block lists → **screen locked** → call from an unknown number wakes the screen
and shows the full-screen incoming-call UI over the lock screen → Answer → a manually blocked
number calls and the phone stays dark → call log showing both decisions and the rule behind each.

**Caption it on YouTube: the recording has no audio.** `screenrecord` captures no sound, and
ringing is the load-bearing claim — a reviewer watching silently should be told why it is silent.

**Uploaded 2026-08-11, unlisted:**

```
https://www.youtube.com/watch?v=x15aUnHav6w
```

Shared originally as `https://youtube.com/shorts/x15aUnHav6w?feature=share` — same video. Give the
Console the `watch?v=` form: some fields validate the URL pattern, and the `feature=share` param is
tracking noise. Check it loads in a logged-out window; a video left **Private** looks fine to the
owner and 404s for a reviewer.

---

## 5. Upload, in order

1. Move every bundle except `-3-` out of the output folder (`-1-` is already in `rejected/`).
2. Internal testing → Create release → upload `corta-spam-0.1.0-3-release.aab`.
3. **Accept Play App Signing when offered. This is one-way** — declining makes a lost upload key
   fatal.
4. Paste release notes, both languages.
5. Decide **managed publishing before review completes**. Off means approval publishes immediately,
   with no gap between "review passed" and "live".
6. Check **form factors** — only phone applies. `android.hardware.telephony` is `required="true"`,
   which already excludes tablets and ChromeOS from the listing.
7. Add testers, then **send them the opt-in link** — they cannot install without it.
8. Send for review **once**, from Publishing overview, after every edit is in. Each send is its own
   review cycle.

---

## 5b. Publishing to Production instead

Two gates decide whether this is even possible today. Check both **before** filling anything in.

**Gate 1 — closed testing requirement.** A Play developer account registered as **personal** after
13 Nov 2023 cannot publish to production until it has run a closed test with **at least 12 testers
opted in continuously for 14 days**, and then been granted production access on application.
**Organisation** accounts are exempt. If this account is personal and has never run that test, the
14-day clock starts the day 12 testers are opted in — production is at minimum two weeks away, and
nothing in this repo changes that.

**Gate 2 — the ringtone is unproven on OEM hardware.** The app declares `IN_CALL_SERVICE_RINGING`,
which means Telecom stops ringing and hands the job to this app. Verified working on an emulator
only. If a manufacturer reserves audio during a call, that phone rings for nothing and **the user
silently misses calls** — the worst failure a phone app has. In internal testing that costs a bug
report; in production it costs uninstalls and one-star reviews, and every review cycle to fix it
runs against a live listing.

Neither gate is a reason not to prepare production. They are reasons to have someone phone the razr
first.

### Production-only fields (everything in §1-§3 applies unchanged)

| Field | Value |
|---|---|
| Track | Production → Create new release |
| Countries / regions | Choose explicitly. Defaults to none selected; a release with no country ships nowhere |
| Rollout percentage | **Staged, start low (5-10%)**. Full rollout cannot be undone — halting stops new installs but does not remove the app from those who already have it |
| Pricing | Free. **One-way**: a free app can never be made paid |
| App category | Communication (already set) |
| Content rating | Already submitted; production reuses it |
| Managed publishing | Decide before review completes. Off means approval publishes immediately |
| Device catalogue | `android.hardware.telephony required="true"` already excludes tablets and ChromeOS. Expect a smaller supported-device count and do not "fix" it — an app that cannot receive calls has no function |

### Order for production

1. Confirm Gate 1 by opening **Production → Country availability**; if production access is not
   granted, the Console says so there and the rest is moot.
2. Prove ringing on the razr with a real inbound call.
3. Production → Create new release → upload the same `corta-spam-0.1.0-3-release.aab`.
4. Accept Play App Signing if not already enrolled (one-way).
5. Release notes — the same two blocks, but reword them: the current text asks testers to *check*
   things, which is wrong copy for a public listing.
6. Select countries, set a staged rollout percentage.
7. Send for review once, from Publishing overview.

**Release notes need rewriting for production.** `RELEASE_NOTES_0.1.0.md` is addressed to testers
("Please check: …"). Shipping that to the store tells the public the app is unverified.

## 6. What is verified, and what is not

**Verified on the API 33 emulator, 2026-08-11:**

- A blocked number logs `BLOCKED` with the real number, the matching rule type and the real rule id
- **The phone rings.** Telecom logged `Ringer: Ending early -- letDialerHandleRinging=true`, standing
  down because of `IN_CALL_SERVICE_RINGING`, and the app's own MediaPlayer then played
  `USAGE_NOTIFICATION_RINGTONE` with a matching RINGTONE vibration. Both stopped when the call ended
- The full-screen intent wakes a locked screen and presents `InCallActivity` with Answer/Decline
- `Call.Details.handle` still arrives with `READ_PHONE_STATE` removed
- Action rules fire in the real path — three calls in five minutes were blocked as `ACTION`

**Verified on the razr 50 ultra:** release build installs and runs clean, no exceptions,
`READ_PHONE_STATE` absent from the installed permission set.

**NOT verified, and an emulator cannot verify it:**

- **Ringing on OEM hardware.** A manufacturer that reserves audio or vibration during a call would
  mean users miss calls silently. This needs a person to phone the razr while Corta Spam holds the
  dialer role.
- **Auto-responder recording against a live call.** `RECORD_AUDIO` ships in this bundle for a feature
  that has never captured a real call. Several manufacturers lock the microphone during calls; the
  listing and privacy policy both say so.
