# Play Store listing copy — Corta Spam

**Default language: Español (es-419).** English is added as a translation, not the default.
The *app's* own fallback locale is unchanged and remains English — this is a Console setting
about the store page, not about the strings in the APK.

Paste-ready. Assets live in [`store/`](store/).

Two rules applied to both languages: **do not promise detection accuracy** (this app blocks by
*your* rules plus a small bundled list — it is not a crowd-sourced spam database), and **do not
describe the auto-responder as reliable** (it reaches the caller only by acoustic coupling).

---

# Español (es-419) — idioma predeterminado

## Nombre de la app (≤30 caracteres)

```
Corta Spam
```
*10 caracteres.*

## Descripción corta (≤80 caracteres)

```
Bloquea llamadas con tus reglas. Código abierto, sin anuncios ni rastreo.
```
*73 caracteres.*

## Descripción completa (≤4000 caracteres)

```
Corta Spam filtra las llamadas entrantes antes de que suene tu teléfono, con reglas que escribes tú.

Sin anuncios. Sin rastreo. Sin cuentas. Sin acceso a la red: cada regla y cada registro se quedan en una base de datos dentro de tu dispositivo, y la app no tiene código capaz de enviarlos a ningún lado.

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

La app no pide acceso al registro de llamadas del sistema, ni a SMS, almacenamiento, ubicación, cámara o micrófono. Lee tus contactos solo para reconocer a quien ya conoces, y eso nunca sale del dispositivo.

No incluye ningún SDK de analítica ni de informes de fallos. La política de privacidad completa se publica junto al código fuente, y puedes comprobar cada afirmación contra el código.

CÓDIGO ABIERTO

Cada línea es pública y auditable en github.com/cpinan/Corta-Spam, con licencia MIT.

IDIOMAS

Español, inglés, portugués e hindi.

NOTA SOBRE LOS PERMISOS

Corta Spam debe ser tu app de teléfono predeterminada. Es la única forma en que Android permite filtrar una llamada antes de que suene: así es como funciona el bloqueo, no es un permiso extra.

El auto-respondedor es experimental. Contesta una llamada bloqueada y reproduce un saludo, pero en versiones recientes de Android puede que quien llama no lo escuche bien; tómalo como un extra, no como una función en la que confiar.
```
*~2.100 caracteres.*

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
Block calls by your own rules. Open source, no ads, nothing leaves your phone.
```
*77 characters.*

## Full description (≤4000 chars)

```
Corta Spam screens incoming calls before your phone rings, using rules you write yourself.

No ads. No tracking. No accounts. No network access at all — every rule and every log entry stays in a database on your device, and the app has no code that could send it anywhere.

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

The app requests no access to your system call log, SMS, storage, location, camera or microphone. It reads your contacts only to recognise callers you know, and that never leaves the device.

There is no analytics SDK and no crash-reporting SDK. The full privacy policy is published alongside the source code, and you can check every claim in it against the code.

OPEN SOURCE

Every line is public and auditable at github.com/cpinan/Corta-Spam, under the MIT licence.

LANGUAGES

English, Spanish, Portuguese and Hindi.

NOTE ON PERMISSIONS

Corta Spam must be set as your default phone app. That is the only way Android lets an app screen a call before it rings — it is how the blocking works, not an extra request.

The auto-responder is experimental. It answers a blocked call and plays a greeting, but on modern Android the caller may not hear it clearly; treat it as a bonus, not a feature to rely on.
```
*~2,050 characters.*

---

## Category and contact

| Field | Value |
|---|---|
| Default language | **Español (es-419)** — English added as a translation |
| Category | **Tools** — not Communication. It is a utility, and Communication draws stricter dialer scrutiny. |
| Tags | Call blocking, Privacy, Open source |
| Contact email | *(a monitored address — required and shown publicly)* |
| Website | `https://github.com/cpinan/Corta-Spam` |
| Privacy policy | `https://cpinan.github.io/Corta-Spam/PRIVACY` |

## Assets

Graphics are **common assets**: Play falls back to the *default language's* graphics for every
translation that doesn't supply its own. Since the default is es-419, the plain
`feature_1024x500.png` carries Spanish text, and the `_en` variant exists only to attach to an
English translation. Uploading the English one as the default would show English marketing copy
to Spanish users.

| Asset | File | Spec | Status |
|---|---|---|---|
| App icon | `store/ic_play_512.png` | 512×512 PNG | ✅ 512×512 |
| Feature graphic | `store/play/feature_1024x500.png` | 1024×500, no alpha | ✅ **Spanish** |
| Feature graphic (en) | `store/play/feature_1024x500_en.png` | only for the en-US translation | ✅ |
| Screenshot 1 | `store/play/01_home.png` | phone, **exactly 9:16** | ✅ 1683×2992 |
| Screenshot 2 | `store/play/02_lists.png` | | ✅ 1683×2992 |
| Screenshot 3 | `store/play/03_patterns.png` | | ✅ 1683×2992 |
| Screenshot 4 | `store/play/04_calllog.png` | | ✅ 1683×2992 |
| Screenshot 5 | `store/play/05_settings.png` | | ✅ 1683×2992 |

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
