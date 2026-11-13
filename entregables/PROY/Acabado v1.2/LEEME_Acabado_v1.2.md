# PiuraVigila · Acabado v1.2: estado «Descartado» (13/11/2026)

## Por qué

Hasta la v1.1, un reporte falso, de prueba o que no correspondía a la vía pública (por ejemplo, «el foco de mi sala no prende») solo podía terminar como «Atendido». Eso tenía dos problemas:
- inflaba las cifras de casos atendidos y de zonas críticas;
- le decía al vecino que se había atendido algo que nunca se atendió.

## Qué cambió

**Reglas** (`EstadoReporte`, `ReporteService`)
- Un caso **abierto** (Recibido o En proceso) puede pasar a **Descartado**. Un caso Atendido no.
- **Descartado es un estado final**, como Atendido: no cambia más y no admite que otros vecinos se unan. Un reporte nuevo del mismo tipo en el mismo distrito crea un caso nuevo.
- **Descartar exige un motivo de al menos 10 caracteres.** Lo comprueban la aplicación y también la base de datos (restricción `ck_historial_motivo`), así que no hay forma de descartar sin explicar.
- El flujo normal no cambia: Recibido → En proceso → Atendido, sin saltarse pasos.

**Pantallas**
- **Detalle del caso:** «Descartado» aparece en «Nuevo estado». Al elegirlo, el comentario pasa a ser obligatorio y aparece un aviso de que se cierra el caso y el vecino verá el motivo.
- **Lista de casos:** el cambio rápido no ofrece «Descartado», porque exige motivo. Cada caso abierto tiene un enlace «Descartar…» que lleva al detalle.
- **Panel:** 5 tarjetas (por atender, en proceso, atendidos, **descartados**, total). La tarjeta de descartados filtra la lista.
- **Consulta del vecino:** la línea de tiempo muestra Recibido → (En proceso, si llegó) → **Descartado**, en gris, con «Motivo: «…»».
- **Mapa:** los puntos descartados aparecen en gris, con su leyenda.
- **Inicio:** «% de casos resueltos» ya no cuenta los descartados.

**Reportes**
- **R01 (zonas críticas)** y el mapa por distrito **no cuentan** los reportes descartados ni a los vecinos unidos a ellos: un reporte falso no vuelve «crítica» una zona.
- **R03** muestra los descartados como un estado más.
- **Excel:**
  - la hoja «Resumen» agrega «Descartados» y calcula el % atendido sin contarlos;
  - en «Casos», un descartado no suma «días abierto».
- **Retención de fotos:** también se borran las de casos descartados hace más de 365 días.

**Base de datos**
- `ck_reporte_estado` y `ck_historial_estado` aceptan `'Descartado'`.
- Nueva restricción `ck_historial_motivo`.
- Ya están en `01_schema.sql` y en `05_migracion_v1_0.sql`, que se puede ejecutar varias veces sobre una base anterior.
- Datos de demostración: 3 casos descartados con motivos realistas:
  - ruido de una obra privada;
  - un reporte de prueba;
  - un foco dentro de una casa.

**Otro arreglo:** la aplicación no tenía ícono (`/favicon.ico`), y cada página dejaba un error 404 en la consola del navegador. Ahora tiene el escudo de PiuraVigila.

## Pruebas

| Tipo | v1.1 | v1.2 | Nuevas |
|---|---|---|---|
| Unitarias | 109 | 116 | `DescartadoTest` (TC-D01 a D07) |
| Integración con PostgreSQL | 16 | 20 | `DescartadoIT` (TC-D08 a D11): la base exige el motivo, R01 excluye descartados, retención y migración desde la v1.1 |

Resultado: 0 fallos (`Evidencias/AV12-01_mvn_verify.txt`).

Prueba contra la aplicación real (`AV12-02`):
1. Las tarjetas muestran 13 / 11 / 35 / 3 / 62.
2. Descartar sin motivo se rechaza y el caso sigue «Recibido».
3. Con motivo, pasa a «Descartado» y ya no admite cambios.
4. El vecino ve «Descartado» y el motivo, en una línea de tiempo de 2 pasos.

`AV12-03`: en el navegador, al elegir «Descartado» el motivo se vuelve obligatorio, y no hay errores en la consola.

## Capturas

`Capturas/` tiene el estado final de todas las pantallas en escritorio y celular. Las nuevas son:
- `*_12_descartar_formulario`: el detalle con «Descartado» elegido;
- `*_13_consulta_descartado`: lo que ve el vecino;
- `*_06_panel`: el panel con las 5 tarjetas.
