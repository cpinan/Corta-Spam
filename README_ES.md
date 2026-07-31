# Corta Spam

App de bloqueo de llamadas de código abierto. Sin anuncios. Sin rastreo. Tus datos nunca salen del dispositivo.

Filtra llamadas entrantes antes de que suene el teléfono. Comprueba cada número contra tus reglas — bloqueo manual, patrones, bloqueo por país, horas de silencio — más un proveedor comunitario de spam opcional. Bloquea, permite o responde con un saludo personalizado.

**i18n**: Inglés, Español (LATAM), Portugués (Brasil), Hindi.

## Estado

M0–M10 completos. Diseño adaptativo integrado (móvil/tablet). i18n en 4 idiomas. Código abierto bajo licencia MIT. 159+ pruebas automatizadas pasan. APK de Android compila. iOS pospuesto.

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

Un curso completo de 13 módulos en HTML recorre cada capa de la app — Gradle, KMM, Compose, navegación, layouts adaptativos, SQLDelight, Koin DI, permisos, Telecom/InCallService, motor de reglas, i18n, testing y CI.

Abre [`course/corta_spam_course.html`](course/corta_spam_course.html) en cualquier navegador. Incluye modo oscuro, seguimiento de progreso, fragmentos de código del proyecto real, diagramas SVG y 45 preguntas de evaluación.

## Licencia

MIT — consulta [`LICENSE`](LICENSE). Corta Spam es software libre y de código abierto.
