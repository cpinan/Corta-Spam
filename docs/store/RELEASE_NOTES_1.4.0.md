# Release notes — 1.4.0 (versionCode 6), internal testing

Release name (Console, internal only, 50-char cap — this one is 46):

```
1.4.0 (6) internal — contacts blocked by mistake
```

Tester track. **Do not paste these into production** — they ask the reader to try things, which on
a public listing reads as an admission the build is unverified. Production notes get their own file.

Counted, not estimated: **es-419 440 characters, en-US 433 characters**, both under Play's 500 cap.

<es-419>
Corregido: alguien de tu agenda podía quedar bloqueado igualmente. Pasaba cuando el contacto estaba guardado con prefijo (+34...) y la llamada llegaba sin él.

El registro ahora indica si un número está en tu lista de bloqueo o de permitidos, y al tocarlo puedes desbloquearlo desde ahí.

El contestador explica qué puede y qué no puede grabar en tu teléfono.

Qué probar: llama desde un contacto guardado con prefijo y comprueba que suena.
</es-419>
<en-US>
Fixed: someone in your contacts could still be blocked. It happened when the contact was saved with a country code (+34...) and the call arrived without one.

The call log now shows whether a number is on your block list or allowlist, and tapping it lets you unblock from there.

The auto-responder explains what it can and cannot record on your phone.

What to test: call from a contact saved with a country code and check it rings.
</en-US>

## Why 6, and why 1.4.0

**Code 5 was uploaded to the internal track**, so it is spent — a code is spent on upload, not on
publication, and Play will not take it twice. The name moves to 1.4.0 rather than 1.3.1 because
the headline change is not a patch on 1.3.0: a number in the user's own address book could still
be blocked, which is the failure a call blocker most needs not to have.

## What changed since 1.3.0 (code 5, internal)

- **A contact in the address book could still be blocked.** `PhoneNumberParser.sameNumber` was
  fixed on 2026-08-11 to compare numbers by deriving the country code from the number itself.
  `AndroidContactsGateway` then went on handing it numbers it had already put through
  `normalizeForComparison`, which strips the `+`. A contact saved `+34611998877` therefore arrived
  stating no country, so the national form could no longer bridge it to a call delivered as
  `611998877` — which is ordinary for a domestic call. The contact was not allowlisted and any
  pattern, country, quiet-hours or default-block rule blocked them. The contact's **name** rendered
  correctly throughout, because the name map is keyed differently and was never wrong; that is what
  made the symptom look inexplicable to the people reporting it.
- **The call log shows block state, and toggles it.** A row now says whether its number is on the
  block list or allowlist right now, and tapping it offers Unblock / Remove from allowlist. It used
  to offer Block regardless, so blocking an already-blocked caller was a tap with no visible
  effect. Deliberately distinct from the call's own outcome: a call blocked by a rule since deleted
  still reads "Blocked call" and carries no badge.
- **A custom auto-responder greeting was never played on a real call.** The file picker used
  `ACTION_GET_CONTENT`, whose read grant is scoped to the picking activity's task and is long gone
  by the time the in-call service reads the URI. The exception was swallowed and the blocked caller
  was answered and hung up on in silence. Now `ACTION_OPEN_DOCUMENT` with a persisted grant, and a
  fallback to the spoken script if the audio still cannot be opened.
- **The recorder is owned per call**, not per service: with two calls in progress the one that
  ended first stopped the other's recording and filed its audio under its own call-log row.
- **The auto-responder says why recording will not run** — responder off, greeting invalid,
  microphone not granted — and a card states the limits it cannot predict: only blocked calls are
  answered, the greeting reaches the caller acoustically, and recording captures the microphone
  rather than the call, so some phones capture nothing.
- **Two strings rendered a literal backslash.** Compose Multiplatform resources are read as plain
  XML, where Android's `\'` escape means nothing, so the duplicate-number dialog had been showing
  `won\'t override the block` for as long as it existed.

## Verified before this cut

`./scripts/verify.sh --release` green — both platforms, ktlint, Android Lint, SQLDelight migration
verification, **571 tests**, plus the R8/minified build. The iOS test job, which had been red on
`:shared:iosSimulatorArm64Test`, is green: Kotlin/Native rejects a comma inside a backticked test
name, and `viewModelScope` work needs `Dispatchers.setMain` there or the test hangs for a minute.
`verify.sh` now compiles `commonTest` for Native, which it never did.

**Artifact audited on the APK, never the source manifest:** `versionCode='6' versionName='1.4.0'`,
targetSdk 36, `android.hardware.microphone` as `uses-feature-not-required`, `faketouch` the only
`uses-implied-feature`. `aapt2 dump permissions` lists `CALL_PHONE`, `READ_CONTACTS`,
`POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `VIBRATE`, `RECORD_AUDIO` and androidx's own
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — **no `READ_PHONE_STATE`**. `jarsigner -verify` →
`jar verified`, `CN=Carlos Pinan`. R8 mapping present at the same timestamp as the artifacts.

**On a `Pixel_8_Pro_API_36` emulator, on the debug build**, `./scripts/rule_matrix_test.sh` reports
**14 passed, 0 failed, 0 skipped** — every block and allow branch driven through a real emulated
call, including two new contact scenarios. `./scripts/ring_test.sh auto` verifies both halves:
an unmatched number rings with this app's own player, and a blocked number produces no player at all.

**The contacts fix was proven by A/B on a real call path, not by reasoning.** A new phase F places
a call from the national form of a contact saved internationally. With the fix reverted, rebuilt and
reinstalled, it reports `expected ALLOWED/CONTACTS, got BLOCKED|-` — the user's report reproduced
exactly. Phase E, the mirror direction added in August, passed against that same broken build, which
is why F had to exist.

## Not verified

- **An answered call on the razr with the release build.** Still the gap carried over from 1.3.0.
- **Ring volume at a normal level on the razr** — last measured with the handset's own slider at 1/7.
- **The store screenshot of the call log (`03_calllog.png`) predates the block-state badge.** It is
  not wrong, only older than the UI. Retake before any production promotion.
