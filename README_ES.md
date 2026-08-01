# Corta Spam

App de bloqueo de llamadas de código abierto. Sin anuncios. Sin rastreo. Tus datos nunca salen del dispositivo.

Filtra llamadas entrantes antes de que suene el teléfono. Comprueba cada número contra tus reglas — bloqueo manual, patrones, bloqueo por país, horas de silencio — más un proveedor comunitario de spam opcional. Bloquea, permite o responde con un saludo personalizado.

**i18n**: Inglés, Español (LATAM), Portugués (Brasil), Hindi.

## Estado

M0–M10 completos. Diseño adaptativo integrado (móvil/tablet). i18n en 4 idiomas. Código abierto bajo licencia MIT. 166+ pruebas automatizadas pasan. APK de Android compila. iOS pospuesto.

- [`docs/SPEC.md`](docs/SPEC.md) — especificación del producto, matriz de capacidades, arquitectura
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — desglose de hitos con criterios de aceptación
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — plan de diseño adaptativo horizontal/tablet
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — declaración para Google Play + política de privacidad
- [`LICENSE`](LICENSE) — Licencia MIT

## Funcionalidades

- **Bloqueo manual** — bloquea o permite números específicos
- **Reglas de patrón** — bloquea por prefijo, sufijo o comodín (`+34900*`, `*1234`)
- **Bloqueo por país** — bloquea todas las llamadas de un código de país
- **Horas de silencio** — silencia todas las llamadas en un horario (TimePicker con ajustes: Noche, Siesta, Trabajo)
- **Auto-respondedor (Experimental)** — responde llamadas bloqueadas con saludo TTS o audio personalizado
- **Registro de llamadas** — historial con hora local, resultado y detalle de la regla (panel dividido en tablet)
- **Devolver llamada** — toca cualquier número en el registro para devolver la llamada
- **Copiar número** — copia números al portapapeles desde el registro
- **Estadísticas** — conteo de llamadas bloqueadas por día/semana/mes
- **Respaldo** — exporta/importa todas las reglas como JSON
- **Diseño adaptativo** — barra inferior en móvil, barra lateral en tablet/apaisado, contenido centrado a 600dp
- **Avisos de duplicados** — advierte al agregar un número que ya está en la otra lista
- **Motor de precedencia** — el bloqueo manual tiene prioridad sobre contactos y lista de permitidos
- **Normalización de contactos** — compara números formateados de contactos con números entrantes sin formato
- **Privacidad y Términos** — política de privacidad y licencia MIT dentro de la app
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
./gradlew :shared:testDebugUnitTest          # 159+ pruebas, commonTest + androidUnitTest (Robolectric)
./gradlew :shared:iosSimulatorArm64Test      # commonTest en Kotlin/Native (pospuesto)
```

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

Un curso completo de 15 módulos en HTML recorre cada capa de la app — Gradle, KMM, Compose, navegación, layouts adaptativos, SQLDelight, Koin DI, permisos, Telecom/InCallService, motor de reglas, i18n, testing, CI, depuración en iOS, y notificaciones de llamadas/UX de permisos.

Abre [`course/corta_spam_course.html`](course/corta_spam_course.html) en cualquier navegador. Incluye modo oscuro, seguimiento de progreso, fragmentos de código del proyecto real, diagramas SVG y 56 preguntas de evaluación.

## Cambios recientes

**2026-08-01:**
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
