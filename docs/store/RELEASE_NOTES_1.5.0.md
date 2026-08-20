# Release notes — 1.5.0 (versionCode 7), internal testing

Release name (Console, internal only, 50-char cap — this one is 44):

```
1.5.0 (7) internal — emergency dialling fixed
```

Tester track. **Do not paste these into production** — they ask the reader to try things, which on
a public listing reads as an admission the build is unverified. Production notes get their own file.

Counted, not estimated: **es-419 451 characters, en-US 419 characters**, both under Play's 500 cap.

<es-419>
Corregido: con Corta Spam como tu app de teléfono, marcar un número de emergencia te llevaba a otro marcador en vez de llamar. Ahora llama de una sola vez. Por favor, no pruebes esto en una línea de emergencia real.

El teclado ya no se mueve mientras escribes: cada dígito cae en la tecla que pulsaste.

El modo oscuro ahora cubre también la barra de navegación.

Qué probar: escribe un número largo y rápido, y comprueba que salen todos los dígitos.
</es-419>
<en-US>
Fixed: with Corta Spam set as your phone app, dialling an emergency number handed you to a different dialer instead of placing the call. It now dials in one tap. Please do not test this on a live emergency line.

The keypad no longer moves while you type, so each digit lands on the key you pressed.

Dark mode now covers the navigation bar too.

What to test: type a long number quickly and check every digit is right.
</en-US>

## Why the notes do not ask anyone to dial 112

The headline fix is emergency dialling, and the obvious "what to test" line would be *dial an
emergency number and check it connects*. It is not there, and it must not be added.

Every tester who follows that instruction places a real call to a real emergency service, on a live
network, for a reason that is not an emergency. The fix was verified on an emulator, whose modem is
simulated and reaches nobody. There is no way for a tester on a real handset to check it without
occupying a line that someone else may need, so the notes name the fix and steer the testing at the
keypad instead.

If this ever has to be checked on hardware, the route is a test emergency number provisioned by the
carrier or a lab SIM — not the public one.

## What 1.5.0 is

**1.4.0 (6) is being retired, not shipped.** Its `.aab` was built on 2026-08-14 and never uploaded.
Twenty-one commits landed after it, so the version string on that binary stopped describing the
tree — and it predates the emergency-dialling fix, which is what moves it from stale to
unshippable.

Headline, and the reason this release exists:

- **The app could not dial an emergency number.** It holds `ROLE_DIALER`, so it is the phone app,
  and tapping Call on `112` placed no call: Telecom cancelled it and launched the stock dialer with
  the number pre-filled. `CALL_PHONE` was granted by the role — the missing thing was caller
  identity, not permission. Both call sites now use `TelecomManager.placeCall`.

Also new since 1.4.0 was cut, and none of it has ever been in an uploaded build:

- Dark mode, including the navigation bar.
- Mute, speaker and a call timer on the in-call screen; a DTMF keypad during a call.
- The screen turns off while the phone is against a face.
- An emergency-callback exemption: every call is let through for 30 minutes after the user calls
  the emergency services.
- Call waiting no longer strands the surviving call; a finished call no longer traps the user on
  its screen; Back no longer throws the call screen away.
- Saving a typed number as a new contact, from the keypad.
- The installed version is shown in Settings; the credits screen is filled.
- The dial pad no longer moves while a number is being typed on it.
- Long-pressing the keypad's delete key clears the whole number, instead of removing one digit.

## Before uploading

- [ ] Build the bundle and confirm the filename is `corta-spam-1.5.0-7-release.aab`. Gradle's
      default name is identical for every build ever made; `archivesName` handles this, but check
      the artifact rather than trusting it.
- [ ] Check the artifact's mtime and size before trusting any comparison — this project has seen
      `BUILD SUCCESSFUL in 1s` over an unchanged APK more than once.
- [ ] Audit permissions with `aapt2 dump badging` on the **artifact**, not the source manifest: a
      `uses-permission` silently implies its hardware `uses-feature` as required, and that cost six
      device models once already.
- [ ] **Do not re-open the full-screen-intent declaration form.** Its *No* to the pre-grant
      question is what cleared the code-3 rejection — see `SUBMISSION_0.1.0.md` §5b.
- [ ] `docs/store/play/03_calllog.png` predates the block-state badge and the dark theme. Retake
      before any production promotion.
