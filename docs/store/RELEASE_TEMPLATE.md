# Release template — naming and notes

For every release. Latest cut is 0.1.0 (versionCode 4). Copy the blocks, fill the gaps, count the
characters. This file is the thing to edit when the process changes; the per-version notes files
are snapshots of what was actually shipped.

---

## 1. Release name

The **release name is internal only** — Play never shows it to a user. It appears in the Console
release list, in the Publishing overview, and in the email you get when review finishes. Its whole
job is to let you tell two releases apart six weeks later.

Play caps it at **50 characters**. The default is just `3 (0.1.0)`, which tells you nothing about
why that build exists.

**Format:**

```
<versionName> (<versionCode>) <track> — <why this build exists>
```

**Examples:**

```
0.1.0 (3) prod — mic optional, FSI declared
0.1.1 (4) prod — OEM ringtone fix
0.2.0 (5) internal — iOS call directory
```

Rules that have already cost something here:

- **Name the reason, not the change.** `versionCode 4` and `bump version` are what the version
  fields already say. `OEM ringtone fix` is what you need when three builds are in flight.
- **Say the track.** Codes 1, 2 and 3 of this app went to three different places (rejected, upload
  withdrawn, production). The version number alone does not record that.
- Keep it under 50 characters — the Console truncates silently.

## 2. Version code and name

Both live in one place, `androidApp/build.gradle.kts`:

```kotlin
val appVersionName = "0.1.0"
val appVersionCode = 4
```

The artifact filename derives from them (`corta-spam-<name>-<code>-release.aab`), so they can never
disagree with what you upload.

**A version code is spent the moment it is uploaded, not when it is published.** Play refuses a code
it has ever seen. This app burned code 1 on a policy rejection, code 2 on a one-line manifest fix,
and code 3 on the same policy finding as code 1. Budget for that: a code is cheap, a wrong upload
is not.

**A rejection spends a code even when the fix is not in the bundle.** Code 3's remedy was one answer
on a Console form, and it still cost a rebuild — Play will not re-review a code it has already
ruled on. Do not plan on resubmitting the same artifact after a form change.

## 3. Release notes

One file per version, per track:

- `RELEASE_NOTES_<version>.md` — tester tracks
- `RELEASE_NOTES_PROD_<version>.md` — production

**Cap is 500 characters per language.** Count it, do not estimate — a translation runs longer than
its source and is the one that goes over:

```bash
python3 - <<'PY'
import re
t = open('docs/store/RELEASE_NOTES_PROD_X.Y.Z.md', encoding='utf-8').read()
for lang in ('es-419', 'en-US'):
    b = re.search(rf'<{lang}>\n(.*?)\n</{lang}>', t, re.S).group(1)
    print(f"{lang}: {len(b)} / 500  {'OK' if len(b) <= 500 else 'OVER'}")
PY
```

### Template

```
<es-419>
[Qué cambió, en una línea.]

[Por qué le importa a quien usa la app — no el detalle técnico.]

[Si algo requiere una acción suya, dilo aquí. Es la parte que más se olvida.]
</es-419>
<en-US>
[What changed, in one line.]

[Why it matters to the person using the app — not the technical detail.]

[If anything needs an action from them, say it here. This is the part most often left out.]
</en-US>
```

Both languages, always. This project maintains Spanish/English parity everywhere else and the store
listing default is `es-419` — an English-only note is read by the *majority* of this app's users as
a missing translation.

### Rules

- **Never paste tester notes into production.** The internal notes for 0.1.0 asked the reader to
  confirm the phone rings. On a public listing that reads as an admission the app is unverified.
- **No accuracy promises.** This app blocks by the user's rules plus a small bundled list. It is not
  a spam database and must not imply it is.
- **No price, promo, accolades or licensing language.** Barred outright from the short description,
  and this listing was already flagged once for `Open source, no ads, no tracking`. Release notes
  are not governed by that rule, but the reviewer is the same one.
- **Do not advertise the auto-responder.** Experimental, ships off, and the caller may not hear it.
- **Say when a release needs the user to do something** — grant a permission, re-set the default
  phone app, re-import a backup. A silent behavioural change reads as a bug.

## 4. Before every upload

```bash
./scripts/verify.sh
./gradlew :androidApp:bundleRelease :androidApp:assembleRelease
```

Then audit the **artifact**, never the source manifest — merging adds things nobody wrote, and a
permission can imply hardware:

```bash
AAPT=~/Library/Android/sdk/build-tools/37.0.0/aapt2
$AAPT dump badging androidApp/build/outputs/apk/release/corta-spam-*-release.apk \
  | grep -E 'package:|uses-feature|uses-implied-feature|uses-permission'
jarsigner -verify androidApp/build/outputs/bundle/release/corta-spam-*-release.aab | grep -i verified
```

Check for:

- **`uses-implied-feature`** — a permission has just declared hardware as *required* on your behalf.
  `RECORD_AUDIO` implying `android.hardware.microphone` dropped 6 devices from versionCode 2.
- **Unexpected `uses-permission`** — manifest merging adds them.
  `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` from `androidx.core` is expected and
  deliberately kept.
- `android:permission` **guards** (`BIND_INCALL_SERVICE`, `DUMP`) are not requests. Do not explain
  them on a form and do not try to remove them.
- `jar verified` and the right `CN`.

Move every bundle except the one being uploaded into `rejected/`. Three files one digit apart in one
folder is how the wrong build gets published.
