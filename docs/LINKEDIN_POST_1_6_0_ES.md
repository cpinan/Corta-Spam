# Post de actualización 1.6.0 — LinkedIn (español)

Distinto del post de lanzamiento ([`LINKEDIN_POST_ES.md`](LINKEDIN_POST_ES.md)), que anunciaba la
app por primera vez. Este cuenta lo que cambió después de que la usara gente real.

**Al pegar en LinkedIn:** copia solo el bloque "Texto del post". LinkedIn no interpreta Markdown —
los `•` y los saltos de línea sí pasan, negritas y títulos no. **El límite es 3.000 caracteres**;
el texto de abajo está contado y cabe.

**Las imágenes no son opcionales en este post.** El texto dice que los QR de Yape y Plin están
en las imágenes, así que hay que adjuntarlos o esa línea miente. Adjunta, en este orden:

1. **`docs/donate/yape-plin-linkedin.png`** — los dos QR juntos en una sola imagen, con título y
   la nota de que no hay comisión. Va primera a propósito: en un carrusel mucha gente no desliza,
   así que los dos códigos tienen que estar en la primera imagen o la mitad no los ve.
2. Opcionalmente `docs/donate/yape-qr.png` y `docs/donate/plin-qr.png` por separado, para quien
   quiera escanear uno a pantalla completa.
3. Opcionalmente una captura de la Agenda o del teclado.

Los dos QR de la imagen combinada están verificados: decodifican a los mismos 107 y 125 caracteres
que los originales después del reescalado, así que el montaje no rompió nada.

**Por qué los QR van como imagen y no como número:** un QR se escanea de una vez y no publica tu
celular. Escribir el número en el post lo deja indexado y raspable para siempre — que es
exactamente el tipo de dato que esta app existe para proteger. Si aun así prefieres poner el
número, es tu decisión, pero entonces cámbialo también en `DONATE_ES.md` para que ambos digan lo
mismo.

---

## Texto del post

Alguien me escribió que el teclado de mi app se veía roto.

Tenía razón. Y mis 721 pruebas automatizadas no tenían forma de saberlo: pasaban todas.

Corta Spam bloquea llamadas spam con tus propias reglas, antes de que el teléfono suene. Está en Google Play, es gratis, y el código es público. Lo que cambió desde que empecé a mirar la app en vez de solo mis pruebas:

• Agenda. Toda tu libreta, con favoritos, buscador y filtros de Bloqueados y Permitidos. Eso responde algo que ninguna otra app del teléfono puede: la lista de bloqueo guarda números, no sabe a cuáles de las personas que conoces has silenciado.

• El teclado, rehecho. Los resultados de contactos ahora flotan sobre la pantalla en vez de reservar una franja vacía — eso era lo que se veía roto. Escribe "ana" o escribe 611: encuentra igual.

• El teclado ya no deja un tercio de la pantalla en blanco: las teclas se miden contra la ventana real, no contra una estimación que erraba por toda una fila.

• Tus contactos más probables encima del teclado: favoritos, o las últimas cuatro llamadas. Con un interruptor para apagarlo: el historial en la pantalla que le prestas a alguien es historial que le prestas.

• 223 nombres de países, traducidos. Estaban en inglés dentro de la regla, así que un registro en español decía "País: Morocco", y ningún archivo de idioma llegaba a ellos.

• La excepción por llamada de emergencia ahora caduca de verdad. Confiaba en una señal del sistema que en algunos teléfonos nunca se apaga — y mientras siguiera encendida, el bloqueo quedaba desactivado.

• Deslizar para recargar, pulsación larga para borrar el número entero, y ajustes desde un engranaje en el inicio.

Sigue sin pedirte nada: sin cuentas, sin anuncios, sin rastreo, sin permiso de internet. No tiene código capaz de enviar nada a ningún lado, y está publicado para que lo compruebes tú.

Por dentro: Kotlin Multiplatform, Compose Multiplatform, SQLDelight y Koin. 721 pruebas en JVM, 369 en el simulador de iOS, y scripts que hacen llamadas reales en un emulador — porque seis funcionalidades se publicaron con las pruebas en verde y sin ejecutarse nunca.

Gracias a Sig Mandel, Faride Altamirano, Jose Arellano y Augusto Piñán, que la instalaron y me dijeron qué no funcionaba. Los peores bugs salieron de ahí. Y a Claude Code, mi par de programación.

Play: https://play.google.com/store/apps/details?id=org.carlospinan.cortaspam
Código (MIT): https://github.com/cpinan/Corta-Spam
Web: https://cpinan.github.io/corta-spam/

Es gratis y va a seguir siéndolo. Lo que más ayuda no cuesta nada: calificarla en Play, una estrella en el repo, o traducirla. Y si quieres invitarme un café:

🇵🇪 Yape y Plin: los QR están en las imágenes de este post. Ábrelos y escanéalos desde tu app; no hay comisión para ninguno de los dos.
💜 GitHub Sponsors: https://github.com/sponsors/cpinan
☕ Ko-fi: https://ko-fi.com/carlospinan
💳 PayPal: https://paypal.me/carlospinan

#Android #KotlinMultiplatform #ComposeMultiplatform #OpenSource #Privacidad #Perú

---

## Notas

- **Primera línea = el gancho.** LinkedIn corta tras ~3 líneas con un "ver más". Las dos primeras
  frases funcionan solas y son verdad: el band de 132 dp reservado bajo el buscador se leía como un
  error de renderizado, y ninguna prueba de componente podía detectarlo.
- Las cifras son reales a 2026-08-26: **721** pruebas JVM (`:shared` 629 + `:androidApp` 92) y
  **369** en el simulador de iOS. Están en el CHANGELOG.
- Lo de "seis funcionalidades publicadas sin ejecutarse nunca" está documentado en el README y en
  el curso — si alguien pregunta en comentarios, hay dónde apuntarle.
- **Los enlaces de donación van aquí, no en la app ni en la ficha de Play.** La app no tiene código
  de cobros ni aviso de donación, y la descripción corta de Play no admite información promocional.
  El razonamiento completo está en `DONATE_ES.md`.
- Antes de publicar: abre `github.com/sponsors/cpinan`, `ko-fi.com/carlospinan` y
  `paypal.me/carlospinan` una vez, y comprueba que la ficha de Play sigue en 200.
