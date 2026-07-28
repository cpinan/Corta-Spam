# Corta Spam

App de bloqueo de llamadas de código abierto. Sin anuncios. Sin rastreo. Tus datos nunca salen del dispositivo.

Filtra llamadas entrantes antes de que suene el teléfono. Comprueba cada número contra tus reglas — bloqueo manual, patrones, bloqueo por país, horas de silencio — más un proveedor comunitario de spam opcional. Bloquea, permite o responde con un saludo personalizado.

## Estado

M0–M10 completos. Diseño adaptativo integrado (móvil/tablet). 151 pruebas automatizadas pasan. APK de Android compila. iOS pospuesto.

- [`docs/SPEC.md`](docs/SPEC.md) — especificación del producto, matriz de capacidades, arquitectura
- [`docs/MILESTONES.md`](docs/MILESTONES.md) — desglose de hitos con criterios de aceptación
- [`docs/ADAPTIVE_PLAN.md`](docs/ADAPTIVE_PLAN.md) — plan de diseño adaptativo horizontal/tablet
- [`docs/STORE_COMPLIANCE.md`](docs/STORE_COMPLIANCE.md) — declaración para Google Play + política de privacidad

## Funcionalidades

- **Bloqueo manual** — bloquea o permite números específicos
- **Reglas de patrón** — bloquea por prefijo, sufijo o comodín (`+34900*`, `*1234`)
- **Bloqueo por país** — bloquea todas las llamadas de un código de país
- **Horas de silencio** — silencia todas las llamadas en un horario (TimePicker con ajustes rápidos)
- **Auto-respondedor** — responde llamadas bloqueadas con saludo TTS o audio personalizado
- **Proveedor de spam** — base de datos comunitaria opcional (desactivada por defecto)
- **Registro de llamadas** — historial con resultado y detalle de la regla (panel dividido en tablet)
- **Estadísticas** — conteo de llamadas bloqueadas por día/semana/mes
- **Copia de seguridad** — exporta/importa todas las reglas como JSON
- **Diseño adaptativo** — barra inferior en móvil, barra lateral en tablet/apaisado, contenido centrado a 600dp
- **Identidad visual** — icono "Barrera de Llamadas" (auricular azul marino, barrera coral, fondo crema)

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
./gradlew :shared:testDebugUnitTest          # 151 pruebas, commonTest + androidUnitTest (Robolectric)
./gradlew :shared:iosSimulatorArm64Test      # commonTest en Kotlin/Native (pospuesto)
```

## Regeneración de iconos

Si cambian los SVG fuente, regenera los 21 iconos PNG:

```sh
npm install --no-save sharp
node design/iconography/render_ui_icons.mjs
# Salida esperada: "Rendered and validated 21 interface icons."
```

## Licencia

MIT — consulta [Términos y Condiciones](docs/STORE_COMPLIANCE.md) en la app o en el repositorio público.
