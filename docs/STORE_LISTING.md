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

Cada línea es pública y auditable en github.com/cpinan/Corta-Spam, con licencia MIT.

IDIOMAS

Español, inglés, portugués e hindi.

NOTA SOBRE LOS PERMISOS

Corta Spam es una app de teléfono, así que debe ser tu app de teléfono predeterminada. Es la única forma en que Android permite recibir una llamada, mostrarla y filtrarla antes de que suene: así es como funciona la app, no es un permiso extra.

Por la misma razón muestra la llamada entrante a pantalla completa sobre la pantalla de bloqueo, como haría cualquier app de teléfono. Sin eso, una llamada que llega con el teléfono bloqueado no se podría contestar.

El auto-respondedor es experimental. Contesta una llamada bloqueada y reproduce un saludo, pero en versiones recientes de Android puede que quien llama no lo escuche bien; tómalo como un extra, no como una función en la que confiar. Si quieres, puede grabar el mensaje que deje quien llama: para activarlo, tu saludo debe incluir una frase que avise de la grabación.
```
*3.472 caracteres.*

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

Every line is public and auditable at github.com/cpinan/Corta-Spam, under the MIT licence.

LANGUAGES

English, Spanish, Portuguese and Hindi.

NOTE ON PERMISSIONS

Corta Spam is a phone app, so it must be set as your default phone app. That is the only way Android lets an app receive a call, show it, and screen it before it rings — it is how the app works, not an extra request.

For the same reason it shows the incoming call full-screen over the lock screen, the way any phone app does. Without that, a call arriving while your phone is locked could not be answered.

The auto-responder is experimental. It answers a blocked call and plays a greeting, but on modern Android the caller may not hear it clearly; treat it as a bonus, not a feature to rely on. It can optionally record the message a caller leaves — to turn that on, your own greeting must contain a line telling the caller they are being recorded.
```
*3,196 characters.*

---

## Category and contact

| Field | Value |
|---|---|
| Default language | **Español (es-419)** — English added as a translation |
| Category | **Communication** — matches what the APK is (an `InCallService` holding `ROLE_DIALER`). An earlier draft of this doc said Tools, on the theory that Communication draws stricter dialer scrutiny; that is backwards. `USE_FULL_SCREEN_INTENT` is auto-granted only to apps whose core function is *receiving phone or video calls*, and a Tools app is not one. Do not change this back. |
| Tags | Caller ID, Communication |
| Contact email | *(a monitored address — required and shown publicly)* |
| Website | `https://github.com/cpinan/Corta-Spam` |
| Privacy policy | `https://cpinan.github.io/corta-spam/privacy.html` |
| Support / marketing site | `https://cpinan.github.io/corta-spam/` |

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
