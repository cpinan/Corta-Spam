# Privacy Policy — Corta Spam

**Last updated: 2026-08-05**

Corta Spam does not collect, transmit, or share any personal data. No analytics, no telemetry,
no advertising, and no network access at all.

## What the app stores, and where

All rules, settings, and call logs are stored locally in a SQLite database on your device. You
can delete them at any time by clearing the app data or uninstalling the app. The in-app
call log also has a "clear log" action.

That database is excluded from Android's cloud backup and from device-to-device transfer, so it
does not leave your phone even when the rest of your device is backed up. If you want your rules
on a new phone, the app has an explicit export in Settings that you trigger yourself and can read
the contents of before sharing.

## Permissions, and why each one exists

| Permission | Why | What leaves the device |
|---|---|---|
| `READ_PHONE_STATE`, `CALL_PHONE` | Required to be Android's default phone app, which is the only way an app can screen a call before it rings | Nothing |
| `READ_CONTACTS` | To recognise callers who are in your contacts and let them through | Nothing. Contacts are read on-device and held in memory for at most five minutes |
| `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT` | To show the incoming-call screen and blocked/missed-call notifications | Nothing |
| `VIBRATE` | The app plays its own ringtone and vibration, because as the default dialer it is responsible for ringing | Nothing |

The app requests **no** access to your system call log, SMS, storage, location, camera or
microphone.

## The spam list

The optional spam provider is a list of known scam dialling codes bundled inside the app itself.
Turning it on does not contact any server: the check happens entirely on your device, and no
number, contact, device identifier or call audio is ever transmitted.

## Call recording

The auto-responder can optionally record. It is off by default, and the app requires your
greeting to state that the call may be recorded before it will let you enable it. Recordings, if
any, stay on your device like everything else.

## Crash reporting

The app contains no crash-reporting SDK. If you install from Google Play, Google itself collects
crash and ANR reports as a platform feature; that is Google's collection under
[their privacy policy](https://policies.google.com/privacy), not the app's, and the app neither
adds to it nor receives the contents.

## Children

The app is not directed at children and collects no data from anyone.

## Changes

This policy is versioned in the app's public repository alongside the code it describes. Its
history is the change log.

## Contact and verification

The app is fully open source. Every line of code can be audited independently at
<https://github.com/cpinan/Corta-Spam> — including the claims on this page. If you find a
discrepancy between this policy and the code, that is a bug: please open an issue.
