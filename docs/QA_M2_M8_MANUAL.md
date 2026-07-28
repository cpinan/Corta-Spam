# M2-M8 manual QA — real-call regression on physical hardware

Not automatable: real telephony is required to exercise `evaluateCall()`
end-to-end on a device that actually receives calls. The emulator pass
(`Pixel_8_Pro_API_33`, `adb emu gsm call`) already covered the resolver logic,
migrations, and UI wiring — this pass is specifically for the things that
can only be seen/heard on real hardware: audible auto-responder playback,
real dual-SIM/call-waiting behavior, and confirming nothing regressed for a
real caller.

Covers rule precedence (M2), pattern/country rules (M3/M4), action-based
blocking (M5), auto-responder (M6), spam-provider stub (M7), and quiet
hours (M8) on top of the M1 default-dialer foundation.

## Setup

- Device: motorola razr 50 ultra (`ZY22JZJDNH`) or equivalent, connected via
  `adb devices`.
- Install/relaunch: `~/Projects/install_bloquea_llamadas.sh` (builds,
  installs, launches — does not set default dialer, see step 1).
- A second phone number you can both call from and receive calls on (a
  second personal phone, a friend/family member's number, or a second SIM).
  Have it handy for the whole session — most steps below call the device
  from this number.
- Note the current default dialer before starting (Settings → Apps →
  Default apps → Phone app) so it can be restored afterwards.

## 1. Onboarding

1. Fresh install if the app isn't already installed
   (`adb uninstall org.carlospinan.bloqueador.app` first, then re-run the
   install script). Launch it.
2. **Expect:** permission-explainer screen (not M1 detail — just confirm it
   appears; full onboarding state-machine coverage is in
   `docs/QA_M1_MANUAL.md`).
3. Tap **Continue**, accept the OS "set as default phone app" dialog.
4. **Expect:** app lands on Home, stats show 0/0/0 (fresh install) or
   whatever's left over from prior testing.

## 2. Manual block list

5. Home → **Manage block lists** → **Blocked Numbers** → **Add** → enter
   your second number → **Add**.
6. **Expect:** number appears in the list, count badge updates to 1.
7. Call the device from that number.
8. **Expect:** no ring, no UI shown, call rejected silently within ~2-3
   seconds of connecting.
9. Home → **View call log**.
10. **Expect:** an entry for that number, marked blocked, reason shows
    "Manually blocked" (or your label if you set one).
11. Remove the number from Blocked Numbers, call again.
12. **Expect:** this time it rings through normally (default action —
    should be "Allow" unless you changed it in Settings).

## 3. Contacts allowlist

13. Save your second number as a real contact on the device (Contacts app),
    if not already saved.
14. Add that same number to **Blocked Numbers** again.
15. Settings → confirm **Auto-allow contacts** is **on**.
16. Call from that number.
17. **Expect:** rings through normally — contacts bypass the block even
    though the number is also in the manual block list (allowlist beats
    manual block in precedence order).
18. Settings → turn **Auto-allow contacts** off.
19. Call again.
20. **Expect:** now blocked — same number, same contact, but the bypass
    setting is off so the manual block applies.
21. Turn Auto-allow contacts back on, remove the number from Blocked
    Numbers before continuing.

## 4. Pattern rule

22. **Manage block lists → Pattern Rules → Add**. Enter a pattern that
    matches your second number's prefix (e.g. if it's `+1555xxxxxxx`, use
    `+1555*`).
23. Call from that number.
24. **Expect:** blocked, call log reason shows the pattern (or your label).
25. Toggle the pattern rule off (switch in the list), call again.
26. **Expect:** rings through — disabled rules don't apply.
27. Remove the pattern rule before continuing.

## 5. Country rule

28. **Manage block lists → Country Rules → Add**. Pick the country your
    second number belongs to.
29. Call from that number.
30. **Expect:** blocked, call log reason shows "Country: <name> (<code>)".
31. Remove the country rule before continuing.

## 6. Action rule (repeated calls)

32. **Manage block lists → Action Rules → Add**. Set attempts=3,
    window=5 minutes.
33. Call from your second number, let it ring through (don't answer),
    hang up. Repeat 3 times within 5 minutes.
34. **Expect:** first 2 calls ring through normally; the 3rd is blocked
    (call log reason mentions repeated calls).
35. Remove the action rule before continuing.

## 7. Quiet hours

36. **Manage block lists → Quiet Hours → Add**. Set a window covering the
    current time (e.g. now to 1 hour from now, in HH:mm).
37. Call from your second number (not on any other list).
38. **Expect:** blocked silently, call log reason "Quiet hours" (or your
    label).
39. Add that number to the **Allowlist**.
40. Call again.
41. **Expect:** rings through — allowlist overrides an active quiet-hours
    window (the core M8 acceptance test).
42. Remove the allowlist entry and the quiet-hours rule before continuing.

## 8. Auto-responder (needs actual ears)

43. Settings → **Auto-responder** → enable it, set a short script (e.g.
    "This number is currently unavailable").
44. Add your second number to Blocked Numbers.
45. Call from that number.
46. **Expect:** call auto-answers, and you **hear** the TTS greeting play
    over the call audio.
47. If you enable recording: confirm the consent line is present in the
    script and can't be removed while recording is on, leave a test
    message after the greeting, then check the recording was saved
    locally (Settings → Auto-responder, or wherever recordings list).
48. Turn auto-responder off, remove the number from Blocked Numbers.

## 9. Call-waiting (resource-leak regression)

49. Have two people/numbers available, or use call-waiting if your carrier
    supports it. Start a call from number A (let it ring through /
    answer it).
50. While that call is active, have number B call in.
51. **Expect:** the app doesn't crash, freeze, or silently drop audio for
    call A when call B arrives; the second call gets its own
    evaluation (ring, block, or auto-respond per whatever rules apply to
    it) independent of the first.

## 10. Stats live-refresh

52. After the blocked calls above, note Home's "Blocked today/week/month"
    counts.
53. Background the app (home button, don't force-stop), wait a moment,
    reopen it via the launcher.
54. **Expect:** counts are current, not stale from before backgrounding
    (this was a real bug found and fixed in this branch — regression
    check).

## Teardown

55. Clean up any test rules left over (blocked numbers, patterns,
    countries, action rules, quiet hours, allowlist entries) so `main`
    doesn't inherit test data expectations — this is just local device
    state, not committed anywhere, but keep it tidy for the next test pass.
56. Restore the previous default dialer if needed (Settings → Apps →
    Default apps → Phone app), unless keeping BloqueaLlamadas as default
    intentionally.

## Known gaps (expected, not bugs)

- `DefaultAction.ASK` behaves identically to `ALLOW` — no dedicated
  review UI exists yet (documented, deliberate scope decision).
- Spam-provider toggle (M7) is a no-op stub — nothing to test behaviorally
  yet beyond the settings toggle persisting.
- iOS has none of M2-M8 wired up to a `CallDirectory` extension yet — this
  QA pass is Android-only, matching the milestone scope.
