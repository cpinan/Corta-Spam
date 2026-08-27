# Play Store listing copy — Corta Spam

**Default language: Español (es-419).** English is added as a translation, not the default.
The *app's* own fallback locale is unchanged and remains English — this is a Console setting
about the store page, not about the strings in the APK.

Paste-ready. Assets live in [`store/`](store/).

Three rules applied to both languages: **do not promise detection accuracy** (this app blocks by
*your* rules plus a small bundled list — it is not a crowd-sourced spam database), **do not
describe the auto-responder as reliable** (it reaches the caller only by acoustic coupling), and
**the short description must describe function and nothing else**.

That last one has its own
[guideline](https://support.google.com/googleplay/android-developer/answer/9866151): the short
description may not carry *"language that is not related to the function or purpose of your app,
including… price and promotional information"*. Play flagged an earlier short description reading
`Open source, no ads, no tracking` — an ad-free claim is monetization information, and licensing
is not a function. Being flagged does not block publishing, it makes the app **ineligible for
Play promotion**. The full description is not bound by this rule, so the privacy and open-source
positioning lives there instead.

---

# Español (es-419) — idioma predeterminado

## Nombre de la app (≤30 caracteres)

```
Corta Spam
```
*10 caracteres.*

## Descripción corta (≤80 caracteres)

```
App de teléfono que filtra cada llamada con tus reglas, antes de que suene.
```
*75 caracteres.* Solo función: ver la regla de la descripción corta más arriba.

## Descripción completa (≤4000 caracteres)

```
Corta Spam es tu app de teléfono. Sustituye al marcador predeterminado, recibe todas tus llamadas entrantes y las filtra con reglas que escribes tú, antes de que el teléfono suene.

Sin anuncios. Sin rastreo. Sin cuentas. Sin acceso a la red: cada regla y cada registro se quedan en una base de datos dentro de tu dispositivo, y la app no tiene código capaz de enviarlos a ningún lado.

CUANDO ENTRA UNA LLAMADA

Corta Spam es la app que suena. Muestra a quien llama a pantalla completa, con Contestar y Rechazar, encima de la pantalla de bloqueo cuando el teléfono está bloqueado, y con el nombre del contacto si lo tienes guardado. Mientras hablas puedes salir de la app y volver a la llamada desde la notificación.

QUÉ PUEDES BLOQUEAR

• Números concretos, con una etiqueta para que el registro te diga por qué
• Patrones: bloquea un rango entero con comodines, como +34900* o *1234
• Países: bloquea un código de país del que nunca esperas una llamada
• Horas de silencio: silencia todo según un horario, salvo tu lista de permitidos
• Llamadas repetidas: bloquea un número tras demasiados intentos en poco tiempo
• Una lista de spam incluida: códigos de marcación asociados a estafas, dentro de la app

Tus contactos y tu lista de permitidos siempre pasan, y un número que bloqueas a mano sigue bloqueado aunque esté en tus contactos. Tú decides qué manda.

QUÉ PASA CON UNA LLAMADA BLOQUEADA

Tu teléfono no suena. El registro guarda qué ocurrió y qué regla tomó la decisión, en tu idioma. Puedes devolver la llamada o copiar cualquier número desde el propio registro.

Tú eliges qué hacer con un número que no coincide con ninguna regla: dejarlo pasar, bloquearlo, o dejarlo pasar marcado para revisarlo después. También hay una opción para dejar pasar a un desconocido que ya ha insistido varias veces, porque una persona real que llama repetidamente casi nunca es una robollamada.

PRIVACIDAD

La app no pide acceso al registro de llamadas del sistema, ni a SMS, almacenamiento, ubicación o cámara. Lee tus contactos solo para reconocer a quien ya conoces, y eso nunca sale del dispositivo. El micrófono solo se pide si activas la grabación del auto-respondedor, que viene desactivada; las grabaciones se guardan en el almacenamiento privado de la app y nunca se transmiten.

No incluye ningún SDK de analítica ni de informes de fallos. La política de privacidad completa se publica junto al código fuente, y puedes comprobar cada afirmación contra el código.

CÓDIGO ABIERTO

Cada línea es pública y auditable en https://github.com/cpinan/Corta-Spam, con licencia MIT.

Ahí está también la política de privacidad completa, el historial de cambios y las instrucciones para compilar la app tú mismo.

IDIOMAS

Español, inglés, portugués e hindi.

NOTA SOBRE LOS PERMISOS

Corta Spam es una app de teléfono, así que debe ser tu app de teléfono predeterminada. Es la única forma en que Android permite recibir una llamada, mostrarla y filtrarla antes de que suene: así es como funciona la app, no es un permiso extra.

Por la misma razón muestra la llamada entrante a pantalla completa sobre la pantalla de bloqueo, como haría cualquier app de teléfono. Sin eso, una llamada que llega con el teléfono bloqueado no se podría contestar.

El auto-respondedor es experimental. Contesta una llamada bloqueada y reproduce un saludo, pero en versiones recientes de Android puede que quien llama no lo escuche bien; tómalo como un extra, no como una función en la que confiar. Si quieres, puede grabar el mensaje que deje quien llama: para activarlo, tu saludo debe incluir una frase que avise de la grabación.
```
*3.609 caracteres.*

## Notas de la versión

Cambian en cada release, así que no viven aquí. Copia paste-ready, en los dos idiomas y con las
etiquetas que pide la Consola: [`store/RELEASE_NOTES_0.1.0.md`](store/RELEASE_NOTES_0.1.0.md).

---

# English (en-US) — added translation

## App name (≤30 chars)

```
Corta Spam
```
*10 characters.*

## Short description (≤80 chars)

```
Phone app that screens every call by your rules, before your phone rings.
```
*73 characters.* Function only — see the short-description rule above.

## Full description (≤4000 chars)

```
Corta Spam is your phone app. It replaces the default dialer, receives every incoming call, and screens it against rules you write yourself, before your phone rings.

No ads. No tracking. No accounts. No network access at all — every rule and every log entry stays in a database on your device, and the app has no code that could send it anywhere.

WHEN A CALL COMES IN

Corta Spam is the app that rings. It shows the caller full-screen with Answer and Decline, over the lock screen when your phone is locked, and names the caller from your contacts. While you are on a call you can leave the app and get back to it from the notification.

WHAT YOU CAN BLOCK

• Specific numbers — with a label so your call log tells you why
• Patterns — block a whole range with wildcards, like +34900* or *1234
• Countries — block a dialling code you never expect a call from
• Quiet hours — silence everything on a schedule, except your allowlist
• Repeat callers — block a number after it tries too many times in a short window
• A bundled spam list — known scam dialling codes, shipped inside the app

Your contacts and your allowlist always come through, and a number you block manually stays blocked even if it is in your contacts. You decide what wins.

WHAT HAPPENS TO A BLOCKED CALL

Your phone stays silent. The call log records what happened and which rule made the decision, in your language. You can call back or copy any number straight from the log.

You choose what happens to a number no rule matched: let it through, block it, or let it through but flag it for review. There is also an optional setting to let an unknown number through once it has tried several times — because a real person calling repeatedly is usually not a robocall.

PRIVACY

The app requests no access to your system call log, SMS, storage, location or camera. It reads your contacts only to recognise callers you know, and that never leaves the device. The microphone is requested only if you switch auto-responder recording on, which ships off; recordings stay in the app's private storage and are never transmitted.

There is no analytics SDK and no crash-reporting SDK. The full privacy policy is published alongside the source code, and you can check every claim in it against the code.

OPEN SOURCE

Every line is public and auditable at https://github.com/cpinan/Corta-Spam, under the MIT licence.

The same repository holds the full privacy policy, the changelog, and the instructions for building the app yourself.

LANGUAGES

English, Spanish, Portuguese and Hindi.

NOTE ON PERMISSIONS

Corta Spam is a phone app, so it must be set as your default phone app. That is the only way Android lets an app receive a call, show it, and screen it before it rings — it is how the app works, not an extra request.

For the same reason it shows the incoming call full-screen over the lock screen, the way any phone app does. Without that, a call arriving while your phone is locked could not be answered.

The auto-responder is experimental. It answers a blocked call and plays a greeting, but on modern Android the caller may not hear it clearly; treat it as a bonus, not a feature to rely on. It can optionally record the message a caller leaves — to turn that on, your own greeting must contain a line telling the caller they are being recorded.
```
*3,323 characters.*

---

# Português (pt-BR) — added translation

The app has shipped Brazilian Portuguese strings since 1.2.0. Until this listing exists, a
Brazilian user sees a Spanish store page and then an app in Portuguese.

## Nome do app (≤30 caracteres)

```
Corta Spam
```
*10 caracteres.* Marca, não traduzida.

## Descrição breve (≤80 caracteres)

```
App de telefone que filtra cada chamada pelas suas regras, antes de tocar.
```
*74 caracteres.* Só função — ver a regra da descrição breve acima: nada de preço, promoção ou
licença aqui.

## Descrição completa (≤4000 caracteres)

```
Corta Spam é o seu app de telefone. Ele substitui o discador padrão, recebe todas as suas chamadas e as filtra com regras que você mesmo escreve, antes de o telefone tocar.

Sem anúncios. Sem rastreamento. Sem contas. Sem acesso à rede: cada regra e cada registro ficam em um banco de dados dentro do seu aparelho, e o app não tem código capaz de enviá-los a lugar nenhum.

QUANDO CHEGA UMA CHAMADA

O Corta Spam é o app que toca. Mostra quem está ligando em tela cheia, com Atender e Recusar, por cima da tela de bloqueio quando o telefone está bloqueado, e com o nome do contato se você o tiver salvo. Durante a chamada você pode sair do app e voltar a ela pela notificação.

O QUE VOCÊ PODE BLOQUEAR

• Números específicos, com uma etiqueta para o registro dizer por quê
• Padrões: bloqueie uma faixa inteira com curingas, como +55119* ou *1234
• Países: bloqueie um código de país do qual você nunca espera uma chamada
• Horário de silêncio: silencie tudo por agenda, exceto a sua lista de permitidos
• Chamadas repetidas: bloqueie um número após tentativas demais em pouco tempo
• Uma lista de spam incluída: códigos de discagem ligados a golpes, dentro do app

Seus contatos e a sua lista de permitidos sempre passam, e um número bloqueado à mão continua bloqueado mesmo se estiver nos contatos. Você decide o que vale mais.

O QUE ACONTECE COM UMA CHAMADA BLOQUEADA

Seu telefone não toca. O registro guarda o que aconteceu e qual regra tomou a decisão, no seu idioma. Você pode retornar a ligação ou copiar qualquer número direto do registro.

Você escolhe o que fazer com um número que nenhuma regra reconheceu: deixar passar, bloquear, ou deixar passar marcado para revisar depois. Há também a opção de deixar passar um desconhecido que já insistiu várias vezes, porque uma pessoa real ligando de novo e de novo quase nunca é uma robochamada.

PRIVACIDADE

O app não pede acesso ao registro de chamadas do sistema, nem a SMS, armazenamento, localização ou câmera. Lê os seus contatos apenas para reconhecer quem você já conhece, e isso nunca sai do aparelho. O microfone só é pedido se você ativar a gravação da resposta automática, que vem desligada; as gravações ficam no armazenamento privado do app e nunca são transmitidas.

Não há nenhum SDK de análise nem de relatório de falhas. A política de privacidade completa é publicada junto com o código-fonte, e você pode conferir cada afirmação contra o código.

CÓDIGO ABERTO

Cada linha é pública e auditável em https://github.com/cpinan/Corta-Spam, sob licença MIT.

O mesmo repositório traz a política de privacidade completa, o histórico de mudanças e as instruções para compilar o app você mesmo.

IDIOMAS

Português, espanhol, inglês e hindi.

OBSERVAÇÃO SOBRE AS PERMISSÕES

O Corta Spam é um app de telefone, então precisa ser o seu app de telefone padrão. É a única forma de o Android permitir que um app receba uma chamada, mostre-a e a filtre antes de tocar: é assim que o app funciona, não é um pedido a mais.

Pelo mesmo motivo ele mostra a chamada em tela cheia sobre a tela de bloqueio, como qualquer app de telefone faria. Sem isso, uma chamada que chega com o telefone bloqueado não poderia ser atendida.

A resposta automática é experimental. Ela atende uma chamada bloqueada e reproduz uma saudação, mas em versões recentes do Android quem liga pode não ouvir bem; encare como um extra, não como um recurso em que confiar. Se quiser, ela pode gravar o recado deixado por quem ligou: para ativar, a sua saudação precisa conter uma frase avisando que a chamada está sendo gravada.
```
*3,559 caracteres.* Contém o parágrafo `CÓDIGO ABERTO` com o link do repositório, que
desaparece por idioma se for esquecido.

---

# हिन्दी (hi-IN) — added translation

The app has shipped Hindi strings since 1.2.0, enforced by `TranslationCompletenessTest`. Same
reason as pt-BR: the strings exist, the store page does not.

## ऐप का नाम (≤30 characters)

```
Corta Spam
```
*10 characters.* Brand name, left untranslated — it is what the icon and the app itself say.

## संक्षिप्त विवरण (≤80 characters)

```
फ़ोन ऐप जो हर कॉल को आपके नियमों से जाँचता है, घंटी बजने से पहले।
```
*65 characters.* Function only, same rule as the other three.

## पूरा विवरण (≤4000 characters)

```
Corta Spam आपका फ़ोन ऐप है। यह डिफ़ॉल्ट डायलर की जगह लेता है, आपकी हर आने वाली कॉल लेता है, और घंटी बजने से पहले उसे आपके अपने लिखे नियमों से जाँचता है।

कोई विज्ञापन नहीं। कोई ट्रैकिंग नहीं। कोई खाता नहीं। कोई नेटवर्क पहुँच नहीं: हर नियम और हर रिकॉर्ड आपके ही डिवाइस के डेटाबेस में रहता है, और ऐप में ऐसा कोई कोड नहीं है जो उन्हें कहीं भेज सके।

जब कॉल आती है

घंटी Corta Spam बजाता है। यह कॉल करने वाले को पूरी स्क्रीन पर दिखाता है, उत्तर दें और अस्वीकारें के साथ, फ़ोन लॉक होने पर लॉक स्क्रीन के ऊपर, और संपर्क सहेजा हो तो उसके नाम के साथ। बात करते हुए आप ऐप से बाहर जा सकते हैं और सूचना से कॉल पर लौट सकते हैं।

आप क्या अवरुद्ध कर सकते हैं

• खास नंबर — एक लेबल के साथ, ताकि रजिस्टर बताए कि क्यों
• पैटर्न — वाइल्डकार्ड से पूरी शृंखला अवरुद्ध करें, जैसे +9190* या *1234
• देश — जिस देश कोड से आप कभी कॉल की उम्मीद नहीं करते, उसे अवरुद्ध करें
• शांत घंटे — समय-सारणी के अनुसार सब कुछ शांत, आपकी अनुमत सूची को छोड़कर
• बार-बार कॉल — थोड़े समय में बहुत बार कोशिश करने पर नंबर अवरुद्ध
• साथ आने वाली स्पैम सूची — ठगी से जुड़े डायलिंग कोड, ऐप के भीतर

आपके संपर्क और आपकी अनुमत सूची हमेशा पास होते हैं, और हाथ से अवरुद्ध किया नंबर संपर्कों में होने पर भी अवरुद्ध ही रहता है। किसकी चलेगी, यह आप तय करते हैं।

अवरुद्ध कॉल का क्या होता है

आपका फ़ोन नहीं बजता। रजिस्टर आपकी भाषा में दर्ज करता है कि क्या हुआ और कौन-से नियम ने फ़ैसला किया। आप वहीं से कॉल लौटा सकते हैं या कोई भी नंबर कॉपी कर सकते हैं।

जिस नंबर से कोई नियम मेल नहीं खाता, उसके लिए आप चुनते हैं: जाने दें, अवरुद्ध करें, या जाने दें पर बाद में देखने के लिए चिह्नित कर दें। एक विकल्प यह भी है कि कई बार कोशिश कर चुके अनजान नंबर को जाने दिया जाए — क्योंकि बार-बार कॉल करने वाला असली इंसान आम तौर पर रोबोकॉल नहीं होता।

निजता

ऐप सिस्टम कॉल लॉग, SMS, स्टोरेज, स्थान या कैमरे तक पहुँच नहीं माँगता। संपर्क सिर्फ़ इसलिए पढ़ता है कि जिन्हें आप जानते हैं उन्हें पहचान सके, और वह डिवाइस से कभी बाहर नहीं जाता। माइक्रोफ़ोन तभी माँगा जाता है जब आप स्वतः उत्तर की रिकॉर्डिंग चालू करें, जो बंद ही आती है; रिकॉर्डिंग ऐप के निजी स्टोरेज में रहती है और कभी भेजी नहीं जाती।

कोई एनालिटिक्स SDK नहीं, कोई क्रैश-रिपोर्टिंग SDK नहीं। पूरी निजता नीति स्रोत कोड के साथ प्रकाशित है, और आप उसका हर दावा कोड से मिलाकर देख सकते हैं।

मुक्त स्रोत

हर पंक्ति https://github.com/cpinan/Corta-Spam पर सार्वजनिक और जाँचने योग्य है, MIT लाइसेंस के तहत।

वहीं पूरी निजता नीति, बदलावों का इतिहास और ऐप को खुद संकलित करने के निर्देश भी हैं।

भाषाएँ

हिंदी, अंग्रेज़ी, स्पेनिश और पुर्तगाली।

अनुमतियों के बारे में

Corta Spam एक फ़ोन ऐप है, इसलिए इसे आपका डिफ़ॉल्ट फ़ोन ऐप होना चाहिए। Android केवल इसी तरह किसी ऐप को कॉल लेने, दिखाने और घंटी बजने से पहले जाँचने देता है: ऐप ऐसे ही काम करता है, यह कोई अतिरिक्त माँग नहीं है।

इसी कारण यह आने वाली कॉल को लॉक स्क्रीन के ऊपर पूरी स्क्रीन पर दिखाता है, जैसा कोई भी फ़ोन ऐप करता है। इसके बिना, फ़ोन लॉक होने पर आई कॉल उठाई ही नहीं जा सकती।

स्वतः उत्तर प्रायोगिक है। यह अवरुद्ध कॉल उठाकर एक संदेश सुनाता है, पर Android के नए संस्करणों में कॉल करने वाले को वह ठीक से सुनाई न दे — इसे अतिरिक्त सुविधा मानें, भरोसे का साधन नहीं। चाहें तो यह कॉल करने वाले का संदेश रिकॉर्ड कर सकता है: उसके लिए आपके अपने संदेश में यह बताने वाली पंक्ति होनी चाहिए कि कॉल रिकॉर्ड की जा रही है।
```
*3,139 characters.* Carries the मुक्त स्रोत (open source) paragraph with the repository
link.

---

## Adding a translation in the Console

Four fields and one decision, per language. **Play refuses a release-notes tag for a language the
listing does not have**, which is why the pt-BR and hi-IN blocks in `store/RELEASE_NOTES_*.md`
have been unusable for five releases.

1. **Grow the app store listing → Store listing → Main store listing**, language selector at the
   top → *Add translations* → *Add your own translation text* → pick `Português (Brasil) – pt-BR`
   and `हिन्दी – hi-IN`.
2. Paste the three text fields above for each: app name, short description, full description.
3. **Graphics: leave them empty and they inherit the default language's**, which is es-419 — so a
   Hindi reader would get Spanish screenshots and a Spanish feature graphic. Uploading a per
   language set is optional but is the only way that page reads as translated. See below.
4. Save, then re-check: Play validates length on save and silently keeps the *previous* text if a
   field is rejected.

Once both exist, the four-tag release-notes block in `store/RELEASE_NOTES_1.6.1.md` can be pasted
whole instead of the two-tag subset.

### Per-language graphics, if you want them

The app itself is fully translated, so screenshots in each language cost one emulator run each:

```bash
./scripts/seed_screenshots.sh --device <serial> --locale pt-BR   # then capture
./scripts/seed_screenshots.sh --device <serial> --locale hi-IN
./scripts/play_assets.sh                                          # pad 1344x2992 -> 1683x2992
```

`seed_screenshots.sh` needs the **debug** build, because it reads and rewrites the database
through `run-as`. The feature graphic carries Spanish text in the default file and English in
`_en`; a pt-BR or hi-IN feature graphic does not exist yet and would have to be drawn. Without
one, that language falls back to the Spanish graphic rather than showing nothing.

## Category and contact

| Field | Value |
|---|---|
| Default language | **Español (es-419)** — English, Portuguese (pt-BR) and Hindi (hi-IN) added as translations |
| Category | **Communication** — matches what the APK is (an `InCallService` holding `ROLE_DIALER`). An earlier draft of this doc said Tools, on the theory that Communication draws stricter dialer scrutiny; that is backwards. `USE_FULL_SCREEN_INTENT` is auto-granted only to apps whose core function is *receiving phone or video calls*, and a Tools app is not one. Do not change this back. |
| Tags | Caller ID, Communication |
| Contact email | *(a monitored address — required and shown publicly)* |
| Website | `https://github.com/cpinan/Corta-Spam` |
| Privacy policy | `https://cpinan.github.io/corta-spam/privacy.html` |
| Support / marketing site | `https://cpinan.github.io/corta-spam/` |

## Repository link — where it actually appears

The repo is linked from the listing in two independent places, and they are not interchangeable.

| Console location | Value | Why it matters |
|---|---|---|
| **Store settings → Store listing contact details → Website** | `https://github.com/cpinan/Corta-Spam` | The **only** always-visible link. It renders as a tappable *Website* entry in the app's "App support" section, with no expansion needed |
| **Main store listing → Full description**, `CÓDIGO ABIERTO` / `OPEN SOURCE` paragraph | `https://github.com/cpinan/Corta-Spam` as plain text | Play does **not** hyperlink URLs inside a description — it is text a reader copies or types. Kept anyway because it explains *what* the link is, which the Website field cannot |

Two consequences worth stating, because both are easy to get wrong:

- **The full description is truncated.** Play shows roughly the first few lines and hides the rest
  behind *Read more*, so the `OPEN SOURCE` paragraph is below the fold for most readers. A URL in
  the description is not a substitute for the Website field — set the field.
- **The full description is per-language.** Every translation needs the paragraph, or the repo
  link silently disappears for that locale. It is present in all four: es-419 (default), en-US,
  pt-BR (`CÓDIGO ABERTO`) and hi-IN (`मुक्त स्रोत`).

### Donation links stay out of the listing

The repository carries the donation options ([`DONATE.md`](../DONATE.md)); the Play listing and
the app itself carry none, and that is deliberate rather than an omission:

- The **short description may not contain promotional or price information** — the same
  [guideline](https://support.google.com/googleplay/android-developer/answer/9866151) that already
  cost this listing its `no ads, no tracking` line. A donation ask is squarely inside it.
- The app ships **no billing code and no donation prompt**, so Play's
  [Payments policy](https://support.google.com/googleplay/android-developer/answer/10281818) has
  nothing to apply to. The ask lives one deliberate hop away, on a repository page the user has to
  choose to visit.

Anyone who wants to donate reaches `DONATE.md` through the Website field. That is the whole
funnel, and it is enough.

## Assets

Graphics are **common assets**: Play falls back to the *default language's* graphics for every
translation that doesn't supply its own. Since the default is es-419, the plain
`feature_1024x500.png` carries Spanish text, and the `_en` variant exists only to attach to an
English translation. Uploading the English one as the default would show English marketing copy
to Spanish users.

**Screenshots are per-language too, and the `es_` prefix is the only thing marking them.** The
default listing is es-419, so the **`es_*` set is the default** and the unprefixed `01`-`05` set
belongs to the en-US translation. Attaching `01_home.png` to the default listing puts English
screenshots in front of every Spanish user — the same fallback rule as the feature graphic.

| Asset | File | Attach to | Status |
|---|---|---|---|
| App icon | `store/ic_play_512.png` | both (common) | ✅ 512×512 |
| Feature graphic | `store/play/feature_1024x500.png` | **default (es-419)** | ✅ **Spanish**, no alpha |
| Feature graphic | `store/play/feature_1024x500_en.png` | en-US translation | ✅ English |
| Screenshots ×4 | `store/play/es_01_home.png`, `es_02_calllog.png`, `es_03_lists.png`, `es_04_settings.png` | **default (es-419)** | ✅ 1683×2992, 9:16 |
| Screenshots ×5 | `store/play/01_home.png`, `02_lists.png`, `03_patterns.png`, `04_calllog.png`, `05_settings.png` | en-US translation | ✅ 1683×2992, 9:16 |

**Upload the files in `store/play/`, not `store/`.** The raw captures are 1344×2992 — a 9:20
ratio, more extreme than the 1:2 floor Play accepts, and it rejects them. `store/play/` holds the
same images padded to exactly 1683×2992 (9:16) by replicating the outermost pixel column
outward, so the status bar and navigation bar extend into the padding instead of sitting inside a
visible box. `store/` keeps the unpadded originals for reference.

Captured on the `Pixel_8_Pro_API_33` AVD with seeded demo data. Every phone number shown is from
a range that cannot be a real subscriber (`+34 900` is Spanish premium-rate/service) — these
images are public once uploaded.

## Release notes

Per-release, so they do not live here. Paste-ready in both languages, tagged the way the Console
asks for them: [`store/RELEASE_NOTES_0.1.0.md`](store/RELEASE_NOTES_0.1.0.md).
