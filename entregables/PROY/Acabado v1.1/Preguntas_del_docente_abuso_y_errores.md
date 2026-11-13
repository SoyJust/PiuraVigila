# PiuraVigila · Preguntas del docente: spam, abuso, errores de usuario y fotos

Respuestas basadas en lo que hace el sistema hoy (v1.1, 13/11/2026), con la prueba que lo demuestra. Cuando algo **no** está resuelto, se dice y se explica por qué.

Abreviaturas: **TC-** = prueba automática (`mvn test` / `mvn verify`); **AV11-** = evidencia en `Evidencias/`.

---

## 1. ¿Cómo evitamos el spam?

No hay una sola medida: son capas, y cada una cubre lo que la anterior deja pasar.

| Capa | Qué frena | Dónde está |
|---|---|---|
| 1. Suma de verificación (p. ej. «¿cuánto es 4 + 8?») guardada en la sesión del servidor | Programas que envían el formulario sin leerlo | `ControlAntiSpam` (TC de `ControlAntiSpamTest`) |
| 2. Límite por dispositivo: 1 reporte cada 60 s y 3 por día (cookie anónima `pv_dispositivo`) | Una persona que reporta una y otra vez | `ControlAntiSpam` (RF-06) |
| 3. Bloqueo por dispositivo en la base de datos | Varias peticiones **al mismo tiempo** desde el mismo teléfono (ver pregunta 2) | `ReporteService` + `pg_advisory_xact_lock` (AV11-02, TC-C01, TC-I01) |
| 4. Límite por IP: 20 envíos cada 10 min | Quien borra la cookie para parecer un teléfono nuevo | `ReporteController` + `LimitadorIntentos` (TC-A03) |
| 5. La misma foto exacta no se acepta dos veces en 24 h | Reenviar el mismo archivo desde varios teléfonos | `FotoStorageDisco` (TC-F04, AV11-04) |
| 6. Duplicados: misma categoría + mismo distrito + caso abierto → el vecino confirma si es el mismo | Muchos reportes del mismo problema (no son spam, pero se unen a un solo caso) | `ReporteService` (RF-05, RN-01) |
| 7. Revisión del personal: ve la foto, la descripción y el punto en el mapa | Reportes falsos que pasaron todo lo anterior | Detalle del caso (v1.1) |

**Lo que no hace (y por qué):**
- No pide nombre, DNI ni teléfono. Es una decisión del proyecto: el vecino reporta de forma anónima. Por eso no se puede «banear a una persona», solo frenar un dispositivo o una conexión.
- El límite por IP es holgado a propósito (20 cada 10 min): muchas personas pueden compartir una misma IP (el wifi de un colegio o los datos móviles, que en Perú suelen salir por una IP compartida). Si fuera estricto, bloquearía a vecinos legítimos; el límite fino es el de cada dispositivo. Lo comprobamos al probar: todas nuestras pruebas salían de la misma IP y, tras 20 envíos, el sistema empezó a responder «Demasiados envíos desde esta conexión».
- La suma de verificación es simple. Un atacante que programe un bot específico para PiuraVigila puede resolverla. En el despliegue (semana 15) se puede cambiar por un CAPTCHA de un tercero, como Cloudflare Turnstile o hCaptcha, sin tocar las demás capas.

---

## 2. Si alguien manda varias peticiones a la vez desde un solo teléfono

**Encontramos que esto SÍ era un error y lo corregimos.**

Se mandaron 5 reportes en el mismo instante desde el mismo teléfono (misma cookie y misma sesión):

| | Aceptados | Guardados |
|---|---|---|
| Antes del arreglo | 5 de 5 | 5 |
| Después del arreglo | 1 de 5 (los otros 4: «Espera antes de enviar otro: faltan 60 s») | 1 |

**Por qué pasaba:** el control de 60 s hacía «leer el último envío → si pasó un minuto, guardar». Las 5 peticiones leían al mismo tiempo, ninguna veía a las otras (todavía no se habían guardado) y las 5 guardaban. Es una *condición de carrera* clásica.

**Cómo se arregló:** al empezar a registrar, la transacción pide a PostgreSQL un bloqueo con la clave `dispositivo:<id>` (`pg_advisory_xact_lock`). La segunda petición del mismo teléfono espera a que la primera termine. Cuando entra, ya ve el reporte guardado y la regla de 60 s la rechaza. El bloqueo se libera solo al terminar la transacción, aunque falle.

Además, en el navegador el botón «Enviar reporte» se desactiva al primer clic («Enviando…»). Eso evita el doble clic normal, pero la protección real es la del servidor, porque un atacante no usa el botón.

Pruebas: AV11-02 (antes y después, contra la aplicación real), TC-C01 y TC-I01 (contra PostgreSQL: la segunda transacción esperó ≥ 400 ms a la primera).

---

## 3. Si varias personas deciden hacerlo a la vez

Hay dos casos distintos:

**a) Muchas personas reportan problemas distintos al mismo tiempo.**
- Cada uno recibe un código distinto, porque el número sale de una secuencia de PostgreSQL (`seq_codigo_reporte`): dos envíos simultáneos nunca reciben el mismo. La prueba PV-T37 inserta 100 casos en paralelo y verifica 100 códigos distintos.
- El servidor atiende varias peticiones en paralelo y las demás esperan en fila; no se pierden datos.
- Lo más costoso de cada envío es reducir la foto (una fracción de segundo).

**b) Muchas personas reportan el MISMO problema al mismo tiempo** (por ejemplo, 5 vecinos fotografían el mismo desagüe colapsado).
- **Antes:** podían crearse 5 casos distintos, porque los 5 buscaban «¿ya hay un caso abierto?» al mismo tiempo y ninguno lo encontraba.
- **Ahora:** se bloquea también la clave `caso:<categoría>:<distrito>`. El primero crea el caso; los otros 4 esperan, lo encuentran y ven la pantalla «¿Es el mismo problema?». Si confirman, se suman a ese caso sin código propio. Prueba AV11-03: 5 vecinos simultáneos → 1 caso nuevo y 4 pantallas de duplicado.
- **Sin interbloqueos:** siempre se bloquea primero el dispositivo y luego el caso, en ese orden (TC-C01). Claves distintas no se esperan entre sí (TC-I02).

**c) Dos personas del personal cambian el mismo caso a la vez.**
- Antes podían quedar dos filas «En proceso» en el historial.
- Ahora se bloquea `reporte:<id>`: el segundo ve que el caso ya está en proceso y recibe «El estado solo avanza» (TC-C06).

---

## 4. El spam de fotos: ¿cómo identifican un duplicado?

- **Mismo problema, distinta foto.** No comparamos fotos: así lo decidió la revisión del 15/10/2026, y por eso se quitó `hash_foto`. Dos vecinos nunca toman la misma foto del mismo bache (ángulo, luz, celular), y comparar imágenes «parecidas» es poco confiable y costoso. El duplicado se detecta por categoría + distrito + caso abierto, y lo confirma el propio vecino («Sí, es el mismo»).
- **La misma foto exacta reenviada** (spam). Se calcula la huella SHA-256 del archivo. Si ese mismo archivo ya se envió en las últimas 24 h, venga del teléfono que venga, se rechaza con «Esta misma foto ya se envió hace poco» (TC-F04, AV11-04). La huella vive en memoria y no se guarda en la base de datos (se respeta la decisión del 15/10).
- **Límite honesto:** si alguien cambia un píxel, la huella cambia y la foto pasa. Para ese caso quedan los límites de envío y la revisión del personal, que ahora sí ve la foto.
- **Fotos falsas u ofensivas:** no hay detección automática. Las ve el personal en el detalle del caso. Ver el pendiente del estado «Descartado» en la sección 7.

---

## 5. ¿No estamos gastando recursos al guardar fotos de 2 MB?

Sí, lo estábamos: se guardaba el archivo tal como llegaba. Se corrigió en dos lugares:

1. **En el celular, antes de enviar** (`reducir-foto.js`). La foto se reduce a 1600 px de lado mayor en JPEG. En la prueba, 0.71 MB pasaron a 0.13 MB (AV11-06). Esto ahorra datos móviles al vecino y evita el error «Foto demasiado pesada», común con fotos de celular de 3 a 8 MB.
2. **En el servidor, siempre**, aunque el navegador no lo haya hecho (`FotoStorageDisco`). Toda foto se vuelve a codificar: máximo 1600 px, JPEG al 82 %. En la prueba, 4000 × 3000 y 1253 KB pasaron a 1600 × 1200 y 255 KB, un 80 % menos (AV11-04, TC-F01).

**Retención.** Una foto sirve para atender el caso. Cuando el caso lleva más de 365 días atendido, una tarea diaria borra su foto y las de sus vinculados (`RetencionFotos`, configurable con `piuravigila.foto.retencion-dias`; `0` = nunca). El caso, su historial y sus datos quedan; el panel muestra «Foto no disponible» (TC-F06, TC-I05).

**Cuánto ocupa** (estimación con 300 KB por foto):

| Reportes por día | Por año | Con retención de 1 año tras atender |
|---|---|---|
| 20 | ≈ 2.1 GB | Se estabiliza en ≈ 2 a 3 GB |
| 100 | ≈ 11 GB | Se estabiliza en ≈ 11 a 14 GB |

Antes, con fotos de hasta 2 MB, eran hasta 6 o 7 veces más.

**¿Por qué no quitar las fotos?** Porque son la evidencia: el operador confirma que el problema existe, ve su gravedad para priorizar y descarta reportes falsos sin ir al lugar. Además, pedir una foto desanima los reportes en broma.

**Privacidad de las fotos:**
- Se borran los metadatos EXIF, que pueden traer la ubicación GPS de la casa del vecino y el modelo del teléfono (TC-F02, AV11-04).
- Solo las ve el personal con sesión iniciada: no hay ruta pública `/fotos/...` (devuelve 404) y la ruta del panel sin sesión responde 401 (TC-A04).
- La consulta pública del vecino nunca muestra la foto, porque podría mostrar rostros o placas.

---

## 6. Errores que puede provocar el usuario y cómo se resuelven

### Al registrar un reporte (vecino)

| Qué hace el usuario | Qué pasa | Prueba |
|---|---|---|
| Doble clic en «Enviar» o recarga la página después de enviar | El botón se desactiva al primer clic; si igual llega otro envío, el servidor lo rechaza por la regla de 60 s y el bloqueo por dispositivo | AV11-02 |
| Deja campos vacíos o desactiva el `required` del navegador | El servidor vuelve a validar todo: «Faltan campos obligatorios» | `ReporteServiceTest` |
| Escribe más de 300 caracteres (quitando el `maxlength`) | Se rechaza en el servidor | `OtrosServiciosTest` |
| Sube un archivo que no es imagen (un `.exe` renombrado a `.jpg`) | Se revisa el contenido real del archivo, no solo la extensión | `FotoStorageDiscoTest` |
| Sube una imagen enorme o una «bomba de descompresión» (pesa poco y mide 50 000 × 50 000) | Límite de 3 MB por archivo, 4 MB por petición y 20 megapíxeles | TC-F07 |
| Sube una foto de celular de 6 MB | El navegador la reduce antes de enviarla; sin JavaScript, el mensaje es «Foto demasiado pesada» | AV11-06 |
| Marca un punto fuera de Piura (por error o a propósito) | «La ubicación marcada está fuera de la provincia de Piura» | TC-C07 |
| Elige un distrito que no coincide con el punto del mapa | **No se valida** (haría falta el contorno de cada distrito). El personal lo ve en el mapa del detalle | Pendiente aceptado |
| Escribe HTML o JavaScript en la descripción (`<script>`) | Thymeleaf lo muestra como texto (`th:text`) y la CSP bloquea scripts en línea | TC-SEC-05, TC-SEC-08 |
| Escribe SQL en la descripción (`'; DROP TABLE ...`) | Se guarda como texto: todas las consultas usan parámetros | TC-SEC-07 |
| Escribe una fórmula (`=HYPERLINK(...)`) que luego terminará en el Excel | En el Excel se guarda como texto, nunca como fórmula | TC-X02 |
| Responde mal la suma | Se muestra «Verificación incorrecta» y una suma nueva | `ControlAntiSpamTest` |
| Borra las cookies para saltarse el límite diario | Aplica el límite por IP (20 cada 10 min) | TC-A03 |
| Cambia de wifi a datos móviles **y además** borra cookies | Queda como teléfono nuevo con IP nueva: **puede saltarse los límites**. Es el riesgo residual de no pedir identificación; se mitiga con el CAPTCHA del despliegue y la revisión del personal | Riesgo aceptado |
| Se queda sin internet a mitad del envío | No se guarda nada a medias (todo va en una transacción); vuelve a enviar | Diseño |
| Dice «No, es otro» cuando sí era el mismo | Se crea un caso nuevo; el personal puede atender ambos | Diseño |

### Al consultar (vecino)

| Qué hace el usuario | Qué pasa | Prueba |
|---|---|---|
| Escribe el código en minúsculas o con espacios (` pr-2026-0001 `) | Se normaliza y funciona | `ReporteServiceTest` |
| Escribe un código que no existe | Mensaje genérico «Código no encontrado», con ayuda | `ReporteControllerTest` |
| Prueba códigos uno por uno (PR-2026-0001, 0002…) para ver reportes ajenos | Tras 60 consultas en 10 min desde una IP: «Demasiadas consultas». La consulta no muestra foto, ni datos del vecino, ni quién atendió | TC-A07 |

### En el panel (personal)

| Qué hace el usuario | Qué pasa | Prueba |
|---|---|---|
| Prueba contraseñas (fuerza bruta) | 5 fallos para un usuario, o 20 desde una IP, en 15 min: bloqueo temporal, aunque después acierte | TC-A01, TC-A02 |
| Olvida su contraseña | El administrador le asigna una nueva («Restablecer contraseña») | TC-U08 |
| El administrador intenta desactivarse a sí mismo | «No puedes desactivar tu propia cuenta» | TC-U06 |
| Se desactiva una cuenta que tiene la sesión abierta | Pierde la sesión en su siguiente clic | TC-U15 |
| Un operador intenta entrar a «Usuarios» o habilitar categorías | Se rechaza (solo administrador) | TC-U11, PV-T41 |
| Intenta retroceder un estado (Atendido → Recibido) o saltarse uno | «El estado solo avanza» | `ReporteServiceTest` |
| Dos operadores cambian el mismo caso a la vez | El segundo ve el cambio del primero; no se duplica el historial | TC-C06 |
| Escribe un comentario de más de 200 caracteres | Se rechaza sin cambiar el estado | TC-C04 |
| Escribe datos personales en el comentario (lo verá el público) | El formulario advierte «No escribas datos personales»; no hay filtro automático | Aviso en pantalla |
| Alguien le envía un enlace malicioso que intenta cambiar estados desde otro sitio (CSRF) | Todo formulario exige un token de la sesión | PV-T39, TC-SEC-04 |
| Manipula el parámetro de regreso para redirigir a otro sitio | Solo se acepta un código con formato válido; si no, vuelve al panel | TC-A06 |
| Abre la foto de un caso sin sesión (copiando el enlace) | 401 | TC-A04 |

---

## 7. Lo que sigue pendiente (dicho con honestidad)

1. **Estado «Descartado».** Hoy un reporte falso o improcedente solo puede terminar como «Atendido», lo que infla las cifras. Lo correcto sería un cuarto estado «Descartado» con comentario obligatorio. Cambia el modelo de estados aprobado (CHECK de la base, transiciones y R03), así que conviene consultarlo antes con el docente.
2. **CAPTCHA real** en el despliegue (semana 15), en lugar de la suma.
3. **Límites que se reinician si se reinicia la aplicación.** Los límites por IP y las huellas de fotos viven en memoria. Para un solo servidor es suficiente; con varios servidores habría que guardarlos en la base o en Redis.
4. **Distrito frente al punto del mapa:** no se cruza con el contorno de cada distrito.
5. **Foto huérfana:** si la base de datos falla justo después de guardar la foto, el archivo queda en disco sin reporte (limitación ya documentada; la retención no lo borra).
