# Release notes — 1.6.0 (versionCode 8), internal testing

Release name (Console, internal only, 50-char cap — this one is 45):

```
1.6.0 (8) internal — contacts tab, new keypad
```

Tester track. **Do not paste these into production** — they ask the reader to try things, which on
a public listing reads as an admission the build is unverified. Production notes get their own file.

Counted, not estimated: **es-419 484, en-US 456, pt-BR 464, hi-IN 388 characters** — all four under Play's
500 cap. The first draft of all four was over it, which is why they are counted rather than eyed.

**Version code 8, after 7 was refused.** This release was first built on code 7, on the repo's own
claim that 7 had never been uploaded. Play answered `Version code 7 has already been used` — so it
had been, and no local record could have known: a code is spent on upload, and an upload leaves no
trace on this machine. That is the second time in four days the same belief was wrong, the first
being code 6.

Both code-7 bundles — 1.5.0 (7) and 1.6.0 (7) — are in `superseded/`. Neither can ever be
uploaded. The name stayed at 1.6.0 because nothing about the app changed between the two builds,
only the number it answers to.

**Four languages, because the app ships four.** pt-BR and hi-IN only reach anyone once those
languages are added to the store listing itself — a Console change, not a file. Until then they
cost nothing and are ready.

<es-419>
Nueva pestaña Agenda: toda tu libreta dentro de la app, con buscador, tus favoritos arriba y filtros Bloqueados y Permitidos, para ver a cuáles de tus contactos les cortaste el paso. Tócalos para llamar, bloquear, permitir o copiar el número.

Teclado rediseñado: teclas más grandes y resultados que flotan en vez de dejar un hueco en blanco.

Desliza hacia abajo en Agenda o Registro para recargar.

Qué probar: busca un contacto por nombre y por número, y bloquéalo desde la Agenda.
</es-419>
<en-US>
New Contacts tab: your whole address book in the app, with search, your favourites on top, and Blocked and Allowed filters so you can see which of the people you know you have silenced. Tap anyone to call, block, allow or copy their number.

Rebuilt keypad: bigger keys, and results that float instead of leaving a blank gap.

Pull down on Contacts or the Log to refresh.

What to test: search a contact by name and by number, then block one from Contacts.
</en-US>
<pt-BR>
Nova aba Agenda: toda a sua lista de contatos no app, com busca, seus favoritos no topo e filtros Bloqueados e Permitidos, para ver quais dos seus contatos você silenciou. Toque para ligar, bloquear, permitir ou copiar o número.

Teclado refeito: teclas maiores e resultados que flutuam em vez de deixar um espaço em branco.

Puxe para baixo na Agenda ou no Registro para atualizar.

O que testar: busque um contato por nome e por número e bloqueie um pela Agenda.
</pt-BR>
<hi-IN>
नया संपर्क टैब: पूरी संपर्क सूची ऐप में — खोज, ऊपर पसंदीदा, और अवरुद्ध व अनुमत फ़िल्टर, ताकि दिखे कि आपने किन परिचितों को चुप कराया है। कॉल, अवरुद्ध, अनुमति या नंबर कॉपी करने के लिए टैप करें।

नया कीपैड: बड़ी कुंजियाँ, और खाली जगह के बजाय तैरते खोज परिणाम।

ताज़ा करने के लिए संपर्क या रजिस्टर में नीचे खींचें।

क्या जाँचें: संपर्क को नाम और नंबर से खोजें, फिर संपर्क टैब से अवरुद्ध करें।
</hi-IN>

## What is in this build

- **A Contacts (Agenda) tab** — the address book with search, the platform's starred contacts, and
  Blocked/Allowed filters that cross it with this app's rules. Matched with `sameNumber`, so a rule
  saved from a national-format call still matches a card saved internationally.
- **The keypad rebuilt** — results float in a popup instead of reserving a blank band, the number
  field sits on the pad, the keys grow to fill the screen, and the space above holds favourites or
  recent callers (or one line saying so). Recent callers are behind a setting, on by default,
  because they are call history on the screen a phone gets handed to someone on.
- **Pull-to-refresh** on the Contacts tab and the call log, past the address book's five-minute
  cache.
- **Settings left the navigation bar** to make room and is reached from a gear on Home.

## Verified before this bundle

- 721 automated tests (`:shared` 629, `:androidApp` 92) plus 369 on the iOS simulator, all green,
  and `./scripts/verify.sh --release` including R8.
- On a razr 50 ultra with a real 940-contact address book: the tab, the favourites strip read from
  `ContactsContract`, search, the dial pad holding position across keystrokes, and 2.29% janky
  frames while typing.
- On a Pixel 8 Pro API 36 emulator: `device_check.sh`, `rule_matrix_test.sh` 14/14,
  `ring_test.sh auto`, and `call_test.sh` end to end — a blocked call answered by the
  auto-responder with a recording on its log row.

**Not verified:** no call has been placed or received on the razr's live SIM. Everything
call-related is emulator-only, including emergency dialling.

## Before uploading

- [ ] Confirm the filename is `corta-spam-1.6.0-8-release.aab`. Gradle's default name is identical
      for every build ever made; `archivesName` handles it, but check the artifact.
- [ ] Check the artifact's mtime and size before trusting any comparison — this project has seen
      `BUILD SUCCESSFUL in 1s` over an unchanged APK more than once.
- [x] Audit permissions with `aapt2 dump badging` on the **artifact**, not the source manifest: a
      `uses-permission` silently implies its hardware `uses-feature` as required, and that cost six
      device models once already. Done for this build: `versionCode='8' versionName='1.6.0'`,
      targetSdk 36, 7 permissions, `uses-feature-not-required: android.hardware.microphone`.
- [ ] **Check the Console for the next free code before building, not after.** Twice now a build
      has been made against a code the Console had already taken.
- [ ] **Do not re-open the full-screen-intent declaration form.** Its *No* to the pre-grant
      question is what cleared the code-3 rejection — see `SUBMISSION_0.1.0.md` §5b.
- [ ] Screenshots predate the Agenda tab and the new keypad. Retake with
      `scripts/seed_screenshots.sh` before any production promotion.
