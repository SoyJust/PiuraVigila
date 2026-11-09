# PiuraVigila — Documentación de diseño v0.6
Curso Integrador I: Sistemas - Software · Semana 7 · rev. 09/11/2026 (alineado con BD v0.5 rev. 15/10/2026 y el código de APF3)

## 1. Pantallas

### P01 · Registrar reporte de incidencia
- **Actor:** Ciudadano
- **Objetivo:** enviar un reporte con evidencia mínima y recibir un código de seguimiento.
- **Requerimiento:** RF-01 (también RF-05, RF-06 y RN-01)
- **Campos:** categoría (lista), foto JPG/PNG (máx. 2 MB), zona (lista), ubicación en el mapa (opcional), descripción breve (máx. 300), suma de verificación.
- **Acciones:** enviar reporte, cancelar.
- **Reglas:**
  - Control anti-spam (RF-06): deben pasar 60 s entre envíos del mismo dispositivo, máximo 3 reportes por día y la suma de verificación debe ser correcta; si no, se muestra el motivo y no se guarda.
  - Si existe un caso abierto (Recibido o En proceso) de la misma categoría y zona, se pregunta «¿Es el mismo caso?» (RF-05, RN-01): **Sí** vincula el reporte al caso sin generar código nuevo; **No** crea un caso nuevo.
  - Si se registra correctamente, se muestra el código con formato `PR-AAAA-####`.
- **Entidades:** Reporte, Categoria, Zona.

### P02 · Consultar estado del reporte
- **Actor:** Ciudadano
- **Objetivo:** ver el estado actual de un reporte ya enviado sin volver a llamar.
- **Requerimiento:** RF-02
- **Campos:** código de seguimiento.
- **Resultado:** código, categoría, estado, fecha de registro, última actualización y línea de tiempo (Recibido → En proceso → Atendido).
- **Reglas:**
  - Si el código no existe, se muestra "Código no encontrado. Verifica que esté bien escrito".
  - La última actualización es la fecha del último registro de `historial_estado`; la línea de tiempo no muestra al responsable de cada cambio.
  - (Rev. 09/11/2026: se retiró «próxima acción», que no tenía respaldo en la BD.)
- **Entidades:** Reporte, HistorialEstado.

### P03 · Panel de gestión municipal
- **Actor:** Personal municipal (operador / administrador)
- **Objetivo:** filtrar reportes por zona, tipo y estado, y cambiar su estado.
- **Requerimiento:** RF-03 (acceso: RNF-01); también RF-07
- **Campos:** usuario, contraseña, filtros (zona, tipo, estado), listado con selector de estado, historial del reporte.
- **Acciones:** iniciar sesión, filtrar, cambiar estado, guardar cambios, habilitar categoría nueva (RF-07, solo administrador), descargar zonas en Excel.
- **Reglas:**
  - Sin sesión iniciada no se muestra el panel.
  - Al guardar un cambio de estado, se muestra "Estado actualizado" y queda registrado con fecha y usuario responsable en el historial.
  - Cuando los reportes en "Otra" superan el 10 % del mes calendario (A14), el panel muestra un aviso y solo el administrador puede habilitar la categoría nueva.
  - Las acciones del panel exigen sesión y token CSRF; la contraseña se guarda como hash BCrypt.
- **Entidades:** Reporte, HistorialEstado, UsuarioMunicipal.

### P04 · Mapa de zonas críticas
- **Actor:** Personal municipal
- **Objetivo:** identificar en qué zonas se concentran más incidencias para priorizar atención.
- **Requerimiento:** RF-04 (prioridad Media)
- **Campos:** periodo (7 días / mes / 3 meses / año).
- **Resultado:** conteo de reportes por zona (distritos: Piura, Castilla, Veintiséis de Octubre, Catacaos, La Arena, La Unión), más oscuro = más reportes; además, mapa con los reportes que tienen ubicación, coloreados por estado.
- **Entidades:** Reporte, Zona.

## 2. Reportes

### R01 · Reporte de zonas críticas
- **Pregunta que responde:** ¿en qué zonas se concentran más incidencias en un periodo?
- **Usuario:** personal municipal.
- **Datos usados:** Reporte, Zona.
- **Frecuencia:** semanal.
- **Formato:** cuadrícula por zona + mapa de puntos + exportación a Excel.
- **Uso posterior:** priorizar las zonas donde atender primero.

### R03 · Resumen de gestión
- **Pregunta que responde:** ¿cuántos casos hay por estado, cuántos duplicados se evitaron, qué % tiene estado actualizado y cuánto pesa «Otra»?
- **Usuario:** personal municipal.
- **Datos usados:** Reporte, HistorialEstado, Categoria.
- **Frecuencia:** bajo demanda (por defecto, últimos 30 días).
- **Formato:** tabla en el panel.
- **Uso posterior:** seguimiento general y alerta de categorías faltantes (RF-07).

### R02 · Tiempos de atención
- **Pregunta que responde:** ¿cuánto tarda un reporte en pasar de Recibido a Atendido?
- **Usuario:** administrador municipal.
- **Datos usados:** Reporte, HistorialEstado, Categoria.
- **Frecuencia:** mensual.
- **Formato:** tabla + gráfico de barras.
- **Uso posterior:** detectar categorías con demora y reasignar prioridades.

## 3. Catálogo de mensajes

| Mensaje | Pantalla | Cuándo aparece | Acción del sistema |
|---|---|---|---|
| ¿Es el mismo caso? | P01 | Existe un caso abierto de la misma categoría y zona | Sí: vincula al caso sin código nuevo; No: crea un caso nuevo |
| Espera antes de enviar otro reporte | P01 | No pasaron 60 s desde el último envío del dispositivo | No se guarda; indica los segundos que faltan |
| Límite diario alcanzado | P01 | El dispositivo ya envió 3 reportes hoy | No se guarda el reporte |
| Verificación incorrecta | P01 | La suma de verificación no es correcta | No se guarda; se genera otra suma |
| Código registrado | P01 | Reporte guardado correctamente | Muestra código `PR-AAAA-####` |
| Código no encontrado | P02 | El código no existe o el formato es inválido | Pide revisar el código |
| Estado actualizado | P03 | Se guarda un cambio de estado | Registra fecha y usuario responsable en el historial |
| Sesión requerida | P03 | Se intenta abrir el panel sin sesión iniciada | No muestra el panel |

## 4. Trazabilidad (resumen)

| ID | Requerimiento | Actividad BPMN | Entidad/BD | Pantalla | Reporte |
|---|---|---|---|---|---|
| T01 | RF-01 | A02 | Reporte, Categoria, Zona | P01 | R01 |
| T02 | RF-02 | A08 | Reporte, HistorialEstado | P02 | — |
| T03 | RF-03 | A10/A11 | Reporte, HistorialEstado, UsuarioMunicipal | P03 | R02 |
| T04 | RF-04 | A13 | Reporte, Zona | P04 | R01 |
| T05 | RF-07 | A14 | Categoria | P03 | R03 |
| T06 | RF-05 / RN-01 | A05/A06 | Reporte (reporte_original_id) | P01 | R03 |
| T07 | RF-06 | A03/A04 | Reporte (dispositivo_id, fecha_registro) | P01 | — |

## 5. Documentación relacionada
- **D01 — Guía rápida de usuario:** ver sección 6 (agregada 27/09/2026).
- **D02 — Diseño funcional de pantallas:** este documento; base para la arquitectura MVC/DAO de Semana 9.
- **D03 — Catálogo de reportes:** ver sección 7 (agregada 27/09/2026).
- **D04 — Comentarios Javadoc:** escritos en el código de APF3 (DAO, Service y Controller) en el repositorio SoyJust/PiuraVigila.

---
*Fuentes: Plantilla Semana 7 UX/UI v0.6, Prototipo v0.3, BPM v0.4, Diseño de BD v0.5.*

## 6. D01 — Guía rápida de usuario

### Para el ciudadano

**Reportar un problema (P01)**
1. Abre la app y elige "Registrar reporte".
2. Selecciona la categoría que más se parece al problema (bache, luminaria dañada, basura, etc.). Si no encaja en ninguna, elige "Otra".
3. Toma o sube una foto del problema.
4. Elige la zona donde está y, si quieres, marca el punto en el mapa.
5. Escribe una descripción breve y resuelve la suma de verificación.
6. Presiona "Enviar reporte". Si ya hay un caso abierto igual en tu zona, te preguntaremos si es el mismo: si respondes que sí, usa el código de ese caso. Vas a recibir un código con formato `PR-AAAA-####` — guárdalo, lo necesitas para consultar el estado después.

**Consultar el estado de un reporte (P02)**
1. Abre "Consultar estado".
2. Escribe el código que recibiste.
3. Presiona "Consultar". Vas a ver la categoría, el estado actual, la última actualización y la línea de tiempo del caso.

### Para el personal municipal

**Revisar y atender reportes (P03)**
1. Inicia sesión con tu usuario y contraseña.
2. Usa los filtros (zona, tipo, estado) para encontrar los reportes que te interesan.
3. Cambia el estado de un reporte (Recibido → En proceso → Atendido) cuando corresponda.
4. Presiona "Guardar cambios" — queda registrado con tu nombre y la fecha.
5. Si aparece el aviso de que la categoría "Otra" superó el umbral, el administrador puede habilitar una categoría nueva desde ahí mismo.

**Ver el mapa de zonas críticas (P04)**
1. Elige el periodo que quieres revisar (7 días, mes, 3 meses o año).
2. La cuadrícula muestra qué zonas tienen más reportes (más oscuro = más reportes) y el mapa ubica cada reporte con su estado.
3. Desde el panel puedes descargar los conteos por zona en Excel.

### Mensajes frecuentes

| Si ves esto... | Significa... |
|---|---|
| "¿Es el mismo caso?" | Ya hay un caso abierto de la misma categoría en tu zona; si es el mismo, tu reporte se suma a ese caso. |
| "Espera antes de enviar otro reporte" | Deben pasar 60 segundos entre dos reportes. |
| "Límite diario alcanzado" | Este dispositivo ya envió 3 reportes hoy. |
| "Código no encontrado" | El código está mal escrito o no existe. |
| "Estado actualizado" | El cambio del personal municipal se guardó correctamente. |
| "Sesión requerida" | Hay que iniciar sesión antes de ver el panel. |

---

## 7. D03 — Catálogo de reportes

| Reporte | Pregunta que responde | Usuario | Fuente de datos | Indicador | Frecuencia |
|---|---|---|---|---|---|
| R01 · Zonas críticas | ¿En qué zonas se concentran más incidencias en un periodo? | Personal municipal | Reporte, Zona | Conteo de reportes por zona | Semanal |
| R02 · Tiempos de atención | ¿Cuánto tarda un reporte en pasar de Recibido a Atendido? | Administrador municipal | Reporte, HistorialEstado, Categoria | Días promedio entre el primer estado y "Atendido", por categoría | Mensual |
| R03 · Resumen de gestión | ¿Cuántos casos hay por estado y cuántos duplicados se evitaron? | Personal municipal | Reporte, HistorialEstado, Categoria | Casos por estado, duplicados evitados, % con estado actualizado, % «Otra» | Bajo demanda |

Los tres reportes se apoyan en datos que ya existen en la base de datos v0.5 — no requieren tablas adicionales, solo consultas sobre `reporte`, `zona`, `historial_estado` y `categoria`.
