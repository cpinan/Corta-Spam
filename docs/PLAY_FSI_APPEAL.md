# `USE_FULL_SCREEN_INTENT` — two rejections and the way out

**App:** Corta Spam — `org.carlospinan.cortaspam`
**Finding, both times:** *Full-Screen Intent Permission policy: Permission use is not directly
related to your app's core purpose.*

| Version code | Declaration form | Listing | Outcome |
|---|---|---|---|
| 1 | never submitted | led with call blocking | rejected 2026-08-06 |
| 3 | submitted, pre-grant **Yes** | opens with "phone app" | rejected 2026-08-12 |

---

## What the second rejection settled

The version-code-1 theory was *the reviewer never saw the declaration form*. Code 3 was submitted
with the form filled in first, deliberately, and with listing copy rewritten to open with "phone
app" in both languages. It was rejected anyway. So the theory is dead, and with it the assumption
that the fix was a paperwork problem.

The manifest facts were never in dispute and still are not. The app declares an `InCallService`
with both `IN_CALL_SERVICE_UI` and `IN_CALL_SERVICE_RINGING`, which means Telecom stops ringing
and hands the job here; it declares the `ACTION_DIAL` filters `ROLE_DIALER` requires; it declares
`android.hardware.telephony` as `required="true"`. There is exactly one `setFullScreenIntent` call
in the codebase, in `telecom/IncomingCallNotifier.notifyIncomingCall`, targeting `InCallActivity`.
The [policy](https://support.google.com/googleplay/android-developer/answer/16558241#full_screen_intent)
auto-grants the permission where core functionality is *receiving phone or video calls*.

Being right did not get the app published twice.

## The resolution — decline the pre-grant (2026-08-12)

The declaration form has two questions, and only the first was ever read carefully here:

1. **Core functionality** → *Making and receiving calls*. Unchanged. It is true.
2. **Install behaviour — pre-grant at install?** → changed from **Yes** to **No**.

The rejection is a verdict on question 2. *"Permission use is not directly related to your app's
core purpose"* is Play's answer to a request to have the app-op granted at install without the
user being asked. Answering **No** withdraws the app from that review entirely. The permission
stays in the manifest, `setFullScreenIntent` stays in the code, and the user grants the app-op
themselves from system settings.

**The permission is not removed and no code was deleted for policy reasons.**

## What it costs

Every install on Android 14+ now starts with the ringing screen switched **off**. A call arriving
on a locked phone shows the `CallStyle` notification — `IMPORTANCE_HIGH`, `setBypassDnd(true)`,
`VISIBILITY_PUBLIC`, Answer and Decline both present — but does not take over the screen until the
user grants the app-op.

That is worse than a pre-grant and much better than deleting the feature, which was the only other
option on the table. It also means the in-app route to granting it is now load-bearing rather than
a fallback, which is what `PermissionChecklist` was changed for: full-screen intent is a row in the
onboarding checklist, not just the third-ranked warning banner on a screen where only the first is
rendered.

## Do not confuse this with the platform check

`NotificationManager.canUseFullScreenIntent()` is an API 34+ call. Below that the app-op does not
exist, the platform shows the ringing screen without being asked, and both the checklist row and
the warning banner are correctly absent — `MainActivity.refreshPermissionStatus` hard-codes
`fullScreenIntentAllowed = true` there. A consequence worth knowing before testing: **the
ungranted state cannot be reproduced on an API 33 emulator at all.** The only AVD this project had
was `Pixel_8_Pro_API_33`, on which every one of these warnings is unreachable dead code.

## If the pre-grant is ever wanted back

The permission is auto-grant *eligible* — the manifest supports it, and nothing below has been
weakened. Opting back in means answering **Yes** to install behaviour and going through review
again. The 1000-character appeal box text below was written for that, submitted once under reason
*"I believe that this is incorrect"* (not *"I have fixed this issue"*, which reads as *I removed
the permission* and is refuted by the bundle itself). Kept verbatim; every trim in it was made to
fit the limit, so do not pad or rewrap it.

```
Corta Spam is a default phone app. Its core function is receiving phone calls, one of the categories the Full-Screen Intent policy auto-grants.

AndroidManifest.xml declares an InCallService with both IN_CALL_SERVICE_UI and IN_CALL_SERVICE_RINGING metadata. IN_CALL_SERVICE_RINGING means Telecom stops ringing and this app is responsible for ringing the phone and showing the incoming-call screen.

The permission has one use: showing that screen. Android blocks background activity launches when the screen is off or locked, so setFullScreenIntent is the only supported way to show a ringing call there. Without it, a call arriving on a locked phone cannot be answered.

The app has one setFullScreenIntent call, targeting the incoming-call activity. It is not used for ads, analytics or re-engagement. Source: github.com/cpinan/Corta-Spam

The declaration form has now been submitted, selecting "making and receiving calls".
```

Replies to appeals arrive in Chinese, English, Japanese or Korean only.

## The option that was not taken

Removing the permission outright. Recorded here because it was costed, and because a third
rejection would put it back on the table:

- `androidApp/src/main/AndroidManifest.xml` — the `uses-permission` line
- `telecom/IncomingCallNotifier.kt` — the `setFullScreenIntent` call
- `MainActivity.kt` — `fullScreenIntentAllowed`, `canUseFullScreenIntent()`,
  `openFullScreenIntentSettings()`
- `permissions/PermissionChecklist.kt` — the `FULL_SCREEN_INTENT` row
- `App.kt`, `navigation/AppNavHost.kt`, `settings/SettingsScreen.kt` — the parameter and the
  warning card threaded through them
- `settings_fullscreen_disabled` + `_desc`, `permissions_fullscreen_title` + `_body` in all four
  locales under `shared/src/commonMain/composeResources/`
- `notification_channel_incoming_calls_desc` in `androidApp/src/main/res/` says "Full-screen
  alert" and would become untrue
- bump `appVersionCode` in `androidApp/build.gradle.kts`

An open question that would decide how much is actually lost, and which nobody has tested:
`PassthroughInCallService.onCallAdded` already calls `startActivity(InCallActivity)` directly. The
comment on `IncomingCallNotifier` asserts that this only reaches the screen when the app is
already foregrounded — **that assertion has never been tested with the permission absent.** If
Telecom grants a bound default-dialer `InCallService` background-activity-launch privilege, the
full-screen intent is redundant on modern Android and removing it costs nothing at all. Test it
before believing either answer.
