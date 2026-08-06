# Appeal — `USE_FULL_SCREEN_INTENT` rejection

Standing text for the Play Console appeal / declaration form. Paste-ready.

**App:** Corta Spam — `org.carlospinan.cortaspam`
**Rejected version code:** 1
**Finding:** *Full-Screen Intent Permission policy: Permission use is not directly related to your
app's core purpose.*

---

## Position

The app's core function is **receiving phone calls** — one of the two categories the policy
auto-grants this permission to. Corta Spam is a **default phone app** (dialer replacement), not a
utility that happens to show call notifications.

The policy
([reference](https://support.google.com/googleplay/android-developer/answer/16558241#full_screen_intent))
grants the permission where core functionality is *setting an alarm* or *receiving phone or video
calls*.

---

## Appeal text (paste into the form)

```
Corta Spam is a default phone app (dialer replacement). Its core function is receiving
incoming phone calls: it holds ROLE_DIALER and is bound by Telecom as the device's
InCallService, which makes it responsible for ringing the phone and for presenting the
incoming-call screen. This falls under the "receiving phone or video calls" category that the
Full-Screen Intent policy auto-grants.

The permission is used for exactly one thing: showing the incoming-call screen when a call
arrives. Android does not permit an app to launch an activity from the background when the
screen is off or locked, which is the ordinary case for an incoming call. A notification with
setFullScreenIntent is the platform-sanctioned mechanism for a phone app to present a ringing
call over the lock screen, and it is what every default phone app uses.

Without this permission a user cannot answer a call that arrives while the phone is locked,
because the app that Android has made responsible for that screen cannot show it.

Verifiable in the submitted bundle:

- AndroidManifest.xml declares an InCallService with both
  android.telecom.IN_CALL_SERVICE_UI and android.telecom.IN_CALL_SERVICE_RINGING metadata.
  IN_CALL_SERVICE_RINGING means Telecom stops ringing for incoming calls and this app is
  required to do it instead.
- AndroidManifest.xml declares the android.intent.action.DIAL intent filters required for
  ROLE_DIALER eligibility, and declares android.hardware.telephony as required="true" — the
  app is not installable on a device that cannot receive calls.
- The incoming-call UI is the activity .telecom.InCallActivity, declared with
  showWhenLocked="true" and turnScreenOn="true".
- There is exactly one setFullScreenIntent call in the entire application
  (telecom/IncomingCallNotifier.notifyIncomingCall), and its PendingIntent targets
  InCallActivity. The permission is used for no other purpose — no ads, no promotions, no
  re-engagement notifications. The app contains no advertising SDK and no analytics SDK.
- The source is public and auditable at github.com/cpinan/Corta-Spam (MIT).

The Play listing is categorised Communication, with tags Caller ID and Communication.
```

---

## Short version — **this is the text actually submitted**

The Console appeal box caps at **1000 characters**. Submitted 2026-08-06 under reason
*"I believe that this is incorrect"* — not *"I have fixed this issue"*, which would read as
*I removed the permission* and be refuted by the bundle itself.

979 characters, 987 if the box counts line breaks as CRLF. Do not pad it; every trim below was
made to fit. Paste as-is, no line rewrapping.

```
Corta Spam is a default phone app. Its core function is receiving phone calls, one of the categories the Full-Screen Intent policy auto-grants.

AndroidManifest.xml declares an InCallService with both IN_CALL_SERVICE_UI and IN_CALL_SERVICE_RINGING metadata. IN_CALL_SERVICE_RINGING means Telecom stops ringing and this app is responsible for ringing the phone and showing the incoming-call screen.

The permission has one use: showing that screen. Android blocks background activity launches when the screen is off or locked, so setFullScreenIntent is the only supported way to show a ringing call there. Without it, a call arriving on a locked phone cannot be answered.

The app has one setFullScreenIntent call, targeting the incoming-call activity. It is not used for ads, analytics or re-engagement. Source: github.com/cpinan/Corta-Spam

The declaration form has now been submitted, selecting "making and receiving calls". It was not submitted when this version was reviewed.
```

The load-bearing fact is `IN_CALL_SERVICE_RINGING`: it is not a claim about intent, it is a
manifest declaration meaning Android has made this app responsible for ringing the phone. A
reviewer can verify it in the bundle in seconds. Everything else in the text supports it.

Replies to appeals arrive in Chinese, English, Japanese or Korean only.

---

## If the appeal is refused

Fallback, in this order:

1. **Ask what evidence is missing** before changing code — the manifest facts above are not
   disputable, so a second refusal usually means the reviewer never saw the declaration form.
2. **Remove the permission.** `IncomingCallNotifier` keeps its `CallStyle` notification and
   degrades to a max-priority heads-up with Answer/Decline on the lock screen; what is lost is
   the automatic takeover into `InCallActivity`. Touch points:
   - `androidApp/src/main/AndroidManifest.xml` — the `uses-permission` line
   - `telecom/IncomingCallNotifier.kt` — the `setFullScreenIntent` call
   - `MainActivity.kt` — `fullScreenIntentAllowed`, `canUseFullScreenIntent()`,
     `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`
   - `App.kt`, `navigation/AppNavHost.kt`, `settings/SettingsScreen.kt` — the parameter and the
     warning card threaded through them
   - `settings_fullscreen_disabled` + `_desc` in all four locales under
     `shared/src/commonMain/composeResources/`
   - `notification_channel_incoming_calls_desc` in `androidApp/src/main/res/` says "Full-screen
     alert" and would become untrue
   - bump `appVersionCode` in `androidApp/build.gradle.kts` — Play needs a new version code, and
     the flagged bundle must end up under "Not included"
