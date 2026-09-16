# Release notes — 1.6.2 (versionCode 10), production — built 2026-09-16, uploaded 2026-09-16

Release name (Console, 50-char cap):

```
1.6.2 (10) — call screen and call log fixes
```

Production notes, same posture as `RELEASE_NOTES_1.6.1.md`: ask the reader to do nothing.

Counted, not estimated: **en-US 386, es-419 435, pt-BR 428, hi-IN 358 characters** — all four
under Play's 500 cap.

**Version code 10 confirmed free before the build**, by asking rather than trusting this file —
codes 6, 7 and 8 were each recorded here as unspent and each turned out to already be uploaded.
9 (1.6.1) is the latest live release; nothing newer has been uploaded since.

**Four languages, because the app ships four.** pt-BR and hi-IN still only reach anyone once those
languages are added to the store listing itself (`docs/STORE_LISTING.md:395` — still pending).

<es-419>
Ahora se respeta el No Molestar mientras el teléfono suena. La respuesta automática se silencia sola en cuanto termina el saludo, para no seguir grabando después.

Nuevo: una acción de un toque para «colgar y bloquear» en la pantalla de llamada.

Corregido: un diálogo del registro de llamadas que se volvía a abrir solo, un número del teclado que se reescribía al cambiar de pestaña, y un texto que mostraba «%d» en vez de un número.
</es-419>
<en-US>
Do Not Disturb is now honoured while a call is ringing. The auto-responder mutes itself once the greeting finishes, so it can't keep recording past that point.

New: a one-tap "hang up & block" action on the call screen.

Fixed: a call-log dialog that reopened on its own, a dial pad number that retyped itself after switching tabs, and text that showed a raw "%d" instead of a number.
</en-US>
<pt-BR>
Agora o Não Perturbe é respeitado enquanto o telefone toca. A resposta automática se silencia sozinha assim que a saudação termina, para não continuar gravando depois.

Novo: uma ação de um toque para "desligar e bloquear" na tela de chamada.

Corrigido: uma caixa de diálogo do registro de chamadas que reabria sozinha, um número do teclado que se reescrevia ao trocar de aba, e um texto que mostrava "%d" em vez de um número.
</pt-BR>
<hi-IN>
अब फ़ोन बजते समय डू नॉट डिस्टर्ब का पालन होता है। स्वतः उत्तर, ग्रीटिंग खत्म होते ही खुद म्यूट हो जाता है, ताकि उसके बाद रिकॉर्डिंग न चलती रहे।

नया: कॉल स्क्रीन पर एक-टैप "काटें और ब्लॉक करें" बटन।

ठीक किया: कॉल लॉग का डायलॉग जो अपने आप फिर खुल जाता था, डायलपैड का नंबर जो टैब बदलने पर फिर से टाइप हो जाता था, और वह टेक्स्ट जो नंबर के बजाय "%d" दिखाता था।
</hi-IN>

## What is in this build

- **Do Not Disturb honoured when ringing** (`a4c583b`).
- **Auto-responder mutes the uplink once the greeting ends** (`0d237c3`) — it can no longer record
  past that point.
- **Compose format args are positional** (`cf21606`) — `%d` no longer reaches the user; the four
  Play Console advisories this release does *not* touch are unrelated (see `docs/PLAY_ADVISORIES.md`).
- **Contact matcher stressed with 100 near-miss numbers** (`34953c3`) — test coverage only, no
  behaviour change; the known hole (a contact saved without a country code) is documented, not
  fixed — see `docs/STATUS.md` Open questions.
- **Full-screen call UI no longer flashes for a call about to be blocked** (`dde6e4c`) — waits up
  to 700 ms for the block decision before showing the incoming-call screen.
- **The return-to-call notification survives "Notifications" being turned off** (`5263fd0`).
- **New: "Hang up & block" / "Decline & block" action on the call screen** (`8f37fef`), later
  corrected so it always ends the call actually on screen during call waiting (`37479c6`).
- **Call log: a dismissed caller-actions dialog no longer reopens** on revisiting the screen
  (`9354a93`); the same class of bug fixed on the keypad, where an `ACTION_DIAL` number could
  retype itself after a tab switch (`4a6b33c`).
- **A call-log write failure can no longer leave a blocked call unblocked** (`be89c5b`) — the log
  write is isolated from the block decision.

## Verified before this bundle

- 779 automated tests (`:shared` 666, `:androidApp` 113), `./tools/verify.sh` green 2026-09-16,
  iOS compile included (`:shared:iosSimulatorArm64Test` is CI-only, not run locally).
- Release bundle built and installed on a physical device (motorola razr 50 ultra,
  `ZY22JZJDNH`) — app launches, no crash on open.

- **All three on-device rounds from `docs/STATUS.md` Next action ran and passed on 2026-09-16**:
  DND-while-ringing from a second phone, call-waiting during "hang up & block", and the
  full-screen gate/auto-responder trigger. Uploaded on that basis.

## Before uploading

- [x] **Checked the next free code by asking, not by trusting this file.** 2026-09-16: 9 (1.6.1)
      is latest live, nothing newer uploaded, so 10 is free.
- [x] Confirmed the filename is `10-1.6.2-release.aab`; mtime 2026-09-16 14:36, 4,972,098 bytes
      against 1.6.1's 4,950,941; signing certificate SHA-256 `4CA33D30…46E0`, identical to 1.6.1's
      — same upload key.
- [x] Audited permissions with `bundletool dump manifest` on the **artifact**: unchanged from
      1.6.1 — 7 permissions, `uses-feature android.hardware.telephony` (required), microphone
      not required, `versionCode='10' versionName='1.6.2'`, targetSdk 36.
- [x] Installed the **release** build and opened the app on physical hardware (razr 50 ultra) —
      launches clean.
- [x] **The three hardware verification rounds in `docs/STATUS.md` Next action** — ran and passed
      2026-09-16.
- [ ] **Do not re-open the full-screen-intent declaration form** — see `SUBMISSION_0.1.0.md` §5b.
- [ ] Screenshots are from 2026-08-20; no UI changed in this release except the new call-screen
      action, which does not appear in the current screenshot set. Consider retaking if the
      listing is reworked before upload.
- [ ] Re-triage the four Play Console advisories against this build's `mapping.txt` per
      `docs/PLAY_ADVISORIES.md`'s checklist — not done yet, they were last triaged against 9.
- [ ] Write `docs/RELEASE.md` (the release-tracking state file) once this bundle actually uploads.
