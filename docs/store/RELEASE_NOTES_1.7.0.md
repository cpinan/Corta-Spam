# Release notes — 1.7.0 (versionCode 11), production — built 2026-10-08, not yet uploaded

Release name (Console, 50-char cap):

```
1.7.0 (11) — block by default, first-run guide
```

Production notes, same posture as `RELEASE_NOTES_1.6.2.md`.

Counted, not estimated: **es-419 355, en-US 322, pt-BR 344, hi-IN 335 characters** — all four
under Play's 500 cap.

**Version code 11 confirmed free by the maintainer on the Console, 2026-10-08** — asked, not
inferred from this repo. 10 (1.6.2) was uploaded 2026-09-16.

**Four languages, because the app ships four.** pt-BR and hi-IN still only reach anyone once those
languages are added to the store listing itself (`docs/STORE_LISTING.md:395`).

<es-419>
Nuevo: las instalaciones nuevas bloquean por defecto los números desconocidos, así el spam se frena desde la primera llamada (quienes ya usaban la app conservan su ajuste). Una guía breve la primera vez explica cómo usar Ajustes.

Nuevo: añade un número a tus contactos sin salir de la pantalla de llamada.

La pantalla de Créditos suma a una persona más.
</es-419>
<en-US>
New: fresh installs now block unknown numbers by default, so spam is stopped from the very first call (existing installs keep their current setting). A short first-run walkthrough shows how to use Settings.

New: add a number to your contacts without leaving the call screen.

The Credits screen now lists one more person.
</en-US>
<pt-BR>
Novo: instalações novas agora bloqueiam números desconhecidos por padrão, então o spam é barrado desde a primeira chamada (quem já usava o app mantém sua configuração). Um guia rápido na primeira vez mostra como usar os Ajustes.

Novo: adicione um número aos contatos sem sair da tela de chamada.

A tela de Créditos agora cita mais uma pessoa.
</pt-BR>
<hi-IN>
नया: नए इंस्टॉल में अब अनजान नंबर डिफ़ॉल्ट रूप से ब्लॉक होते हैं, इसलिए स्पैम पहली ही कॉल से रुक जाता है (पहले से इस्तेमाल करने वालों की सेटिंग वही रहती है)। पहली बार एक छोटा गाइड दिखाता है कि सेटिंग्स कैसे इस्तेमाल करें।

नया: कॉल स्क्रीन छोड़े बिना किसी नंबर को संपर्कों में जोड़ें।

क्रेडिट्स स्क्रीन में अब एक और व्यक्ति का नाम है।
</hi-IN>

## What is in this build

- **Fresh installs block unmatched numbers by default** (`fa6eb47`). Existing installs that never
  chose a default keep ALLOW — an unset value reads as ALLOW once the welcome screen has been seen.
  `setWelcomeShown` pins the fresh-install BLOCK so it survives the next launch.
- **First-run Settings walkthrough** (`cb1b91f`, `0fc77a9`) — shown once, also to existing users
  after the update; strings in all four locales.
- **Add a number to contacts from the call screen** (`046ec82`).
- **Credits: Christopher (Kris) Gead** (`6a52cc8`), name only.
- Listing screenshots retaken (`8e00445`) — see below.

## Verified before this bundle

- 784 automated tests plus 3 for the tutorial dialog (787: `:shared` 674, `:androidApp` 113),
  `./tools/verify.sh` green 2026-10-08, iOS compile included.
- Debug build on a fresh `Pixel_8_Pro_API_36` AVD: tutorial shown once, `default_action=BLOCK`,
  not shown again after relaunch.
- **Release** build (`11-1.7.0-release.apk`, R8) installed on the same AVD after an uninstall, with
  nothing granted but the dialer role: welcome → permissions → tutorial, no crash in the crash log.
- NOT run on a physical device; the three on-device call rounds from 1.6.2 were not repeated
  because no call-handling code changed in this build.

## Before uploading

- [x] Next free code asked of the maintainer, not trusted from this file: 11.
- [x] Filename `11-1.7.0-release.aab`, 4,981,685 bytes, built 2026-10-08 16:27; signing
      certificate SHA-256 `4CA33D30…46E0`, identical to 10's — same upload key; `jarsigner -verify`
      passes.
- [x] Permissions audited with `bundletool dump manifest` on the **artifact**: byte-identical to
      10 apart from `versionCode='11' versionName='1.7.0'` — 7 permissions, telephony required,
      microphone not required, targetSdk 36.
- [ ] Upload `11-1.7.0-release.aab`; paste the release name and notes above.
- [ ] Replace the listing screenshots with `docs/store/play/{01..05}_*.png` (en-US) and
      `docs/store/play/es_*` (es-419, the default language). 1683x2992, 9:16.
- [ ] **Do not re-open the full-screen-intent declaration form** — `SUBMISSION_0.1.0.md` §5b.
- [ ] 10 (1.6.2) was still in review at last check — Play may take both; check the Console.
- [ ] Re-triage the four Play advisories against this build's `mapping.txt`
      (`docs/PLAY_ADVISORIES.md`) once the Console shows them.
