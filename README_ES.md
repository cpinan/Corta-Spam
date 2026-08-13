# Corta Spam

App de bloqueo de llamadas de código abierto. Sin anuncios. Sin rastreo. Tus datos nunca salen del dispositivo.

Filtra llamadas entrantes antes de que suene el teléfono. Comprueba cada número contra tus reglas — bloqueo manual, patrones, bloqueo por país, horas de silencio — más una lista de spam opcional incluida dentro de la app (sin red, nunca). Bloquea, permite o responde con un saludo personalizado.

**i18n**: Inglés, Español (LATAM), Portugués (Brasil), Hindi.

## Estado

M0–M13 completos, incluido el diseño adaptativo de M12 (ya están los dos paneles list-detail de tablet). i18n en 4 idiomas. Código abierto bajo licencia MIT. 373 pruebas automatizadas pasan. APK de Android compila. La app de iOS compila y arranca, pero el bloqueo de llamadas allí sigue pendiente de la extensión CallDirectory.

- [`docs/SPEC.md`](docs/SPEC.md) — especificación del producto, matriz de capacidades, arquitectura
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — desglose de hitos con criterios de aceptación
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — plan de diseño adaptativo horizontal/tablet
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — declaración para Google Play + política de privacidad
- [`LICENSE`](LICENSE) — Licencia MIT

## Funcionalidades

- **Bloqueo manual** — bloquea o permite números específicos, con una etiqueta opcional visible en la lista y en las filas del registro que coincidan
- **Reglas de patrón** — bloquea por prefijo, sufijo o comodín (`+34900*`, `*1234`)
- **Bloqueo por país** — bloquea todas las llamadas de un código de país, comparado solo con números escritos en formato internacional (`+34…` o `0034…`), de modo que bloquear Marruecos nunca bloquea un número `212` de Manhattan
- **Reglas de llamadas repetidas** — bloquea un número tras N intentos dentro de una ventana de tiempo, opcionalmente limitado a un patrón
- **Horas de silencio** — silencia todas las llamadas en un horario (TimePicker con ajustes: Noche, Siesta, Trabajo)
- **Auto-respondedor (Experimental)** — responde llamadas bloqueadas con saludo TTS o audio personalizado; botón "Probar saludo" para escucharlo localmente sin necesidad de una llamada real. El saludo por defecto y la frase de consentimiento de grabación están traducidos, y la reproducción fuerza el altavoz para que quien llama pueda oírlo
- **Grabación del mensaje del llamante (Experimental, desactivada por defecto)** — graba lo que dice quien llama después del saludo, con un límite de 60 s, y se puede reproducir y borrar desde su entrada del registro de llamadas. Requiere tanto una frase de consentimiento en tu propio saludo como el permiso de micrófono de Android. Graba por el micrófono, porque Android reserva el audio real de la línea para apps privilegiadas — así que en teléfonos cuyo fabricante bloquea el micrófono durante una llamada no captará nada
- **Respuesta a llamadas repetidas** — opcional: un número desconocido que normalmente se bloquearía en silencio se deja pasar tras suficientes intentos, con un aviso en la pantalla de llamada entrante y una notificación. Nunca aplica a números bloqueados manualmente ni por patrón, país, spam u horario
- **Registro de llamadas** — historial con hora local, resultado, detalle de la regla y el nombre del contacto cuando coincide (panel dividido en tablet)
- **Devolver llamada** — toca cualquier número en el registro para devolver la llamada
- **Copiar número** — copia números al portapapeles desde el registro
- **Estadísticas** — conteo de llamadas bloqueadas por día/semana/mes, agrupado por medianoche *local* y con soporte de horario de verano
- **Respaldo** — exporta/importa todas las reglas como JSON, conservando las etiquetas; un diálogo en la app ("Ver formato de ejemplo") muestra la estructura JSON
- **Diseño adaptativo** — barra inferior en móvil, barra lateral en tablet/apaisado, contenido centrado a 600dp
- **Avisos de duplicados** — advierte al agregar un número que ya está en la otra lista
- **Motor de precedencia** — el bloqueo manual tiene prioridad sobre contactos y lista de permitidos
- **Normalización de contactos** — compara números formateados de contactos con números entrantes sin formato
- **Lista de permisos al primer arranque** — tras la explicación del rol de teléfono, una pantalla enumera cada permiso que pide la app y lo único para lo que se usa, con su diálogo del sistema detrás de un Permitir explícito. Nada en ella es obligatorio, y el micrófono se nombra pero solo se pide cuando se activa la grabación de llamadas
- **Avisos de permisos en Inicio** — si la app pierde el rol de teléfono, las notificaciones, el intent a pantalla completa o el permiso de llamada, una tarjeta lo dice encima de los contadores de llamadas bloqueadas, con un botón que arregla exactamente eso. Los mismos avisos aparecen en Ajustes
- **Privacidad y Términos** — política de privacidad y licencia MIT dentro de la app
- **Tono de llamada** — la app reproduce el tono y la vibración por sí misma (como exige el contrato de marcador predeterminado), respetando el modo de sonido del sistema, y lo silencia en cuanto llega una decisión de bloqueo
- **Control de notificaciones** — un solo interruptor "Mostrar notificaciones" silencia todo lo que publique la app, incluida la alerta de llamada entrante
- **i18n** — inglés (predeterminado), español (es), portugués (pt), hindi (hi)

## Estructura del proyecto

```
shared/       Módulo Kotlin Multiplatform — lógica común, UI Compose, SQLDelight
androidApp/   Aplicación Android (MainActivity, InCallService, InCallActivity)
iosApp/       Aplicación iOS — project.yml (xcodegen) + Swift
docs/         Especificación, hitos, plan adaptativo, cumplimiento, guiones QA
design/       Iconografía — maestros SVG, renderizador Sharp, activos de marca
```

## Requisitos previos

- JDK 17
- Android SDK (`ANDROID_HOME` configurado), platform 36 + build-tools
- Xcode 16+ y [xcodegen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) — solo iOS
- Node.js (para regenerar iconos)

## Compilar y ejecutar — Android

```sh
./gradlew :androidApp:assembleDebug
# con dispositivo/emulador conectado:
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n org.carlospinan.bloqueador.app/.MainActivity

# o usa el script auxiliar:
./install_android.sh
```

## Compilar y ejecutar — iOS

```sh
cd iosApp && xcodegen generate && cd ..
open iosApp/iosApp.xcodeproj
# o sin interfaz:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -configuration Debug build
```

## Pruebas

```sh
./gradlew :shared:testDebugUnitTest          # 378 pruebas, commonTest + androidUnitTest (Robolectric)
./gradlew :androidApp:testDebugUnitTest      # 8 pruebas, clases exclusivas de Android (Robolectric)
./gradlew :shared:iosSimulatorArm64Test      # commonTest en Kotlin/Native (pospuesto)
./scripts/verify.sh                          # todo lo anterior + ktlint, lint, migraciones, compilación iOS
```

### En un dispositivo

Parte del comportamiento de esta app no la puede alcanzar ninguna prueba unitaria. Una llamada solo
existe mientras Telecom la sostiene, y el motor de reglas puede ser perfectamente correcto sin que
le llegue nada — que es como seis funcionalidades llegaron a publicarse, con sus pruebas en verde,
sin ejecutarse nunca. Estos scripts comprueban lo que una prueba de JVM no puede ver. Todos aceptan
`--device <serial>`.

```sh
./scripts/rule_matrix_test.sh    # 13 escenarios de bloqueo/permiso, cada uno con una llamada real (emulador)
./scripts/ring_test.sh auto      # el teléfono suena de verdad, y calla cuando debe callar
./scripts/ring_test.sh watch     # las mismas comprobaciones en hardware real, mientras alguien llama
./scripts/call_test.sh preflight # ¿está este dispositivo listo para probar la grabación en vivo?
./scripts/device_check.sh        # instalación, sin fatales, versión de esquema e índices en el teléfono
```

`rule_matrix_test.sh` necesita un emulador (él mismo lanza las llamadas con `adb emu gsm call`), una
build de debug (siembra la base de datos con `run-as`) y el rol de marcador. **Reemplaza** las
reglas, los ajustes y el registro de llamadas del dispositivo. Lo único que un emulador no puede
resolver es si suena en hardware real de cada fabricante — para eso está `ring_test.sh watch`, y
alguien tiene que llamar al teléfono.

## Regeneración de iconos

Si cambian los SVG fuente, regenera los 21 iconos PNG:

```sh
npm install --no-save sharp
node design/iconography/render_ui_icons.mjs
# Salida esperada: "Rendered and validated 21 interface icons."
```

## Localización

Los recursos de texto están en `shared/src/commonMain/composeResources/`:

| Directorio | Idioma |
|---|---|
| `values/` | Inglés (predeterminado) |
| `values-es/` | Español (LATAM) |
| `values-pt/` | Portugués (Brasil) |
| `values-hi/` | Hindi |

Agrega nuevos idiomas creando un archivo `values-<código>/strings.xml` con la misma estructura de claves.

## Aprendizaje

Un curso completo de 22 módulos en HTML recorre cada capa de la app — Gradle, KMM, Compose, navegación, layouts adaptativos, SQLDelight, Koin DI, permisos, Telecom/InCallService, motor de reglas, i18n, testing, CI, depuración en iOS, notificaciones de llamadas/UX de permisos, cómo extender el motor de precedencia con seguridad, gestión de estado (MVVM + MVI bien hecho, incluyendo cuándo *no* forzar el patrón), dobles de prueba a escala (cómo consolidar fakes duplicados entre los source sets de prueba de KMP), cómo probar la capa de persistencia contra un motor SQLite real, cómo auditar código que se publica pero nunca se ejecuta, políticas de plataforma y las promesas que hace tu app, y (el más nuevo) la UX de permisos como problema de diseño: pedirlos, explicarlos y darse cuenta cuando te los quitan.

Abre [`course/corta_spam_course.html`](course/corta_spam_course.html) en cualquier navegador. Incluye modo oscuro, seguimiento de progreso, fragmentos de código del proyecto real, diagramas SVG y 88 preguntas de evaluación.

## Cambios recientes

**2026-08-13:** versión de código 4, generada por un arreglo que no está en el paquete.

- **Un rechazo gasta un código de versión aunque el remedio sea una respuesta en un formulario.** Lo que resolvió el rechazo del código 3 fue una respuesta en la declaración de intención a pantalla completa de Play Console — *No* a la pregunta sobre la concesión previa — y ni una línea de la app cambió por ello. Aun así, Play no vuelve a revisar un código sobre el que ya dictaminó, así que enviar esa respuesta cuesta una recompilación igualmente. `appVersionCode` ahora es **4**, y lleva la fila del checklist de bienvenida que hizo necesaria el rechazo de la concesión previa. Los códigos 1, 2 y 3 están gastados; el 2 ni siquiera llegó a revisión. Cuenta con eso al planificar una publicación.
- **Se auditó el artefacto, no el manifiesto fuente.** `aapt2 dump badging` sobre el APK compilado confirma `versionCode='4'`, `android.hardware.microphone` como `uses-feature-not-required` (la línea que impide que `RECORD_AUDIO` lo declare *obligatorio*, lo que dejó fuera a 6 dispositivos en el código 2), sin `READ_PHONE_STATE`, y como único `uses-implied-feature` el `faketouch` que recibe cualquier app. El paquete está firmado y verificado (`jar verified`) con `CN=Carlos Pinan`. Los tres paquetes anteriores están ya en `rejected/`, dejando exactamente un archivo en la carpeta de publicación: cuatro nombres que se diferencian en un dígito son la forma en que se publica el build equivocado.
- **El arreglo que vive en un formulario es el que hay que volver a comprobar antes de enviar.** Ninguna auditoría del artefacto puede verlo. `docs/store/SUBMISSION_0.1.0.md` convierte ahora la reconfirmación de esa respuesta *No* en un paso numerado del orden de producción, porque lo único que podría deshacer esta publicación en silencio no deja rastro en la compilación.

**2026-08-12:** el mismo rechazo de política dos veces, y el permiso que nadie habría encontrado.

- **Tener razón sobre la política no sirvió para publicar la app.** El código de versión 3 fue rechazado bajo la política de intención a pantalla completa con exactamente el mismo motivo que tumbó al código 1 — *«el uso del permiso no está directamente relacionado con el propósito principal de la app»* — solo que esta vez el formulario de declaración sí se había enviado antes de subir el paquete, y la ficha ya abría con «app de teléfono». La teoría de que el revisor nunca vio el formulario queda descartada. Lo que resolvió el caso fue la *segunda* pregunta del formulario, que nadie había leído como decisiva: **¿quieres que este permiso se conceda automáticamente al instalar?** El rechazo es un veredicto sobre esa petición, así que responder **No** elimina aquello que se estaba juzgando. El permiso sigue en el manifiesto, `setFullScreenIntent` sigue en `IncomingCallNotifier`, y no se borró código para satisfacer una política. Ver [`docs/PLAY_FSI_APPEAL.md`](docs/PLAY_FSI_APPEAL.md).
- **Renunciar a la concesión automática volvió obligatoria una ruta dentro de la app, y en la práctica no había ninguna.** Ahora cada instalación en Android 14+ empieza con la pantalla de llamada entrante desactivada, así que el permiso lo tiene que conceder la persona usuaria. La única ruta era una tarjeta de aviso en *tercer* lugar, detrás del rol de marcador y de las notificaciones — y la pantalla de inicio muestra solo la primera tarjeta, así que en una instalación nueva el único permiso que ahora falta siempre quedaba escondido tras un enlace de «más cosas por arreglar» hacia Ajustes. Ahora la intención a pantalla completa es una fila de la lista de permisos del onboarding, justo debajo de notificaciones porque entre las dos forman una sola función. Su botón dice **Abrir ajustes**, no **Permitir**: no existe diálogo de permiso para un app-op, y un botón que promete uno que nunca aparece es un control muerto. `onResume` recoge la concesión al volver.
- **El aviso al que sustituye era inalcanzable en el único emulador que tenía el proyecto.** `canUseFullScreenIntent()` existe desde API 34; por debajo, `MainActivity` da el permiso por concedido y toda esa ruta de aviso es código muerto. El único AVD del proyecto era API 33, así que *todos* los estados de este permiso se habían publicado sin probar. Ya existe un AVD de API 36 para ello.

**2026-08-11:** contactos que la lista de permitidos no veía, un permiso sin ningún uso, y un permiso que exigía hardware sin decirlo.

- **Los contactos guardados tal y como se marcan no entraban en la lista de permitidos.** Todas las comparaciones del motor de reglas pasaban por `normalizeForComparison`, que es `filter { it.isDigit() }`. Un contacto guardado como `611 99 88 77` queda en `611998877`; esa misma persona llamando llega desde Telecom como `+34611998877`, que queda en `34611998877`. No son iguales — y como `RulePrecedenceResolver` fusiona los contactos con la lista de permitidos usando esa comparación, **un contacto real no quedaba permitido** y podían bloquearlo las horas de silencio, una regla de país o la acción por defecto, en silencio. La mayoría de la gente guarda los contactos en formato nacional, así que este era el caso común. Se reportó como un registro de llamadas que mostraba números en vez de nombres, que era solo la mitad visible: la notificación de llamada entrante llevaba todo el tiempo mostrando el nombre correcto, porque ese camino usa el `ContactsContract.PhoneLookup` de la plataforma. Ahora la comparación contrasta un número internacional con la forma nacional derivada de *su propio* código de país, así que no hay que adivinar ninguna región — adivinarla exigiría leer la SIM o el locale, una tabla ISO-a-código-de-país que esta app no tiene, y una regla de prefijo troncal distinta por país, porque quitar el cero inicial es correcto en Reino Unido e incorrecto en Italia. Dos números que declaran país siguen decidiéndose por su código, así que `+34611998877` y `+51611998877` siguen siendo personas distintas. Cuatro pruebas del resolver se vieron fallar contra la comparación antigua antes de aplicar el arreglo.

- **Declarar `RECORD_AUDIO` estaba excluyendo dispositivos sin micrófono.** Play avisó de que la
  versión 2 "ya no admite 6 dispositivos que sí admitía la versión anterior", y la causa no estaba
  en el manifiesto tal y como se escribió: `aapt2 dump badging` mostraba
  `uses-implied-feature: name='android.hardware.microphone' reason='requested
  android.permission.RECORD_AUDIO permission'`. Un permiso implica su función de hardware como
  **obligatoria** salvo que se declare de forma explícita, así que añadir la grabación opcional
  redujo en silencio el catálogo de dispositivos. La grabación del auto-respondedor viene
  desactivada, y `AutoResponderRecorder.start()` ya devuelve false si el micrófono falta o está
  ocupado, capturando `IOException`, `IllegalStateException` y el `RuntimeException` pelado que
  `MediaRecorder.start()` lanza en algunos dispositivos — la app funciona igual sin él. El
  manifiesto ahora declara `android.hardware.microphone` con `required="false"`, y el artefacto lo
  confirma: `uses-feature-not-required`. El versionCode pasa a 3, porque el 2 ya se había subido y
  Play nunca acepta dos veces el mismo código.

- **`READ_PHONE_STATE` estaba declarado en el manifiesto y no lo leía ni una línea de código.** Su única justificación era el comentario que tenía encima, que afirmaba que hacía falta para ser elegible como `RoleManager.ROLE_DIALER` — y eso no es lo que dice el `roles.xml` de AOSP. La elegibilidad viene de las dos actividades `ACTION_DIAL` que aparecen en `<required-components>`; el conjunto de permisos de teléfono está en `<permissions>`, que es lo que el rol *concede*, no lo que exige para concederse. Es decir, la app pedía un permiso al usuario para que le dieran un permiso que nunca leía. Eliminado, y el comentario sustituido por lo que sí es cierto. La fila **Teléfono** de la lista de comprobación no cambia: pese al nombre, `AppPermission.PHONE` comprueba `CALL_PHONE`, que se usa para devolver una llamada desde el registro. **Se deja a propósito:** `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, que añade `androidx.core` al fusionar manifiestos. Es privado de la app y no concede nada, y aunque esta app llama a `registerReceiver` cero veces, las librerías de androidx que incluye sí lo hacen — quitar un permiso que una librería necesita se manifiesta como un `SecurityException` en tiempo de ejecución en el teléfono de alguien, no como un fallo de compilación, así que no es algo que se elimine por lo que diga un grep.
- **La ficha de la tienda afirmaba que la app no pide acceso al micrófono.** Era cierto en la versión 1 y dejó de serlo al día siguiente, cuando llegó la grabación del auto-respondedor: `RECORD_AUDIO` está en la versión 2. Ahora la descripción completa, en los dos idiomas, dice que el micrófono solo se pide si activas la grabación, que la función viene desactivada, y que las grabaciones se guardan en el almacenamiento privado de la app y nunca se transmiten — que es justo lo que ya decía la política de privacidad publicada. Una afirmación sobre permisos en la ficha se comprueba contra el paquete con un solo comando, y por eso tiene que ser verdad.

**2026-08-08:** lo primero que ve una persona nueva.

- **Inicio le decía a la persona que estaba protegida mientras le decía que el filtrado estaba apagado.** Detectado al instalar la build de release en un razr 50 ultra y rechazar todos los permisos — algo que ninguna prueba miraba, porque cada mitad era correcta por separado. El banner decía *"El filtro de llamadas está inactivo hasta que Corta Spam sea tu app de teléfono predeterminada"*, y el texto del interruptor justo debajo decía *"Bloqueo activado — se filtran las llamadas de spam y bloqueadas"*. Sin el rol de teléfono no llega ninguna llamada a la app, así que la segunda frase era falsa, y es justo la que se lee para decidir si uno está protegido. Ahora hay un tercer texto para "activado, pero inerte", en el color de error. La misma instalación mostró cuatro tarjetas de aviso apiladas que empujaban el interruptor y todos los contadores fuera de pantalla, así que Inicio muestra solo la más grave — el rol de teléfono manda sobre un permiso que degrada una sola función — seguida de un enlace a Ajustes, que las sigue listando todas.
- **Los avisos estaban en una pantalla que nadie abre.** `PermissionWarnings` vivía como componente privado dentro de `SettingsScreen`, así que una app que había dejado de funcionar en silencio se veía idéntica a una que funcionaba, salvo que la persona fuera a buscar. Ahora es compartido, se muestra también en Inicio y está colocado a propósito *encima* de los contadores: quien ha dejado de bloquear necesita ver por qué antes de leer un "0 bloqueadas hoy" que parece una buena noticia. También ganó el aviso que más importa, el del rol de teléfono ausente, cuyo botón relanza la petición de rol del sistema en vez de dejar a la persona tirada en los ajustes. Contactos y micrófono siguen ausentes a propósito: ambos son opcionales y se explican donde vive la función que los necesita, y un banner por un permiso que no has pedido usar es una molestia, no un aviso.
- **Ceder el rol de teléfono dejaba a la app afirmando que aún lo tenía.** `DialerOnboardingViewModel.refresh()` se ejecuta en cada reanudación para detectar un cambio de app de teléfono hecho desde los ajustes del sistema — pero solo sabía subir, a `ALREADY_DEFAULT`. Al revocar el rol, el estado se quedaba en `GRANTED` para siempre: una app con aspecto de funcionar, con el interruptor de bloqueo encendido, que ya no podía filtrar ni una llamada, y sin nada en ninguna parte que lo dijera. Ahora se mueve en ambas direcciones, y deja `REQUESTING` intacto a propósito, porque el diálogo de rol del sistema está en pantalla y es su resultado — no un sondeo al reanudar — quien decide lo que pasa después. Tres de las cinco pruebas nuevas se vieron fallar contra el código antiguo antes de aplicar el arreglo.
- **El primer acto de la app era un diálogo de permiso que nadie había explicado.** `MainActivity.onCreate` lanzaba la petición de `POST_NOTIFICATIONS` sin previo aviso, encima de una pantalla de bienvenida que no mencionaba permisos — indistinguible de una app que agarra lo que puede, y la forma más rápida de ganarse un "Denegar" permanente. Ahora el onboarding tiene un paso de lista de comprobación entre la explicación del rol de teléfono y la app: una fila por permiso, nombrando lo único para lo que se usa, con su diálogo del sistema detrás de un **Permitir** explícito. Nada es obligatorio; el botón de continuar siempre está activo y solo cambia de texto. El micrófono aparece en la lista pero nunca se pide ahí — se pide en el momento en que se activa la grabación, porque una app de teléfono pidiendo el micrófono durante el onboarding, para una función que viene apagada, se lee como abuso. Por debajo de API 33 la fila de notificaciones se omite por completo en lugar de mostrarse como un botón que no abre nada.

**2026-08-06:** consecuencias de una política de Play, y un interruptor de grabación que no grababa.

- **«Grabar mensaje del llamante» era un interruptor conectado a nada.** El campo se guardaba, se validaba con una comprobación de consentimiento, se dibujaba como Switch y tenía un test de repositorio en verde — y ningún código grababa audio. No había permiso `RECORD_AUDIO`, ni `MediaRecorder`, ni `AudioRecord`. Peor aún: la política de privacidad publicada incluía una sección entera de «Grabación de llamadas» describiendo la función en los dos idiomas, y la pantalla de bienvenida prometía no grabar «a menos que lo actives por separado». Cuatro superficies visibles afirmando algo que no existía. Ahora está implementado de verdad: `AutoResponderRecorder` graba el mensaje de quien llama después del saludo, en una llamada bloqueada que la app contestó automáticamente, en almacenamiento privado y con un tope de 60 segundos. **Graba el micrófono, no la llamada.** Android reserva `VOICE_CALL`/`VOICE_DOWNLINK`/`VOICE_UPLINK` tras `CAPTURE_AUDIO_OUTPUT`, un permiso `signature|privileged` que ninguna app de terceros puede tener — y el rol de marcador predeterminado tampoco lo concede — así que es captura acústica por el auricular, y varios fabricantes reservan el micrófono durante la llamada, en cuyo caso no se captura nada y la llamada termina igual que antes.
- **Una grabación no tenía dónde vivir ni cómo salir.** Nueva columna `CallLogEntry.recording_path` (migración `2.sqm`) que ata el audio a la llamada de la que salió, de modo que reproducir y borrar aparecen en esa fila y no en una lista de archivos desconectada; y `clearAll` borra ahora los archivos *antes* de eliminar las filas — «borrar registro» podía dejar la voz de un desconocido en el disco sin nada que la señalara. `logCall` devuelve el id de la fila insertada dentro de una transacción junto a `last_insert_rowid()`: al ser este valor propio de la conexión, una segunda llamada entrando a la vez (la llamada en espera lo hace real) habría archivado la grabación de un llamante bajo la entrada de otro.
- **El aviso del micrófono está en la pantalla del auto-respondedor, no en Ajustes.** Avisar de un permiso para una función que viene desactivada sería la misma molestia permanente que fue `showGrantContacts`, así que solo aparece cuando la grabación está realmente activada.
- **Play rechazó `USE_FULL_SCREEN_INTENT` por «no estar directamente relacionado con el propósito principal de la app».** El permiso se queda: la política lo concede automáticamente a las apps cuya función principal es recibir llamadas, y esta declara `IN_CALL_SERVICE_UI` *y* `IN_CALL_SERVICE_RINGING`, lo que significa que Telecom deja de hacer sonar el teléfono y le pasa el trabajo a esta app. Lo que faltaba era el formulario de declaración de Play Console, que ningún documento del repositorio mencionaba. El texto de la ficha, que empezaba hablando de bloqueo y nombraba el rol de marcador una sola vez en una nota final, ahora abre con «app de teléfono» en los dos idiomas. **Este diagnóstico resultó ser incorrecto** — ver 2026-08-12 más arriba: el formulario se envió antes de la siguiente subida y el mismo motivo la rechazó otra vez. Ver `docs/PLAY_FSI_APPEAL.md`.
- **`STORE_COMPLIANCE.md` describía una llamada de red que no existe.** Afirmaba que el proveedor de spam opcional enviaba números a una base de datos pública. No hay cliente HTTP en el grafo de dependencias ni permiso `INTERNET`; la única implementación es una lista dentro del dispositivo. La política de privacidad ya se había corregido por el mismo error; el documento de cumplimiento no, y alimenta un formulario de Seguridad de los Datos con valor legal.

**2026-08-05 (auditoría):** revisión de todo el código y corrección de lo encontrado.

- **El teléfono sonaba en silencio.** El manifiesto declara `IN_CALL_SERVICE_RINGING`, que le dice a Telecom que esta app se encarga del tono — así que Telecom no lo hacía. La app nunca reproducía tono ni vibración, de modo que como marcador predeterminado recibía todas las llamadas en silencio. El nuevo `CallRinger` reproduce el tono y la vibración del usuario, respeta el modo de sonido del sistema y se detiene en cuanto llega una decisión de bloqueo — que es justo lo que mantiene silenciosas las llamadas bloqueadas, y por eso se conservó la declaración en vez de devolverle el tono al sistema.
- **La restauración de copias desactivaba la regla equivocada.** Al importar cualquier regla desactivada se escribía una fila por defecto (activada) y luego se buscaba "la que acabo de insertar" como el *último* elemento de una consulta `created_at DESC, id DESC` — es decir, la más antigua. Restaurar una copia con un patrón desactivado apagaba en silencio otro patrón que el usuario sí quería, en cada restauración. Ahora las filas llevan su `enabled` y `created_at` reales en un solo insert, toda la importación va en una transacción, los conteos vienen de las filas afectadas y no de un contador de bucle (reimportar tu propia copia afirmaba haberlo añadido todo dos veces), `created_at` sobrevive al viaje de ida y vuelta, el ámbito `patternId` de una regla de acción se vuelve a enlazar al nuevo id del patrón en vez de quedar colgando sobre otra fila, y se rechazan las entradas que la interfaz nunca podría producir — como `attempts = 0`, que coincide con todas las llamadas.
- **La lista de spam incluida no podía coincidir con nada.** El resolutor normalizaba el número a solo dígitos antes de la consulta, quitando el `+`, mientras que cada entrada de la lista está en formato `+E.164`. Los 32 prefijos eran inertes en la app publicada, y las pruebas no lo detectaban porque probaban el proveedor directamente y no a través de `evaluate`. Ahora los proveedores reciben la forma canónica con `+`. La lista de patrones por forma, inalcanzable por otra razón, se eliminó en lugar de activarse: una heurística de "contiene ceros seguidos" que nunca se ha medido con tráfico real no debe estar en una ruta que rechaza llamadas en silencio.
- **Bloquear Marruecos bloqueaba Manhattan.** Misma causa: sin el `+`, un número nacional es indistinguible de uno internacional, así que `2125551234` se interpretaba como Marruecos (+212) y `912345678` como India (+91). `PhoneNumberParser` ahora solo informa un país para números escritos realmente en formato internacional (`+` o el código de acceso `00`), y normaliza los caracteres de formato por el camino.
- **Las reglas de llamadas repetidas no tenían interfaz.** La tabla, el repositorio, la rama del resolutor, el seguimiento en `CallAttempt` y los campos de copia existían desde M2; la única forma de crear una era editando a mano un JSON de copia. Ahora hay una pantalla de Llamadas repetidas con ámbito opcional por patrón. Las reglas con ámbito también eran inalcanzables en el motor — el ámbito se resolvía contra los patrones *activados*, y un patrón activado ya bloquea esos números dos pasos antes — así que ahora se resuelve contra todos los patrones, que es lo que hace útil un patrón desactivado usado solo como ámbito.
- **Un patrón `*` bloqueaba el teléfono entero.** La comparación usa dígitos, así que un patrón sin ninguno tiene núcleo vacío, y un núcleo vacío satisface `contains`/`startsWith`/`endsWith` para cualquier número. El diálogo lo aceptaba. Ahora se rechaza en el comparador, en el ViewModel y en la importación. La prueba que afirmaba lo contrario bajo el nombre `patternMatch_caseInsensitive` estaba observando este fallo — la comparación de patrones no distingue mayúsculas.
- **Cuatro países no se podían añadir.** `CountryRule` es `UNIQUE(country_code)` con `INSERT OR IGNORE`, y `COUNTRIES` listaba los códigos `1`, `7`, `212` y `590` dos veces. La segunda entrada aparecía en el selector, no hacía nada al pulsarla y nunca llegaba a la lista. Fusionadas en una entrada por código, con una prueba que lo fija.
- **Las estadísticas se reiniciaban a medianoche UTC.** "Bloqueadas hoy" cambiaba a las 19:00 para alguien en Nueva York, y la fila "Hoy" del gráfico contenía llamadas de dos fechas locales distintas. Los límites se calculan ahora en horario local con kotlinx-datetime, y los grupos por día son medianoches locales consecutivas en lugar de pasos fijos de 86 400 000 ms — lo que además corrige que los días de 23 y 25 horas del cambio de hora colocaran llamadas en el grupo equivocado dos veces al año. El gráfico de siete días cargaba todas las columnas de todas las filas que hubiera tenido el registro; ahora lee solo las marcas de tiempo bloqueadas hasta el grupo más antiguo, sobre un índice nuevo.
- **Texto en inglés en una app de cuatro idiomas.** `RuleDecision` construía su motivo como una frase en inglés *y* lo escribía en `CallLogEntry.rule_detail`, así que todos leían inglés y cada fila histórica quedaba congelada así. Ahora los motivos son datos estructurados que se traducen en el momento de mostrarlos, con un códec que degrada una fila desconocida a su texto original en vez de dejarla en blanco. Igual para las etiquetas de días de estadísticas y los mensajes de copia de seguridad. La comprobación de consentimiento de grabación solo aceptaba una frase en inglés, así que quien escribiera su saludo en español, hindi o portugués nunca podía activar la grabación; ahora se aceptan las cuatro. El saludo por defecto viene de los recursos de la plataforma en lugar de una constante en inglés.
- **Toda la E/S de base de datos corría en `Dispatchers.Default`.** `DriverFactory.databaseDispatcher` estaba declarado, implementado en ambas plataformas y no lo leía nadie — 43 llamadas bloqueantes a SQLite sobre el pool dimensionado por CPU que Compose también usa. Ya está conectado, con pruebas que fallan si alguna llamada vuelve atrás.
- **Una segunda llamada corrompía la primera.** `onCallAdded` cancelaba todo el scope del servicio y sobrescribía estado en campos únicos, así que una llamada que llegaba durante otra mataba en silencio la evaluación en curso de la primera: ni bloqueada ni registrada. Ahora el estado es por llamada, sobre un scope con la vida del servicio.
- Además: el auto-respondedor nunca forzaba el altavoz, así que quien llamaba oía silencio; cada llamada entrante inicializaba un motor de texto a voz estuviera o no activado; se escaneaba el proveedor de contactos completo en cada timbre; el repositorio de ajustes hacía su primera lectura síncrona de disco en el hilo principal durante el timbre; y los ids de notificación del historial podían chocar con los de la llamada en curso.
- **La política de privacidad describía algo que la app no hace** — afirmaba que la comprobación de spam opcional envía números a una base de datos pública. El proveedor incluido es totalmente local y no hace ninguna llamada de red. Corregida y traducida a los cuatro idiomas, junto con las otras dos cadenas visibles (`about_open_source`, `terms_conditions_body`) que solo existían en inglés. Se eliminaron ocho claves muertas y una huérfana que solo estaba en español. Ahora `TranslationCompletenessTest` cubre los tres árboles de recursos en ambos sentidos, porque este proyecto tiene dos sistemas de cadenas independientes y Android Lint solo ve uno.
- **Android Lint nunca había podido ejecutarse.** El frontend UAST de AGP 8.7.3 solo lee metadatos de Kotlin 2.0 y fallaba en toda tarea de lint contra nuestro 2.2.20 — por eso no estaba en CI. AGP 8.13.2 / Gradle 8.14.3 lo resuelve y además retira dos parches que aquella versión obligaba (`android.suppressUnsupportedCompileSdk` y la bajada de `androidx.activity`). `kotlin-stdlib` queda fijado a la versión del propio toolchain; SQLDelight arrastraba el grafo a 2.3.10, es decir, compilar contra una stdlib más nueva que el compilador. Lint encontró tres comprobaciones de permiso ausentes, una rama muerta anterior a la API 26, una lectura de `StateFlow.value` dentro de la composición, formas plurales incorrectas en hindi y portugués, y una capa monocroma del icono adaptativo que estaba dibujada pero nunca referenciada — todo corregido, y lint ya corre en CI.
- **`androidApp` no tenía tipo de compilación release**, así que `release` era el de AGP por defecto: sin minificar, sin reducir y firmado con la clave de depuración. Añadido, con reglas de ProGuard para los rincones reflexivos (serializadores cuyo `@SerialName` acaba en la base de datos, puntos de entrada de Telecom nombrados desde el manifiesto), y `assembleRelease` corre en CI para que R8 se ejercite antes de que un lanzamiento dependa de ello. `android:dataExtractionRules` ahora dice explícitamente lo que `allowBackup="false"` ya significaba: el registro de llamadas no sale del dispositivo, ni por copia en la nube ni por transferencia entre dispositivos.
- De 273 a 339 pruebas (JVM); commonTest ejecuta además 227 de ellas en el simulador de iOS.

**2026-08-05:**
- **M12 queda terminado.** Ajustes estrena el diseño list-detail para tablet que estaba pendiente desde el 2026-07-28: en Expanded (>=840dp) una lista de secciones — Bloqueo, Contactos, Notificaciones, Acerca de — se coloca junto a un panel de detalle, igual que el registro de llamadas. Los diseños de móvil y Medium no cambian, ni siquiera el orden de la lista de ajustes. Los avisos de permisos se muestran a propósito en todas las secciones en lugar de archivarse en la que les corresponde: un aviso que solo se encuentra por casualidad no es un aviso. Verificado en un AVD a 448dp (barra inferior, lista plana) y a 997dp (barra lateral, paneles divididos).
- Se cerraron las carencias de pruebas restantes: `SqlRuleRepository` (unos 40 miembros sin probar), `BundledSpamProvider` y `ContactNameLookup`. `androidApp` no tenía ningún source set de pruebas — ahora sí, con Robolectric y conectado a CI. De 239 a 273 pruebas.
- Se corrigió que las listas de reglas se devolvieran de más antigua a más reciente. `created_at` guarda segundos enteros, así que las reglas añadidas en el mismo segundo empataban y SQLite recurría al orden de inserción — lo contrario de "más recientes primero" que documenta el repositorio. Las cinco consultas afectadas ahora desempatan por `id`.
- Dos hallazgos se dejaron sin corregir a propósito, documentados en pruebas: el único *patrón* de spam de `BundledSpamProvider` (`+*000*`) no puede coincidir nunca, porque el comparador de comodines solo entiende asteriscos al principio y al final, de modo que su rama de confianza 0.65 es inalcanzable — hacer que funcionen los comodines intermedios empezaría a bloquear cualquier número que contenga "000", y eso es una decisión de producto. Y `InCallState` no se puede probar del todo sin una librería de mocks que el proyecto evita deliberadamente; cubrirlo bien exige una interfaz entre los callbacks de Telecom y el objeto.

**2026-08-04:**
- Se arregló el job de iOS en CI, en rojo en cada ejecución durante semanas, con dos fallos encadenados:
  - `error: Unknown iOS simulator arch: 'x86_64'`. `shared` declara `iosArm64` y `iosSimulatorArm64` pero no `iosX64`, mientras que CI compila con `-destination 'generic/platform=iOS Simulator'` — un destino genérico de simulador resuelve `ARCHS` a `arm64 x86_64`, así que se le pedía al framework de Kotlin una arquitectura que nunca se configuró. Ahora `iosApp/project.yml` fija `EXCLUDED_ARCHS[sdk=iphonesimulator*]: x86_64`, de modo que el proyecto de Xcode y los targets de Kotlin describen las mismas arquitecturas. La alternativa era añadir un target `iosX64`, descartada porque solo sirve para simuladores en Macs Intel y duplica el trabajo de compilación de Kotlin/Native en cada build de iOS.
  - Detrás de ese, `Undefined symbols: _OBJC_CLASS_$_UIViewLayoutRegion`. Compose Multiplatform 1.11.1 referencia esa clase de UIKit desde `CMPLayoutRegion.o`, y solo existe en el SDK de iOS 26 — el runner `macos-15` trae Xcode 16.4 / iOS 18.5. El job ahora corre en `macos-26` (Xcode 26.6), igual que el entorno local; el deployment target sigue en iOS 16.0, solo cambió el SDK de compilación.
  - Conviene recordarlo: un `xcodebuild` local con Xcode 26 pasa aunque CI esté roto, porque el SDK local sí tiene el símbolo. El job ahora imprime sus versiones de Xcode y del SDK de iOS antes de compilar, así esa discrepancia se ve de un vistazo en vez de aparecer como un error de enlazado inexplicable.
- Se probaron los tres ViewModels que faltaban — `SettingsViewModel`, `BackupViewModel`, `AutoResponderViewModel` — llevando la cobertura de ViewModels a 8 de 8 y el conjunto a 239 pruebas. Cada uno protege algo concreto: el sexto flujo de `SettingsViewModel` va en un segundo `combine()` encadenado sobre el primero (las sobrecargas tipadas llegan solo a cinco), así que ahora hay una aserción que detecta si un ajuste futuro se pierde; `AutoResponderViewModel` tiene una prueba por código de validación, incluido `MISSING_CONSENT`, porque grabar una llamada sin una frase de consentimiento en el saludo es un problema legal, no estético; `BackupViewModel` solo emite efectos de una sola vez por un canal rendezvous, por lo que se cubren los caminos de fallo de exportación e importación.
- Las 7 pruebas de pantalla con Robolectric pasaron al `createComposeRule` v2, eliminando los avisos de obsolescencia de Compose UI test 1.11.2. La regla v2 usa `StandardTestDispatcher` en lugar de `UnconfinedTestDispatcher`, así que las corrutinas se encolan en vez de ejecutarse de inmediato — ninguna prueba dependía de eso, de modo que bastó con cambiar el import, sin añadir sincronización.
- Se corrigió que la pantalla de Estadísticas archivara las llamadas bloqueadas de hoy bajo "Ayer". `blockedByDay()` construía sus intervalos avanzando desde `now - daysBack` en saltos de 24 horas, así que el más reciente abarcaba `[now-1d, now)` — contenía todas las llamadas de las últimas 24 horas, incluida una hecha hace segundos — mientras que su etiqueta se derivaba del *inicio* del intervalo, un día antes. Ningún intervalo se etiquetaba nunca como "Hoy". Ahora los intervalos se alinean con la medianoche UTC, el mismo límite que ya usaba `countBlockedCallsToday`, de modo que la primera barra del gráfico y el conteo de "bloqueadas hoy" se calculan con el mismo límite de día y ya no pueden discrepar.
- La capa de persistencia recibió sus primeras pruebas reales — 36, contra un motor SQLite en memoria en lugar de un fake. Antes ningún repositorio `Sql*` tenía pruebas propias: `SqlSettingsRepository`, `SqlCallLogRepository`, `SqlAutoResponderRepository`, `SqlSpamProviderRepository` y el `KeyValueSettingsStore` que todos comparten. Cubren que cada ajuste sobreviva a un reinicio (un segundo repositorio sobre la misma base de datos, ya que estos se hidratan una sola vez en `init` y no vuelven a leer), que cada variante de `RuleDecision` pase por la restricción CHECK de `CallLogEntry.rule_type`, y las ventanas de estadísticas basadas en `strftime`. El error de etiquetado anterior es lo que detectaron. Queda documentado un segundo hallazgo, deliberadamente sin corregir: borrar el guion del auto-respondedor guarda una cadena vacía, pero `readString` interpreta un valor en blanco como "sin definir", así que al reiniciar reaparece el guion por defecto — `readString` lo comparten tres repositorios, de modo que cambiar su semántica es una decisión más amplia.
- Se consolidaron los tres fakes duplicados más grandes del conjunto de pruebas. `FakeRuleRepository` (2 copias), `FakeSettingsRepository` (5) y `FakeCallLogRepository` (3) ahora existen una sola vez, en `shared/src/commonTest/.../app/testing/`, compartidos tanto por `commonTest` como por `androidUnitTest` — el 15% del código de pruebas eran cuerpos de fakes copiados a mano, y los 64 miembros de `RuleRepository` obligaban a aplicar cada cambio de interfaz a cada copia manualmente. Un efecto secundario que conviene nombrar: `SettingsRepositoryTest.kt` resultó no afirmar nada sobre `SqlSettingsRepository` — sus cinco pruebas corrían contra el fake declarado en el mismo archivo. Ahora es `FakeSettingsRepositoryTest.kt` y fija el comportamiento de escritura directa de los setters del fake compartido, del que sí dependen las pruebas de los ViewModels; el `SqlSettingsRepository` real quedó sin pruebas en ese momento, una carencia preexistente que el nombre del archivo ocultaba — cerrada ese mismo día por el trabajo de pruebas de persistencia descrito arriba. Se dejaron duplicados a propósito: los fakes de 5 a 10 líneas de `ContactsGateway`/`DefaultDialerGateway`, donde una declaración local se lee mejor que un import.
- **Cambio incompatible (pre-lanzamiento):** el archivo de base de datos pasó de `bloquellamadas.db` a `cortaspam.db`, y `rootProject.name` ahora es `CortaSpam`. Un nombre de archivo nuevo significa que cada dispositivo abre una base de datos vacía — las listas de bloqueo, reglas e historial de llamadas existentes en dispositivos de desarrollo se pierden. Se hizo a propósito mientras no hay lanzamiento público.
- Se arregló una red de seguridad de migraciones que nunca había llegado a ejecutarse. `./gradlew :shared:verifySqlDelightMigration` fallaba con `duplicate column name: pattern_id`, y ningún job de CI dependía de esa tarea, así que nadie lo notó. La causa raíz tenía tres capas: nunca se configuró `schemaOutputDirectory`, así que los snapshots del esquema se quedaron estancados en `5.db` mientras las migraciones llegaban a `7.sqm`; y, una vez reconstruida una línea base correcta, la verificación detectó un error real — `5.sqm` añadía `pattern_id` con `ALTER TABLE ADD COLUMN`, sentencia con la que SQLite no puede adjuntar la clave foránea `REFERENCES PatternRule(id)` que declara `AppDatabase.sq`, de modo que las bases de datos actualizadas y las instalaciones nuevas tenían esquemas distintos.
- Como el cambio de nombre de la base de datos deja las siete migraciones históricas sin usuarios, se compactaron en una única línea base en `shared/src/commonMain/sqldelight/databases/1.db`. `verifySqlDelightMigration` ahora corre en CI, y se comprobó que pasa con un esquema coherente y falla con un diff preciso de columnas cuando no lo es.
- Se fijó el paquete de recursos de Compose Multiplatform con `compose.resources { packageOfResClass = ... }`. Antes se derivaba de `rootProject.name`, así que renombrar el proyecto rompía todos los imports `Res.string.*` en 12 archivos de pantalla.
- Se movió `bloquea_llamadas_mockups.html` de la raíz del repositorio a `design/mockups.html`, y se corrigió el orden de reglas que documentaba. Afirmaba "primero la lista de permitidos, luego el bloqueo manual" en dos lugares; el `RulePrecedenceResolver` real comprueba el bloqueo manual en el paso 1 y la lista de permitidos en el paso 2, a propósito, para que un bloqueo manual tenga prioridad sobre una coincidencia de contacto.
- Se eliminaron archivos muertos detectados en una revisión del código: `Greeting.kt`/`GreetingTest.kt` (andamiaje de M0 cuyo único llamador era su propia prueba — conteo de pruebas 176 → 175), los cuatro scripts `.claude/hooks/*.sh` (plantillas sin modificar que asumen una estructura de módulos `feature/`/`domain/`/`core/` que este repositorio no tiene, nunca referenciadas desde `settings.json`, y más débiles que la skill `corta-spam-verify-build` que las reemplazó), y el archivo de notas de sesión `.session_state.md` que estaba versionado.

**2026-08-02:**
- Una revisión de arquitectura encontró que la app era consistentemente MVVM (un único `StateFlow<UiState>` por ViewModel, con scope en Koin) pero no MVI — ningún ViewModel tenía un único punto de entrada tipado para las acciones del usuario, solo N métodos públicos sueltos. Todo ViewModel con al menos una acción despachable ahora expone un tipo `Intent` sellado + una función `onIntent()`; los antiguos métodos públicos ahora son detalles de implementación privados. Los ViewModels de solo lectura sin nada externo que despachar (`StatsViewModel`) deliberadamente no recibieron uno — una clase sellada de un solo caso sin quien la llame es ceremonia, no MVI.
- Se corrigieron 3 ViewModels que se habían desviado de la propia regla del proyecto de "un UiState por ViewModel": `BlockListViewModel` (11 `StateFlow` separados → un `BlockListUiState` con los conteos como propiedades derivadas), `CallLogViewModel` y `DialerOnboardingViewModel` (dos flujos desconectados cada uno → un solo UiState). `CallLogViewModel` también absorbió la lógica de filtrado por rango de fechas del registro de llamadas que antes vivía en el archivo de navegación, con pruebas de regresión nuevas que nunca había tenido.
- Se corrigió que `DialerOnboardingScreen` recibiera el ViewModel directamente como parámetro de un Composable — el único lugar de la app que lo hacía. Ahora recibe estado + un callback de despacho de intenciones, como cualquier otra pantalla.
- El flujo de exportación de `BackupViewModel` devolvía el JSON exportado mediante un callback provisto por la UI, algo que no encaja bien en una Intent de datos puros. Se reemplazó con un nuevo caso `BackupEffect.Exported(json)` junto a los efectos de éxito/error ya existentes.
- Se movió el texto de la Política de Privacidad y los Términos y Condiciones de literales de texto en Kotlin a recursos de cadenas (por ahora solo en inglés).

**2026-08-01:**
- **Nuevo**: respuesta a llamadas repetidas. Un número desconocido (sin regla coincidente, que cae en `defaultAction = Block`) que reintenta al menos N veces en 24h se deja pasar en vez de bloquearse en silencio para siempre — desactivado por defecto, umbral de intentos configurable (2-10) en Ajustes. Deliberadamente limitado a la ruta de "sin regla coincidente" en `RulePrecedenceResolver`: un bloqueo manual o una coincidencia de patrón, país, spam u horario siempre gana sin importar cuántas veces reintente, porque insistir es un rasgo típico de robollamadas y saltarse esos bloqueos anularía el bloqueo real de spam. Muestra un aviso "te llamó N veces" en la pantalla de llamada entrante y una notificación cuando se activa; reutiliza la infraestructura de conteo de intentos ya construida para la regla de acción "bloquear tras N intentos" (su contraparte inversa). Requirió una migración de esquema para la nueva etiqueta `REPEATED_ALLOWED` en `CallLogEntry.rule_type`.
- Se agregó un diálogo "Ver formato de ejemplo" en la pantalla de Respaldo con un fragmento JSON de muestra (con una entrada etiquetada) — el campo de etiqueta ya se conservaba de punta a punta en exportar/importar, solo faltaba documentarlo en la app.
- Se agregó un botón "Probar saludo" en la pantalla de Auto-respondedor que reproduce el guion/audio actual localmente por el altavoz del teléfono, para probarlo sin disparar una llamada real.
- Se agregó un ajuste "Mostrar notificaciones" que silencia toda notificación que publique la app, incluida la alerta de llamada entrante — desactivado significa que las llamadas quedan totalmente silenciosas salvo que la app ya esté en primer plano. También se centró el logo de la app en el espacio vacío de la pantalla de llamada en todas sus fases (timbrando/marcando/activa).
- Se corrigió un cierre inesperado: tocar "Llamar de vuelta" en una entrada del registro con número privado/restringido (número vacío, un caso legítimo para llamadas ocultas) generaba un intent `tel:` sin nada que resolver y lanzaba una `ActivityNotFoundException` sin capturar. La acción ahora está deshabilitada para números vacíos, y el código que realiza la llamada es defensivo ante cualquier otro caso de intent no resoluble.
- Limpieza de arquitectura, a partir de una revisión completa contra las convenciones de clean architecture/MVVM:
  - Los 7 ViewModels ahora están limitados a su ruta de navegación en vez de vivir durante toda la sesión de la app.
  - El mensaje de resultado de la pantalla de respaldo era estado persistente que nadie limpiaba, así que reaparecía obsoleto al volver a visitarla — se reemplazó por un efecto de un solo uso más un Snackbar.
  - `MainActivity` ya no inyecta repositorios ni ViewModels directamente; el resultado del selector de audio y la bandera `welcomeShown` de primer uso ahora fluyen a través de los ViewModels que realmente los poseen.
  - Ajustes/Auto-respondedor/Inicio ahora exponen cada uno un único `UiState` en vez de varios flujos de estado independientes.
  - Se extrajo la lógica de evaluación de reglas de llamadas entrantes fuera del servicio Telecom de Android hacia un caso de uso compartido y probado — antes era la pieza de lógica sin pruebas más grande de la app.
- **Nuevo**: las etiquetas opcionales en números bloqueados/permitidos ahora también aparecen en las filas del registro de llamadas para coincidencias de la lista de permitidos (los bloqueos manuales ya mostraban la suya).
- **Nuevo**: las filas del registro de llamadas, lista de bloqueo y lista de permitidos muestran el nombre del contacto coincidente en vez del número sin formato (solo Android — el acceso a contactos en iOS sigue siendo un stub).
- Se corrigió la verificación de reconstrucción de `install_android.sh`: comparaba la fecha del APK contra `./gradlew` (que nunca cambia) en vez de contra el código fuente real, así que seguía instalando una build obsoleta en silencio después de la primera ejecución.

**2026-07-31:**
- Se agregó el flujo completo de notificaciones de llamadas entrantes — antes no existía ninguno, así que las llamadas entrantes solo aparecían mediante un `startActivity()` desde un servicio en segundo plano, algo que Android descarta silenciosamente cuando la pantalla está apagada o bloqueada. Ahora: una alerta a pantalla completa con acciones de Contestar/Rechazar, una notificación persistente de "volver a la llamada" mientras está activa, y notificaciones posteriores de llamadas perdidas o bloqueadas que muestran el nombre del contacto (vía `ContactsContract.PhoneLookup`) y el motivo del bloqueo. Tres canales de notificación; todos los textos localizados (en/es/hi/pt) mediante recursos nativos de Android.
- Ajustes ahora muestra advertencias reales sobre el estado de los permisos — notificaciones denegadas, alerta a pantalla completa revocada (Android 14+), permiso de teléfono denegado — cada una con un enlace directo a la pantalla de ajustes del sistema correspondiente. Antes estos casos fallaban en silencio, sin ninguna señal para el usuario.
- La acción "Preguntar" (`DefaultAction.ASK`) ahora tiene un comportamiento real en vez de ser un alias silencioso de Permitir: las llamadas sin regla coincidente se dejan pasar y se marcan como "Necesita revisión" en el registro de llamadas. Inicio muestra una tarjeta con el conteo de "Pendiente de revisión"; el Registro de llamadas tiene un filtro y un estado visual propios.
- "Llamar de vuelta" en el registro de llamadas ahora realiza la llamada directamente (`ACTION_CALL` + permiso `CALL_PHONE` en tiempo de ejecución) en vez de solo abrir el marcador con el número prellenado.
- El interruptor de bloqueo en Inicio ahora tiene un subtítulo que explica qué hace.
- La advertencia experimental del contestador automático ahora explica *por qué* puede no funcionar — restricciones de enrutamiento de audio en versiones nuevas de Android/fabricantes (el mismo endurecimiento anti-espionaje que afecta a las apps de grabación de llamadas) — en vez de un genérico "puede no funcionar en todos los dispositivos".
- Corregido: las estadísticas de Inicio se quedaban desactualizadas tras poner la app en segundo plano (una actualización al reanudar se había perdido), el aviso de permiso de contactos insistía para siempre incluso después de concederlo (verificaba si existía un callback, no el permiso real), un import muerto que rompía `ktlintCheck`, y un método muerto sin usar en `PassthroughInCallService`.

**iOS (2026-07-30):**
- Se corrigió la inicialización de Koin — `initKoin()` ahora se llama en `MainViewController.kt` antes de que arranque la interfaz de Compose (antes solo se inicializaba en Android vía `BloqueaLlamadasApp.onCreate`)
- Se agregó `CADisableMinimumFrameDurationOnPhone: true` al Info.plist vía `project.yml` (requerido por Compose Multiplatform en iPhones de alta tasa de refresco)
- Se reemplazó `Dispatchers.IO` por `Dispatchers.Default` en todo el módulo compartido (API interna en Kotlin/Native)
- Se reemplazó `Clock.System` por un `expect/actual currentTimeMillis()` de plataforma para compatibilidad con iOS
- Se agregó `databaseDispatcher` a la interfaz `DriverFactory` (Android: `IO`, iOS: `Default`)
- Se corrigió `arguments?.getString()` de Navigation Compose → cast a `Map` para compatibilidad con KMP
- Se eliminó un `import kotlinx.coroutines.IO` residual (interno en Native)
- **Conocido**: el renderizado por GPU Metal puede colgarse en ciertos simuladores de iOS (p. ej. iPhone 16 Pro en macOS 26). Usa iPhone SE o un dispositivo físico. Pendiente una alternativa de renderizado por software.

**Android (2026-07-30):**
- Se agregó la acción de devolver llamada en el registro (intent `ACTION_DIAL`)
- Se agregaron marcas de tiempo locales a las entradas del registro vía `currentTimeMillis()` expect/actual
- Se corrigió la recarga de pantalla por doble toque en la barra inferior con comparación consciente de la sección
- InCallActivity ahora aparece instantáneamente al recibir una llamada (gana la carrera contra la UI del sistema)
- Se agregó descarte de KeyguardManager para la toma de pantalla completa en llamadas entrantes
- Se ocultó el interruptor del proveedor de spam en ajustes (backend preservado)
- El contestador automático se marcó como Experimental
- La pantalla de estadísticas se de-hardcodeó (Cargando, conteo de bloqueadas)
- "Copiar número" ahora está conectado al portapapeles (`ClipboardManager`)
- Normalización de números telefónicos para comparar contactos entre formatos (`normalizeForComparison`)

## Licencia

MIT — consulta [`LICENSE`](LICENSE). Corta Spam es software libre y de código abierto.
