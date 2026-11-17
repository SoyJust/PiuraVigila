# Semana 11 · PiuraVigila · Control de versiones (avance 40 %, rev2 y rev3)

El código está en el repositorio https://github.com/SoyJust/PiuraVigila (público, rama `main`). Esta carpeta contiene solo documentos.

| Incremento | Rama | Pull request | Merge | Etiqueta | Commits en total |
|---|---|---|---|---|---|
| Hito 40 % | `feature/apf3-rev1` | PR #1 | `0186074` | `semana11-40pct` | 11 |
| Rev2 | `feature/apf3-rev2` | PR #2 | `d592b35` | `semana11-100pct` | 16 |
| Rev3 | `feature/apf3-rev3` | PR #3 | `9d70079` | `apf3-rev3` | 18 |

## Contenido

- `PiuraVigila_Semana_11_Control_Versiones_rev3.xlsx`: registro de commits, ramas y PR, trazabilidad requisito → commit y checklists de cada incremento (`Checklist_40`, `Checklist_Rev2`, `Checklist_Rev3`).
- `REV3_GUIA.md`: criterios de la revisión 3 y dónde se ven en el código.
- `REV3_VALIDACION.md` y `REV3_maven_test.txt`: validación de la revisión 3 y salida de `mvn clean test`.
- `PASOS_GITHUB.txt`: comandos ejecutados en cada incremento.

## Hito 40 % (23–24/10/2026)

Rama `feature/apf3-rev1`, creada desde `main`:

| Commit | Fecha | Mensaje |
|---|---|---|
| `46b7fa9` | 23/10 | fix(panel): exigir rol administrador en habilitarCategoria() |
| `3621201` | 23/10 | feat(seed): agregar usuario operador y PIURA_OPERADOR_PASSWORD |
| `c144f2e` | 23/10 | feat(panel): mapa esquemático P04 de zonas críticas |
| `ab6653b` | 23/10 | test(panel): PV-T39, PV-T41 y PV-T42 con MockMvc |
| `e106cb7` | 23/10 | docs: actualizar README y guía del arnés |
| `6ac9689` | 24/10 | fix(login): crear la sesión antes de cambiar su identificador |
| `ed70900` | 24/10 | test(login): PV-T43 inicio de sesión sin sesión previa |
| `3ce112d` | 24/10 | fix(ui): contraste de los enlaces dentro de las tarjetas |

El commit `docs` corrigió dos frases del README que no coincidían con el código: «recategorización» (no existe esa función) y que `/panel/categoria` «cambia la categoría de un caso» (habilita una categoría nueva).

Verificación:
- `mvn test`: 42 pruebas, 0 fallos, BUILD SUCCESS (24/10/2026, EV-14), incluidas las 7 de `PanelControllerTest`.
- Evidencias EV-06 a EV-14 validadas: base de datos, log, P01 a P04, hash BCrypt, control de rol y `mvn test`.
- PR #1 revisado y fusionado en `main`; etiqueta `semana11-40pct` publicada.
- Sin contraseñas ni tokens en el código; `target/`, `logs/`, `fotos/` e `.idea/` no se versionan.

## Rev2 (25/10/2026)

Cambios: mapa P04 con puntos (RF-04), línea de tiempo en Consultar (RF-02), zonas por distritos reales, datos de demostración (`database/04_datos_demo.sql`) y `config/` y `uploads/` fuera de GitHub. Rama `feature/apf3-rev2` desde `0186074`:

| Commit | Mensaje |
|---|---|
| `043d302` | chore: ignorar config/ y uploads/ |
| `32bcc2b` | feat: mapa de reportes con ubicacion, linea de tiempo en Consultar y mejoras de interfaz |
| `b64ffd2` | feat: zonas por distritos reales, datos de demostración y README actualizado |
| `b3892b5` | docs: actualizar README (zonas, datos de demo, mapa y linea de tiempo) |

PR #2 fusionado por su autor (SoyJust) el 25/10/2026 y aprobado en GitHub por `justzonagamer-rgb` el 26/10/2026. Registrado en `Ramas_PR` (G6) y `Checklist_Rev2` (fila 17).

Verificación:
- `mvn test`: 44 pruebas, 0 fallos (25/10/2026 12:08, EV-16), incluidas PV-T44 y PV-T45.
- Mapa con zonas por distrito y 15 puntos en 7 días (EV-17) y línea de tiempo en Consultar (EV-18).
- El seed solo trae `HASH_PENDIENTE`; los datos de demostración son ficticios.

## Rev3 (27/10/2026)

Cambios: librerías de apoyo (Guava, Apache POI, Commons Lang 3 y Logback), exportación de zonas a Excel, token CSRF en los formularios públicos y validación de fotos. Rama `feature/apf3-rev3` desde `d592b35`, commit `52468ca`, PR #3 (merge `9d70079`).

PR #3 revisado y fusionado por su autor; no tuvo revisión de otro integrante, y así figura en `Ramas_PR` (G7) y `Checklist_Rev3` (fila 17).

Verificación:
- `mvn clean test`: 54 pruebas, 0 fallos (27/10/2026 16:03, EV-21); segunda corrida con JDK 17: 54 pruebas, 0 fallos (EV-22).
- Las cuatro librerías están en el `pom.xml` y se usan en el código (EV-25).
- Excel descargado desde el panel con PostgreSQL (EV-27, 12/11/2026): FV-07 en Verde.
