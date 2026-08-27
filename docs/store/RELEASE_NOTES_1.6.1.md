# Release notes — 1.6.1 (versionCode 9), production — uploaded 2026-08-27

Release name (Console, 50-char cap — this one is 38):

```
1.6.1 (9) — blocked calls always end
```

Production notes. Unlike `RELEASE_NOTES_1.6.0.md`, these ask the reader to do nothing: a "what to
test" line on a public listing reads as an admission the build is unverified.

Counted, not estimated: **es-419 494, en-US 467, pt-BR 493, hi-IN 440 characters** — all four under
Play's 500 cap. Both the English and Spanish first drafts were over it.

**Uploaded 2026-08-27. Code 9 is now spent** — the next build takes 10 or higher, whatever happens
to this bundle in review. Written down the same day it was uploaded, because that is precisely the
step that was missed for 6, 7 and 8.

**Version code 9 confirmed free before the build.** The Console was read on 2026-08-27: `8 (1.6.0)` was uploaded on
2026-08-21 and is **live at 100%**, with nothing uploaded since. This is the first release in a
while whose code was checked before the bundle was built rather than after Play refused it —
codes 6, 7 and 8 were each recorded in this repo as unspent and each had already been uploaded.

**This is a production update, not a test track.** 1.6.0 is at 100%, so 9 replaces it for everyone
who has the app; the notes below are written for that audience and ask the reader to do nothing.

**Four languages, because the app ships four.** pt-BR and hi-IN still only reach anyone once those
languages are added to the store listing itself — a Console change, not a file.

<es-419>
Corrección importante: una llamada bloqueada ahora siempre termina. Si algo la contestaba antes de que la app la rechazara —el bolsillo, un botón de auriculares, el sistema—, la llamada seguía en curso aunque el aviso dijera «Llamada bloqueada». Ahora se cuelga siempre.

La respuesta automática también cuelga si el saludo no llega a sonar, por ejemplo cuando el teléfono no tiene voz en tu idioma.

Activarla ahora pide confirmación y avisa de que el teléfono contestará solo, con el altavoz.
</es-419>
<en-US>
Important fix: a blocked call now always ends. If something answered it before the app could reject it — a pocket, a headset button, the system — the call stayed connected even though the notification said "Blocked call". Now it is always hung up.

The auto-responder also hangs up when the greeting never plays, for example on a phone with no voice for your language.

Switching it on now asks for confirmation, and says your phone will answer by itself, on speaker.
</en-US>
<pt-BR>
Correção importante: uma chamada bloqueada agora sempre termina. Se algo a atendia antes de o app recusá-la — o bolso, um botão do fone, o sistema —, a chamada continuava em andamento mesmo com o aviso "Chamada bloqueada". Agora ela é sempre encerrada.

A resposta automática também desliga quando a saudação não chega a tocar, por exemplo num telefone sem voz instalada no seu idioma.

Ativar a resposta automática agora pede confirmação e diz que o telefone vai atender sozinho, no viva-voz.
</pt-BR>
<hi-IN>
ज़रूरी सुधार: अवरुद्ध कॉल अब हमेशा समाप्त होती है। अगर ऐप के अस्वीकार करने से पहले कोई उसे उठा लेता था — जेब, हेडसेट का बटन, या सिस्टम — तो "अवरुद्ध कॉल" सूचना के बावजूद कॉल जारी रहती थी। अब वह हमेशा काट दी जाती है।

स्वतः उत्तर भी कॉल काट देता है जब ग्रीटिंग बजती ही नहीं, जैसे उस फ़ोन पर जिसमें आपकी भाषा की आवाज़ इंस्टॉल नहीं है।

स्वतः उत्तर चालू करने पर अब पुष्टि माँगी जाती है और साफ़ बताया जाता है कि फ़ोन खुद, स्पीकर पर, कॉल उठाएगा।
</hi-IN>

## What is in this build

- **A blocked call always ends.** `Call.reject()` is applied by Telecom only to a call that is
  still ringing; anything that answered first turned it into a silent no-op while the "Blocked
  call" notification was posted anyway. `BlockedCallPolicy` now picks the action from the call's
  state, an unknown state hangs up rather than hopes, and a watchdog re-checks three seconds later.
- **The auto-responder cannot hold a call open any more.** Text-to-speech had three ways to report
  nothing at all — a failed engine init that was never rebuilt, a missing voice for the device
  language, and `speak()` returning ERROR without calling the listener. All three now report
  completion, and two deadlines (10s if the greeting never makes a sound, 60s if it started and
  never finished) end the call regardless.
- **Switching the auto-responder on is confirmed.** A dialog names what the user will see — the
  phone answers, the loudspeaker comes on, up to a minute longer with recording — and that calls
  which are not blocked are never answered. Off is unchanged and asks nothing.
- **Smaller artifact.** `android.r8.optimizedResourceShrinking=true`, so resources are shrunk with
  R8's own reachability analysis rather than the standalone shrinker's approximation.
- `androidx.activity` 1.9.3 → 1.13.0.

## Verified before this bundle

- 731 automated tests (`:shared` 633, `:androidApp` 98) plus 369 on the iOS simulator, all green,
  and `./scripts/verify.sh --release` including R8.
- On a Pixel 8 Pro API 36 emulator, `scripts/blocked_call_test.sh auto`, asserting from Telecom's
  own per-call history: a blocked call rejected at 0.9s having never connected; a blocked call
  answered at 3.4s and hung up by the app at 4.2s with `cause=LOCAL`; and an auto-responder call
  answered at 1.1s and ended at 6.9s.
- Both new behaviours were watched failing first: reverting the policy's `else` to `REJECT` fails
  three tests, and wiring the switch straight back to the repository fails three more.

**Not verified:** nothing in this release has run on a physical phone. The reporter's device is a
Redmi Note 13 Pro on Android 16 (HyperOS), which is the platform the bug was reported from and the
one most worth confirming on.

## Before uploading

- [x] **Check the Console for the next free code first.** Done 2026-08-27: 8 (1.6.0) live at 100%
      since 2026-08-21, nothing newer, so 9 is free.
- [x] Confirm the filename is `9-1.6.1-release.aab`, and check its mtime and size — this project
      has seen `BUILD SUCCESSFUL in 1s` over an unchanged artifact. Built 2026-08-27 12:30,
      4,950,941 bytes against the 1.6.0 bundle's 5,429,071; signing certificate SHA-256
      `4CA33D30…46E0`, identical to the 1.6.0 bundle's, so it is the same upload key.
- [x] Audit permissions with `aapt2 dump badging` on the **artifact**: done, unchanged from 1.6.0
      despite the androidx bump — 7 permissions, `uses-feature-not-required:
      android.hardware.microphone`, `uses-feature: android.hardware.telephony` (required,
      deliberately), `versionCode='9' versionName='1.6.1'`, targetSdk 36.
- [x] Install the **release** build and open the app before uploading. Done on a Pixel 8 Pro API
      36 AVD in Spanish: welcome, permission checklist and Home all render with their strings and
      icons intact, Settings reports `1.6.1 (9)` from the package, the new auto-responder dialog
      renders in full and Cancel leaves the switch off, and with the default action set to Block
      through the UI a live call went `SET_RINGING → REQUEST_REJECT 1.1s → SET_DISCONNECTED
      (REJECTED) 1.3s`. Nothing was stripped by the new shrinker.
- [ ] **Do not re-open the full-screen-intent declaration form.** Its *No* to the pre-grant
      question is what cleared the code-3 rejection — see `SUBMISSION_0.1.0.md` §5b.
- [ ] Screenshots are from 2026-08-20 and still show this app's screens accurately; no UI changed
      in this release except one dialog. Retake with `scripts/seed_screenshots.sh` only if the
      listing is being reworked anyway.
