# Release notes — 1.3.0 (versionCode 5), internal testing

Release name (Console, internal only, 50-char cap — this one is 44):

```
1.3.0 (5) internal — answered-call crash fix
```

Tester track. **Do not paste these into production** — they ask the reader to try things, which on
a public listing reads as an admission the build is unverified. Production notes get their own file.

Counted, not estimated: **es-419 478 characters, en-US 455**, both under Play's 500 cap.

<es-419>
Corregido: contestar una llamada cerraba la app y el teléfono pasaba tu llamada al marcador del sistema a mitad de conversación.

Busca tus contactos desde el Teclado. El registro ya incluye las llamadas salientes, con filtros y búsqueda. Una llamada entrante muestra el nombre del contacto o tu propia etiqueta.

Tocar la notificación de una llamada perdida o bloqueada abre las acciones de ese número.

Qué probar: contesta una llamada, busca un contacto y filtra el registro.
</es-419>
<en-US>
Fixed: answering a call crashed the app, so the phone handed your call to the system dialer mid-conversation.

Search your contacts from the Keypad. The call log now shows outgoing calls too, with filters and search. A ringing call shows the contact's name, or your own label for that number.

Tapping a missed or blocked call notification opens that caller's actions.

What to test: answer a call and stay in Corta Spam; search a contact; filter the log.
</en-US>

## Why 5, and why 1.3.0

**Code 4 was uploaded to the internal track on 2026-08-13.** A code is spent on upload, not on
publication, so Play will not accept it a second time. The name moves to 1.3.0 rather than 1.2.1
because this is not a patch on what 1.2.0 shipped.

## What changed since 1.2.0 (code 4, internal)

- **Answering a call killed the app.** Android 14 refuses a `CallStyle` notification that is not
  tied to a foreground service or a user-initiated job and carries no full-screen intent — and it
  refuses it by throwing out of `notify()`, on the main thread, inside a Telecom callback. The
  "return to call" notification is posted the moment a call goes `ACTIVE`, so the process died
  exactly when the other person picked up. Telecom then handed the live call to the preloaded
  dialer, which is what a tester sees as the system phone app taking over mid-conversation.
  **Every user on Android 14+ hit this on every answered call.** 1.2.0 (4) is affected.
- **Contact search on the keypad.** One field searches names and numbers while it dials.
- **Outgoing calls are logged,** so the call log is a recents list rather than an incoming-only
  history. New `direction` column, schema version 4, migration `3.sqm`.
- **Call-log filters:** search by name or number, All / Incoming / Outgoing / Blocked, and
  Today / This week / This month.
- **The call screen names the caller** — contact name, or the user's own block/allowlist label.
- **Tapping a blocked, missed or repeat-caller notification** opens the call log with that
  caller's actions already open. Previously the notification body did nothing.
- **The platform's duplicate missed-call notification is suppressed,** by declaring the receiver
  Telecom's `MissedCallNotifierImpl` looks for before posting its own.
- **`tel:` numbers containing `#` are no longer truncated** when dialled from the keypad.

## Verified before this cut

`./scripts/verify.sh --release` green — both platforms, ktlint, Android Lint, SQLDelight migration
verification, **528 tests**, plus the R8/minified build. Re-run after the timestamp-localization
fix below; the uploaded artifact is the 15:41 rebuild, not the 15:18 one.

**Artifact audited on the APK, never the source manifest:** `versionCode='5' versionName='1.3.0'`,
targetSdk 36, `android.hardware.microphone` as `uses-feature-not-required`, `faketouch` the only
`uses-implied-feature`. `aapt2 dump permissions` lists `CALL_PHONE`, `READ_CONTACTS`,
`POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `VIBRATE`, `RECORD_AUDIO` and androidx's own
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — **no `READ_PHONE_STATE`**. That last one needed
checking this time: the new missed-call receiver names `READ_PHONE_STATE` in its
`android:permission` attribute, and the string does appear in the merged manifest, but as an
enforcement on senders rather than a `uses-permission`. The badging dump is the proof.
`jarsigner -verify` → `jar verified`, `CN=Carlos Pinan`. R8 mapping present, same timestamp as
the artifacts (2026-08-14 15:41).

**On a `Pixel_8_Pro_API_36` emulator, running the R8 release build, not a debug one:**

- **The upgrade path.** A real **schema-v3** database (pulled off the razr before this work) was
  planted under a fresh install of this build. On first launch `3.sqm` ran: `user_version` is 4,
  the pre-existing call-log row survived and reads `INCOMING`, and `idx_call_log_action_time`
  exists again after the table rebuild.
- **An answered call.** Ringing showed this app's own `InCallActivity`; after answering, the call
  reached `state=ACTIVE`, the activity was **still** the resumed one (not the preloaded dialer),
  the process id was unchanged either side of the answer, the ongoing notification posted on
  `ongoing_call` with its Hang up action, and logcat contains **zero** `FATAL EXCEPTION` lines.
  This is the exact path that killed 1.2.0 (4).
- **Rule decisions still land**: with the planted database's `default_action=BLOCK`, the first test
  call was blocked and rejected (`BLOCKED|INCOMING` in the log); switched to `ALLOW`, the next one
  rang through and was answered (`ALLOWED|INCOMING`).

**One bug was found after the first cut and fixed before upload.** Taking the Spanish store
screenshots showed the call log's dates still in English — "Aug 14, 2026" — because they were
assembled from an English enum constant in shared code. Every one of the app's non-English locales
was affected. Fixed by delegating to the platform date formatters, re-verified, and the artifact
rebuilt. The screenshots in `docs/store/` are from that rebuilt release build.

**On the razr 50 ultra (API 36), on the debug build of the same commits:** ringing works — the
oldest open risk in this project, and the first time it has been proven off an emulator. Contact
search against a real 1,900-contact address book, call-log filters, outgoing calls appearing in
the log with the contact's name, and the v3→v4 migration on the owner's real database.

**Not verified:** an answered call on the razr with the *release* build specifically (it was
answered on the debug build, and on the release build on the emulator). Ring volume was measured
at 1/7 on the handset's own slider — the app plays at the level the slider says, but nothing has
been tested at a normal ring volume.
