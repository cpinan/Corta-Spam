# M1 manual QA — default-dialer real-call regression

Not automatable: real telephony (SIM or emulator radio) is required. Run by
a human against the acceptance test in `docs/MILESTONES.md` M1: *"place and
receive real calls; call quality/behavior is unchanged from the stock
dialer."*

## Setup

- Device or emulator with telephony (e.g. `Pixel_8_Pro_API_33` AVD, which
  has a working virtual modem).
- A second number to call/be called from (a second emulator works: use
  `adb -s <emulator-serial> emu gsm call <other-emulator-port>`, or the
  Extended Controls → Phone panel to simulate an inbound call).
- Note which app is currently the default dialer before starting, so it can
  be restored afterwards (Settings → Apps → Default apps → Phone app).

## Onboarding flow

1. Fresh install (`adb uninstall org.carlospinan.bloqueador.app` first if
   reinstalling). Launch the app.
2. **Expect:** permission-explainer screen appears (hero text "We need to
   become your default phone app", 3 reason bullets, "what we will never
   do" box, Continue / Not now buttons) — matches
   `design/mockups.html` screen 2.
3. Tap **Not now**. **Expect:** app proceeds past onboarding without
   showing any OS dialog (skip path, `DialerOnboardingScreen`'s local
   `skipped` flag).
4. Force-stop and relaunch. Tap **Continue**.
5. **Expect:** the OS dialog appears —
   - API 29+: RoleManager's "Allow Corta Spam to be your default
     phone app?" dialog.
   - API 26–28: the legacy "Change default phone app?" dialog
     (`TelecomManager.ACTION_CHANGE_DEFAULT_DIALER`).
6. Tap **Deny/Cancel**. **Expect:** app shows the "Not set as your default
   phone app" screen with Try again / Continue without it.
7. Tap **Try again**, then accept the OS dialog this time.
8. **Expect:** app proceeds past onboarding (state → GRANTED). Confirm via
   Settings → Apps → Default apps → Phone app that Corta Spam is now
   selected.
9. Force-stop and relaunch. **Expect:** onboarding is skipped entirely —
   goes straight past the explainer (state → ALREADY_DEFAULT on init).

## Real-call regression (pure pass-through, no blocking logic yet)

10. Place an outgoing call to the second number. **Expect:** call connects,
    audio is two-way, mute/speaker toggle behave the same as the stock
    dialer, hang-up ends the call cleanly on both ends.
11. Receive an incoming call from the second number. **Expect:** the call
    rings and can be answered; behavior indistinguishable from the stock
    dialer (no dropped audio, no delayed ringing, no crash).
12. Let one call go to missed (don't answer, let it time out or reject via
    ADB `adb -s <serial> emu gsm cancel <number>` after ringing starts).
13. Open the system **Call log** / **Phone → Recents** and confirm all
    three calls (outgoing, incoming, missed) appear exactly once each with
    correct type and duration. Android's Telecom stack writes these
    automatically regardless of which app is the default dialer —
    verified on-device that a duplicate app-side write produces a second,
    lower-quality row (missing `phone_account_address`/`subscription_id`).
    `PassthroughInCallService` deliberately does not write call-log entries
    itself; this step is a regression check against ever adding that back.
14. Repeat step 10–13 once more after an app force-stop + cold start, to
    rule out state that only works right after a fresh process launch.

## Teardown

15. Switch the default dialer back to whatever it was before step 1
    (Settings → Apps → Default apps → Phone app), unless continuing
    directly into M2 work on this same device/emulator.

## Known gaps at M1 (expected, not bugs)

- No blocking/screening logic yet — every call goes through unchanged.
- No in-app call UI beyond the OS's own incoming-call screen; M1 only
  supplies the minimum `InCallService` contract, not custom call UI.
- Emergency calls always route through the preloaded system dialer
  regardless of `ROLE_DIALER` status — do not use this app to test
  emergency numbers.
