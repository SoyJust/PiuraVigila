# PiuraVigila — Documentación de diseño v0.6
Curso Integrador I: Sistemas - Software · Semana 7

## 1. Pantallas

### P01 · Registrar reporte de incidencia
- **Actor:** Ciudadano
- **Objetivo:** enviar un reporte con evidencia mínima y recibir un código de seguimiento.
- **Requerimiento:** RF-01 (también RF-05, RF-06)
- **Campos:** categoría (lista), foto, zona (lista), descripción breve.
- **Acciones:** enviar reporte, cancelar.
- **Reglas:**
  - Si el reporte es duplicado (misma foto y zona en un tiempo cercano), se asocia al código existente y no se crea uno nuevo.
  - Si el dispositivo superó el límite diario, se muestra un mensaje y no se guarda.
  - Si se registra correctamente, se muestra el código con formato `PR-AAAA-####`.
- **Entidades:** Reporte, Categoria, Zona.

### P02 · Consultar estado del reporte
- **Actor:** Ciudadano
- **Objetivo:** ver el estado actual de un reporte ya enviado sin volver a llamar.
- **Requerimiento:** RF-02
- **Campos:** código de seguimiento.
- **Resultado:** código, categoría, estado, fecha de registro, próxima acción.
- **Reglas:**
  - Si el código no existe, se muestra "Código no encontrado. Verifica que esté bien escrito".
  - "Próxima acción" **no es una columna de la BD v0.5** — se deriva de `reporte.estado` en tiempo de consulta: `Recibido` → "En revisión"; `En proceso` → "Programado para atención"; `Atendido` → "Reporte cerrado".
- **Entidades:** Reporte, HistorialEstado.

### P03 · Panel de gestión municipal
- **Actor:** Personal municipal (operador / administrador)
- **Objetivo:** filtrar reportes por zona, tipo y estado, y cambiar su estado.
- **Requerimiento:** RF-03 (acceso: RNF-01); también RF-07
- **Campos:** usuario, contraseña, filtros (zona, tipo, estado), listado con selector de estado, historial del reporte.
- **Acciones:** iniciar sesión, filtrar, cambiar estado, guardar cambios, habilitar categoría nueva (RF-07).
- **Reglas:**
  - Sin sesión iniciada no se muestra el panel.
  - Al guardar un cambio de estado, se muestra "Estado actualizado" y queda registrado con fecha y usuario responsable en el historial.
  - Cuando la categoría "Otra" supera el umbral definido (A14), el panel muestra un aviso y permite habilitar la categoría nueva. Antes de esta corrección (27/09/2026), RF-07 no tenía ningún punto de acción en el UI.
- **Entidades:** Reporte, HistorialEstado, UsuarioMunicipal.

### P04 · Mapa de zonas críticas
- **Actor:** Personal municipal
- **Objetivo:** identificar en qué zonas se concentran más incidencias para priorizar atención.
- **Requerimiento:** RF-04 (prioridad Media)
- **Campos:** periodo (7 días / mes / 3 meses / año).
- **Resultado:** conteo de reportes por zona, más oscuro = más reportes.
- **Nota:** valores de ejemplo en el boceto; se recalculan cuando exista la BD poblada.
- **Entidades:** Reporte, Zona.

## 2. Reportes

### R01 · Reporte de zonas críticas
- **Pregunta que responde:** ¿en qué zonas se concentran más incidencias en un periodo?
- **Usuario:** personal municipal.
- **Datos usados:** Reporte, Zona.
- **Frecuencia:** semanal.
- **Formato:** mapa por zona + tabla.
- **Uso posterior:** priorizar las zonas donde atender primero.

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
| Duplicado | P01 | Misma foto y zona en un tiempo cercano | Se asocia al código existente, no se crea uno nuevo |
| Límite diario alcanzado | P01 | El dispositivo superó el límite de reportes del día | No se guarda el reporte |
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
| T05 | RF-07 | A14 | Categoria | P03 | — |

## 5. Documentación relacionada
- **D01 — Guía rápida de usuario:** ver sección 6 (agregada 27/09/2026).
- **D02 — Diseño funcional de pantallas:** este documento; base para la arquitectura MVC/DAO de Semana 9.
- **D03 — Catálogo de reportes:** ver sección 7 (agregada 27/09/2026).
- **D04 — Comentarios Javadoc futuros:** pendiente hasta que exista el código Java (Semanas 9-12); no aplica todavía.

---
*Fuentes: Plantilla Semana 7 UX/UI v0.6, Prototipo v0.3, BPM v0.4, Diseño de BD v0.5.*

## 6. D01 — Guía rápida de usuario

### Para el ciudadano

**Reportar un problema (P01)**
1. Abre la app y elige "Registrar reporte".
2. Selecciona la categoría que más se parece al problema (bache, luminaria dañada, basura, etc.). Si no encaja en ninguna, elige "Otra".
3. Toma o sube una foto del problema.
4. Elige la zona donde está.
5. Escribe una descripción breve.
6. Presiona "Enviar reporte". Vas a recibir un código con formato `PR-AAAA-####` — guárdalo, lo necesitas para consultar el estado después.

**Consultar el estado de un reporte (P02)**
1. Abre "Consultar estado".
2. Escribe el código que recibiste.
3. Presiona "Consultar". Vas a ver la categoría, el estado actual y la próxima acción.

### Para el personal municipal

**Revisar y atender reportes (P03)**
1. Inicia sesión con tu usuario y contraseña.
2. Usa los filtros (zona, tipo, estado) para encontrar los reportes que te interesan.
3. Cambia el estado de un reporte (Recibido → En proceso → Atendido) cuando corresponda.
4. Presiona "Guardar cambios" — queda registrado con tu nombre y la fecha.
5. Si aparece el aviso de que la categoría "Otra" superó el umbral, puedes habilitar una categoría nueva desde ahí mismo.

**Ver el mapa de zonas críticas (P04)**
1. Elige el periodo que quieres revisar (7 días, mes, 3 meses o año).
2. El mapa muestra qué zonas tienen más reportes — más oscuro significa más reportes.

### Mensajes frecuentes

| Si ves esto... | Significa... |
|---|---|
| "Duplicado" | Ya existe un reporte parecido reciente; se usó el código anterior. |
| "Límite diario alcanzado" | Este dispositivo ya reportó demasiadas veces hoy. |
| "Código no encontrado" | El código está mal escrito o no existe. |
| "Estado actualizado" | El cambio del personal municipal se guardó correctamente. |
| "Sesión requerida" | Hay que iniciar sesión antes de ver el panel. |

---

## 7. D03 — Catálogo de reportes

| Reporte | Pregunta que responde | Usuario | Fuente de datos | Indicador | Frecuencia |
|---|---|---|---|---|---|
| R01 · Zonas críticas | ¿En qué zonas se concentran más incidencias en un periodo? | Personal municipal | Reporte, Zona | Conteo de reportes por zona | Semanal |
| R02 · Tiempos de atención | ¿Cuánto tarda un reporte en pasar de Recibido a Atendido? | Administrador municipal | Reporte, HistorialEstado, Categoria | Días promedio entre el primer estado y "Atendido", por categoría | Mensual |

Ambos reportes se apoyan en datos que ya existen en la base de datos v0.5 — no requieren tablas adicionales, solo consultas sobre `reporte`, `zona`, `historial_estado` y `categoria`.
