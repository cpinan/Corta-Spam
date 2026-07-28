# Store Compliance

## Google Play Declaration

### Default Phone App (Android 10+)

BloqueaLlamadas requests `ROLE_DIALER` (default phone app) to screen incoming calls before the phone rings. This is the only way Android lets a third-party app see the caller's number and decide whether to allow, block, or answer the call.

**Core purpose justification:** Call screening and blocking. The app checks every incoming call's number against user-configured rules (manual blocklists, pattern matching, country blocking, schedule rules) plus an optional community spam provider, and either silences the call, plays an auto-responder greeting, or allows it through normally.

**Required permissions:**
- `READ_PHONE_STATE` — read the incoming call's phone number for screening
- `CALL_PHONE` — required by Android for dialer role eligibility
- `READ_CONTACTS` — optional: auto-allow contacts (user must grant explicitly)
- `BIND_INCALL_SERVICE` — handle incoming calls via `InCallService`

### Accessibility / Default Apps

The app declares itself eligible for the default dialer role via the standard `android.intent.action.DIAL` intent filter and `InCallService` with `IN_CALL_SERVICE_UI` and `IN_CALL_SERVICE_RINGING` metadata. No accessibility service is used. No `BIND_NOTIFICATION_LISTENER_SERVICE`, `BIND_ACCESSIBILITY_SERVICE`, or screen-reading APIs are declared.

---

## Privacy Policy

### Data collection

BloqueaLlamadas does **not** collect, transmit, or share any personal data. Specifically:

- **No analytics or telemetry.** The app includes no analytics SDKs, no crash reporters, and no usage tracking of any kind.
- **No phone-home behavior.** The app never contacts any server except when the user explicitly enables the optional spam provider (see below). Even then, only the dialed/ringing number is sent to check against a public spam database — no contact data, no device identifiers, no call audio.
- **No advertising.** The app has no ads and no ad SDKs.

### Data stored locally

All user data stays on the device in a local SQLite database:

| Data | Purpose | User control |
|---|---|---|
| Blocked/allowed numbers and patterns | Call screening rules | User can add/remove at any time |
| Country codes to block | Call screening by country | User can toggle per country |
| Schedule rules (time windows) | Time-based call blocking | User can add/remove at any time |
| Action rules (attempt thresholds) | Frequency-based blocking | User can add/remove at any time |
| Call log (number, timestamp, outcome) | Show call history and stats | No export; cleared when app is uninstalled |
| Auto-responder script and audio | Custom greeting for blocked callers | User can edit or disable at any time |
| Settings (toggles, preferences) | App configuration | User can change at any time |

### Network access

The app accesses the network only when:

1. **Spam provider is enabled** (off by default). The app queries a public, community-maintained spam-number database. Only the calling number is sent in the request. No other data leaves the device. The user can disable this at any time in Settings.
2. **No other network requests.** The app does not download content, check for updates, or connect to any other service.

### Data sharing

No data is shared with any third party under any circumstances. The app has no servers, no user accounts, and no cloud storage. Export is done via manual file sharing (the user selects where to save/load a JSON file), which is fully under the user's control.

### Open source

The app's complete source code is available at its public repository. Every line of code, including all network request logic, can be audited independently.

---

## App Store Review Notes (iOS — deferred)

iOS implementation is pending. When implemented:

- The app will register a `CallDirectory` extension to label and silently block known spam numbers using Apple's "Silence Unknown Callers" system.
- A `Live Caller ID Lookup` extension may be added for on-demand number lookup against the bundled spam list.
- No call answering, audio playback, or recording is possible on iOS (platform limitation).
- All data handling will follow the same local-only, no-telemetry policy described above.
