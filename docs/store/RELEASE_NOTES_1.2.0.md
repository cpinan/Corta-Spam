# Release notes — 1.2.0 (versionCode 4), internal testing

Release name (Console, internal only, 50-char cap):

```
1.2.0 (4) internal — keypad, call back, outgoing fix
```

Tester track. **Do not paste these into production** — they ask the reader to try things, which on
a public listing reads as an admission the build is unverified. Production notes get their own file.

<es-419>
Ya puedes llamar desde Corta Spam: hay una pestaña Teclado, y los enlaces de teléfono de otras apps llegan ahí con el número puesto.

Las llamadas perdidas traen un botón para devolver la llamada. Si alguien insiste, verás un solo aviso con la cuenta de intentos.

Corregido: llamar a un número bloqueado, o llamar en horario silencioso, cortaba tu propia llamada.

Qué probar: llama desde el teclado, llama a un número bloqueado (debe salir) y comprueba que las entrantes se siguen bloqueando.
</es-419>
<en-US>
You can now call from Corta Spam: there is a Keypad tab, and phone links from other apps open it with the number filled in.

Missed calls get a Call back button. A caller who keeps trying leaves one notification with an attempt count.

Fixed: calling a number on your own block list, or calling during quiet hours, hung up your own call.

What to test: call from the keypad, call a number you have blocked (it should go through), and check incoming calls still get blocked.
</en-US>

## What changed since 1.1.4 (code 4, never uploaded)

- **Keypad tab.** Taking the default-dialer role replaces the phone app; until now the only way to
  originate a call was tapping an existing call-log row.
- **`ACTION_DIAL` is handled.** The manifest had declared `tel:` intent filters since the app first
  claimed `ROLE_DIALER` and nothing read them, so Corta Spam was offered for every phone link on the
  device and dropped the number.
- **Call back** on missed and repeat-caller notifications. Not on blocked calls, deliberately.
- **One notification per caller,** carrying an attempt count instead of stacking.
- **Outgoing calls are no longer screened.** The rule engine ran on every call in both directions,
  so a blocklisted number the user dialled — or any number inside a quiet-hours window — had the
  call they placed rejected by their own phone app.

## Verified before this cut

`./scripts/verify.sh` green (both platforms, ktlint, Android Lint, SQLDelight migration, 472 tests).
Artifact audited with `aapt2 dump badging` on the **APK**: `versionCode='4' versionName='1.2.0'`,
targetSdk 36, `android.hardware.microphone` as `uses-feature-not-required`, no `READ_PHONE_STATE`,
`faketouch` the only `uses-implied-feature`. `jar verified`, `CN=Carlos Pinan`. R8 mapping present.

On the `Pixel_8_Pro_API_36` emulator: the `tel:` deep link lands on the keypad pre-filled without
dialling; the keypad places a call; an outgoing call to a blocklisted number and an outgoing call
inside an all-day quiet-hours window both go through untouched, while incoming calls are still
blocked (`BLOCKED|MANUAL`, `BLOCKED|SCHEDULE`); Call back places the call and dismisses its
notification; two missed calls from one number collapse into one reading "2 attempts".

**Not verified on OEM hardware.** No physical device has run any of it, and ringing has never been
proven off an emulator — that is what the internal track is for.
