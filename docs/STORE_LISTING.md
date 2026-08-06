# Play Store listing copy — Corta Spam

Paste-ready. Assets live in [`store/`](store/).

Two rules applied throughout: **do not promise detection accuracy** (this app blocks by *your*
rules plus a small bundled list — it is not a crowd-sourced spam database), and **do not describe
the auto-responder as reliable** (it reaches the caller only by acoustic coupling).

---

## App name (≤30 chars)

```
Corta Spam
```
*10 characters.*

## Short description (≤80 chars)

```
Block calls by your own rules. Open source, no ads, nothing leaves your phone.
```
*77 characters.*

## Full description (≤4000 chars)

```
Corta Spam screens incoming calls before your phone rings, using rules you write yourself.

No ads. No tracking. No accounts. No network access at all — every rule and every log entry stays in a database on your device, and the app has no code that could send it anywhere.

WHAT YOU CAN BLOCK

• Specific numbers — with a label so your call log tells you why
• Patterns — block a whole range with wildcards, like +34900* or *1234
• Countries — block a dialling code you never expect a call from
• Quiet hours — silence everything on a schedule, except your allowlist
• Repeat callers — block a number after it tries too many times in a short window
• A bundled spam list — known scam dialling codes, shipped inside the app

Your contacts and your allowlist always come through, and a number you block manually stays blocked even if it is in your contacts. You decide what wins.

WHAT HAPPENS TO A BLOCKED CALL

Your phone stays silent. The call log records what happened and which rule made the decision, in your language. You can call back or copy any number straight from the log.

You choose what happens to a number no rule matched: let it through, block it, or let it through but flag it for review. There is also an optional setting to let an unknown number through once it has tried several times — because a real person calling repeatedly is usually not a robocall.

PRIVACY

The app requests no access to your system call log, SMS, storage, location, camera or microphone. It reads your contacts only to recognise callers you know, and that never leaves the device.

There is no analytics SDK and no crash-reporting SDK. The full privacy policy is published alongside the source code, and you can check every claim in it against the code.

OPEN SOURCE

Every line is public and auditable at github.com/cpinan/Corta-Spam, under the MIT licence.

LANGUAGES

English, Spanish, Portuguese and Hindi.

NOTE ON PERMISSIONS

Corta Spam must be set as your default phone app. That is the only way Android lets an app screen a call before it rings — it is how the blocking works, not an extra request.

The auto-responder is experimental. It answers a blocked call and plays a greeting, but on modern Android the caller may not hear it clearly; treat it as a bonus, not a feature to rely on.
```
*~2,050 characters.*

---

## Category and contact

| Field | Value |
|---|---|
| Category | **Tools** — not Communication. It is a utility, and Communication draws stricter dialer scrutiny. |
| Tags | Call blocking, Privacy, Open source |
| Contact email | *(a monitored address — required and shown publicly)* |
| Website | `https://github.com/cpinan/Corta-Spam` |
| Privacy policy | `https://cpinan.github.io/Corta-Spam/PRIVACY` |

## Assets

| Asset | File | Spec | Status |
|---|---|---|---|
| App icon | `store/ic_play_512.png` | 512×512 PNG | ✅ 512×512 |
| Feature graphic | `store/feature_1024x500.png` | 1024×500, no alpha | ✅ no alpha |
| Screenshot 1 | `store/01_home.png` | phone, 9:16 | ✅ 1344×2992 |
| Screenshot 2 | `store/02_lists.png` | | ✅ |
| Screenshot 3 | `store/03_patterns.png` | | ✅ |
| Screenshot 4 | `store/04_calllog.png` | | ✅ |
| Screenshot 5 | `store/05_settings.png` | | ✅ |

Captured on the `Pixel_8_Pro_API_33` AVD with seeded demo data. Every phone number shown is from
a range that cannot be a real subscriber (`+34 900` is Spanish premium-rate/service) — these
images are public once uploaded.

## Release notes — first internal release

```
First internal build.

Please check: set Corta Spam as your default phone app, then confirm the phone actually rings for a normal call, and stays silent for a number you have blocked. The call log should tell you which rule made each decision.
```
