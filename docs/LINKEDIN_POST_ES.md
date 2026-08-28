# Post de lanzamiento — LinkedIn (español)

**Pendiente antes de publicar:** rellenar los dos `[NOMBRE]` / `[QUÉ HIZO]` al final.

**Al pegar en LinkedIn:** copia solo el bloque "Texto del post". LinkedIn no interpreta
Markdown — los `•` y los saltos de línea sí pasan, negritas y títulos no. Revisa que los
saltos entre párrafos sobrevivan al pegado.

---

## Texto del post

Estoy cansado de contestar el teléfono para que alguien me ofrezca un préstamo que no pedí.

Empecé a bloquear números a mano. No sirvió: al día siguiente llamaban desde otro, del mismo rango, con dos dígitos distintos. Probé las apps que hay y casi todas querían lo mismo a cambio: mi agenda subida a su servidor, o anuncios entre mis llamadas. No me pareció un buen trato dar mis contactos para dejar de recibir llamadas de desconocidos.

Así que la hice yo. Se llama Corta Spam y desde hoy está en Google Play.

Reemplaza tu app de teléfono. Eso significa que ve la llamada antes de que suene, y ahí decide. Lo que puedes hacer con eso:

• Bloquear números concretos, con una etiqueta que después el registro te recuerda — para no preguntarte en tres meses quién era ese número

• Patrones con comodines (+34900*, *1234), que es la única forma de ganarle a quien cambia de número dentro del mismo rango

• Bloquear un país entero del que nunca esperas una llamada

• Bloquear un número tras N intentos en poco tiempo

• Horas de silencio por horario, con presets de Noche, Siesta y Trabajo

• Una lista de spam que viene dentro de la app, sin conexión, nunca

• Un auto-respondedor que contesta con un saludo tuyo, en voz sintetizada o grabada

• Grabar lo que dice quien llama después del saludo (apagado por defecto, y con una frase de consentimiento, porque en varios países grabar sin avisar no es legal)

• Un teclado que también busca en tu agenda: escribe "ana" o escribe 611, da igual cómo tengas guardado el número

• Un registro con todas tus llamadas, entrantes y salientes, diciéndote qué regla actuó, con buscador y filtros por dirección, resultado y fecha

• El nombre del contacto en la pantalla de llamada, que suena raro tener que mencionarlo, pero es donde "¿quién es?" es toda la pregunta

• Notificaciones con los botones que hacen falta: Devolver la llamada, Bloquear, Permitir siempre

• Cuántas llamadas te bloqueó esta semana, este mes

• Exportar e importar todas tus reglas como un archivo, por si cambias de teléfono

• Pantalla dividida en tablet, y cuatro idiomas: español, inglés, portugués e hindi

Tus contactos siempre pasan. Y todo lo demás se queda en tu teléfono: sin cuentas, sin anuncios, sin rastreo, sin permiso de internet. No es una promesa de marketing, es que la app no tiene código capaz de enviar nada a ningún lado, y el código está publicado para que cualquiera lo compruebe.

Por dentro: Kotlin Multiplatform con Compose Multiplatform, SQLDelight y Koin. 571 pruebas automatizadas, y además unos scripts que hacen llamadas de verdad en un emulador — porque descubrí, de la peor manera, que seis funcionalidades llegaron a publicarse con las pruebas en verde y sin ejecutarse una sola vez. Ahora las pruebas levantan el teléfono.

Nada de esto lo hice solo.

Gracias a [NOMBRE], por [QUÉ HIZO].

Gracias a [NOMBRE], por [QUÉ HIZO].

Gracias a quienes la instalaron en su teléfono y me escribieron para decirme que algo no funcionaba. Los peores bugs salieron de ahí, no de mis pruebas — y varios eran cosas que yo habría jurado que estaban bien.

Y a Claude Code, que fue mi par de programación de principio a fin.

Está en Play: https://play.google.com/store/apps/details?id=org.carlospinan.cortaspam

El código, con licencia MIT: https://github.com/cpinan/Corta-Spam

Y la web: https://cpinan.github.io/corta-spam/

Si te llaman tanto como a mí, pruébala y dime qué le falta.

#Android #KotlinMultiplatform #ComposeMultiplatform #OpenSource #Privacidad

---

## Notas

- La URL de Play está derivada del `applicationId` (`org.carlospinan.cortaspam`) en
  `androidApp/build.gradle.kts`. Ábrela una vez antes de publicar el post.
- Las 571 pruebas son el número real del repo a 2026-08-14 (`:shared` 494 + `:androidApp` 77).
- Lo de "seis funcionalidades publicadas sin ejecutarse nunca" también es real, y está
  documentado en el README y en el curso — si alguien pregunta en comentarios, hay dónde
  apuntarle.
- Primera línea = el gancho. LinkedIn corta el resto tras ~3 líneas con un "ver más", así
  que si cambias el arranque, que la frase siga funcionando sola.
