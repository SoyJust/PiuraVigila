# PiuraVigila · Acabado v1.1 (13/11/2026)

Continuación de la v1.0. Responde a tres observaciones de la revisión:
1. Las fotos se pedían pero nadie las veía.
2. El Excel era demasiado simple.
3. Las preguntas del docente sobre spam, concurrencia y errores de usuario.

El despliegue (semana 15) sigue pendiente.

Las respuestas completas a las preguntas del docente están en **`Preguntas_del_docente_abuso_y_errores.md`**.

## Qué se encontró y se corrigió

| # | Hallazgo | Cómo se comprobó | Corrección |
|---|---|---|---|
| 1 | **5 reportes simultáneos desde el mismo teléfono se aceptaban todos**, saltándose «1 cada 60 s» | AV11-02: antes 5 de 5 aceptados; después 1 de 5 | Bloqueo por dispositivo en PostgreSQL (`pg_advisory_xact_lock`) |
| 2 | Varios vecinos que reportaban el mismo problema a la vez podían crear varios casos | AV11-03: ahora 1 caso y 4 pantallas «¿Es el mismo problema?» | Bloqueo por categoría y distrito |
| 3 | Dos operadores que cambiaban el mismo caso a la vez podían duplicar el historial | TC-C06 | Bloqueo por caso |
| 4 | Las fotos se guardaban, pero **ninguna pantalla las mostraba** | Revisión del código | Nuevo **detalle del caso** (P03b) con foto, mapa, historial y fotos de los vecinos vinculados |
| 5 | Fotos guardadas tal cual: hasta 2 MB cada una y **con metadatos EXIF** (ubicación GPS de la casa) | AV11-04: 1253 KB → 255 KB, sin EXIF | Se reducen en el celular y otra vez en el servidor (1600 px, JPEG); se borran los metadatos; se enderezan |
| 6 | La misma foto podía reenviarse muchas veces | TC-F04, AV11-04 | Se rechaza el mismo archivo exacto durante 24 h |
| 7 | Las fotos se acumulaban para siempre | TC-F06, TC-I05 | Retención: se borran 365 días después de atendido el caso (configurable) |
| 8 | El login no tenía límite de intentos (fuerza bruta) | TC-A01, TC-A02 | 5 fallos por usuario o 20 por IP en 15 min bloquean temporalmente |
| 9 | Quien borraba la cookie quedaba sin límite de envíos | TC-A03 | Límite por IP: 20 envíos cada 10 min |
| 10 | Se podían probar códigos uno por uno en la consulta pública | TC-A07 | Límite por IP: 60 consultas cada 10 min |
| 11 | Se aceptaban ubicaciones de cualquier parte del mundo | TC-C07 | La ubicación debe estar dentro de la provincia de Piura |
| 12 | Doble clic en «Enviar» | Navegador | El botón se desactiva al enviar (el servidor igual lo controla) |
| 13 | El Excel solo tenía el conteo por distrito, con fechas en formato técnico | TC-X01 a X04, AV11-07 | Libro con 4 hojas (abajo) |

## Funciones nuevas

- **Detalle del caso** (`/panel/caso?codigo=…`, se abre al tocar el código en el panel o «Ver caso» en el mapa):
  - foto del vecino, descripción completa y mapa con el punto;
  - historial con quién hizo cada cambio y su comentario;
  - reportes de otros vecinos unidos al caso, con sus fotos;
  - formulario para cambiar el estado con un comentario.
- **Comentario para el vecino** al cambiar de estado (opcional, máximo 200 caracteres). El vecino lo ve en su consulta bajo ese paso; **nunca** ve quién lo escribió.
- **Fotos solo para el personal:** `/panel/foto/{id}` exige sesión; no hay ruta pública a la carpeta de fotos.
- **Excel de gestión** (`/panel/exportar`), con fechas reales, filtros, encabezado fijo y sin fotos ni datos personales:
  - **Resumen:** indicadores R03 y porcentaje de atendidos.
  - **Casos:** cada caso con código, fecha, tipo, distrito, estado, vecinos vinculados, última actualización, días hasta atender o días abierto, y descripción.
  - **Distritos:** R01 con total y gráfico de barras.
  - **Tiempos de atención:** R02, solo si lo descarga el administrador.
- **Vista previa de la foto** en el formulario, con el peso antes y después de reducirla.

## Base de datos

- `historial_estado.comentario VARCHAR(200)` en `01_schema.sql` y en `05_migracion_v1_0.sql`, que se puede ejecutar dos veces.
- `04_datos_demo.sql`: los cambios de estado traen comentarios de ejemplo.
- `database/generar_fotos_demo.py` (opcional, requiere Pillow) crea imágenes ilustrativas marcadas como «FOTO DE DEMOSTRACIÓN» para los 72 reportes de demostración. No son fotos reales.

## Pruebas

| Tipo | v1.0 | v1.1 | Nuevas |
|---|---|---|---|
| Unitarias (`mvn test`) | 83 | 109 | `FotosYLimitesTest` (TC-F01 a F07, L01 y L02), `ServicioV11Test` (TC-C01 a C07), `AbusoYCasoControllerTest` (TC-A01 a A07), `ExportacionServiceTest` reescrita (TC-X01 a X04) |
| Integración con PostgreSQL (`RUN_DB_TESTS=true mvn verify`) | 11 | 16 | `ConcurrenciaYConsultasV11IT` (TC-I01 a I05: el bloqueo realmente hace esperar a la segunda transacción) |

Resultado: 0 fallos (`Evidencias/AV11-01_mvn_verify.txt`).

## Evidencias

| Archivo | Qué muestra |
|---|---|
| `AV11-01_mvn_verify.txt` | Resultado de todas las pruebas |
| `AV11-02_envios_simultaneos_mismo_telefono.txt` | 5 envíos a la vez desde un teléfono: antes 5 aceptados, después 1 |
| `AV11-03_vecinos_simultaneos_mismo_problema.txt` | 5 vecinos a la vez con el mismo problema: 1 caso y 4 duplicados |
| `AV11-04_foto_grande_reducida_sin_metadatos.txt` | Foto de 4000 × 3000 con EXIF: se guarda de 1600 × 1200, un 80 % más liviana y sin metadatos; el reenvío se rechaza |
| `AV11-05_e2e_detalle_comentario_excel.txt` | Detalle, fotos (401 sin sesión, 404 si no existe), comentario visible al vecino y Excel |
| `AV11-06_reduccion_en_el_navegador.txt` | Reducción en el navegador: 0.71 MB → 0.13 MB, sin violaciones de CSP |
| `AV11-07_PiuraVigila_Reporte_ejemplo.xlsx` | Excel de ejemplo con los datos de demostración (versión del administrador) |

`Capturas/` contiene el estado final de todas las pantallas en escritorio y celular, incluido el nuevo `*_11_detalle_caso`. Todos los datos son ficticios, y la cuenta «Lucía Ramírez» se creó solo para tomar las capturas.

## Pendiente

Ver la sección 7 de `Preguntas_del_docente_abuso_y_errores.md`:
- un estado «Descartado» para reportes falsos, que hay que consultar con el docente;
- un CAPTCHA real al desplegar;
- límites compartidos si algún día hay varios servidores.
