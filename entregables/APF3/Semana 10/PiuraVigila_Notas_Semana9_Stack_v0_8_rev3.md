# APF3 · Semana 10 · rev3: qué cambió respecto a APF2 y estado de cierre

Línea base: APF2 (diseño validado, sin cambios) y matriz de arquitectura Semana 9 rev4 (46 componentes, C-01 a C-46). Stack: Spring Boot 3.5.15 (pom), Java 21, PostgreSQL.

## Qué cambió respecto a APF2 (rev1, 23 y 24/10/2026)
- **Arquitectura**: View (Thymeleaf) → Controller → Service → DAO (JdbcTemplate) → PostgreSQL, con los nombres de la Semana 9. El diseño de BD de APF2 no se tocó.
- **Seed**: se agregó el usuario `operador` (hash pendiente, igual que `admin`). `AdminInicializador` guarda el hash de cada usuario desde `PIURA_ADMIN_PASSWORD` y `PIURA_OPERADOR_PASSWORD`.
- **Control de acceso (Q-10)**: `PanelController.habilitarCategoria()` exige rol administrador y el formulario «Nueva categoría» solo se muestra al administrador.
- **RF-04**: mapa P04 (`GET /panel/mapa`, `mapa.html`) con el conteo por zona y periodo (7 días, mes, 3 meses, año), a partir de `DashboardService.zonasCriticas()`; lo ven operador y administrador. En rev1 era una cuadrícula esquemática; en rev2 se añadieron los puntos (ver abajo).
- **Login**: el inicio de sesión ya no falla cuando no existe una sesión previa (hallado al probar con el operador).
- **Pruebas**: `PanelControllerTest` (MockMvc) con PV-T39, PV-T41, PV-T42 y PV-T43. La matriz TDD usa los IDs T-01 a T-24 de la Semana 9 y conserva PV-T25 a PV-T55 como adicionales.

## Qué se añadió en rev2 (25/10/2026, PR #2)
- **Mapa P04 con puntos (RF-04)**: además de la cuadrícula por zona, cada reporte con ubicación se dibuja como un punto en un mapa de Leaflet con OpenStreetMap, coloreado por estado. Usa `DashboardService.puntosReportes()` → `ReporteDAO.puntosPeriodo()` y el DTO `PuntoReporte`. Un reporte vinculado muestra el código y el estado de su caso original. El mapa necesita internet para los mosaicos.
- **Consultar estado con línea de tiempo (RF-02)**: `ReporteService.consultarEstado()` ahora lee `HistorialEstadoDAO.listarPorReporte()` y entrega los tres pasos (Recibido → En proceso → Atendido) con su fecha; los pasos no alcanzados salen como «Pendiente». Nuevo DTO `PasoEstado`; `ConsultaEstado` lleva la lista de pasos. No se muestra quién hizo el cambio. Pruebas PV-T44 y PV-T45.
- **Zonas por distritos reales**: Piura, Castilla, Veintiséis de Octubre, Catacaos, La Arena y La Unión (antes: Centro, Norte, Sur, Este, Oeste y Castilla).
- **Datos de demostración**: `database/04_datos_demo.sql` (20 casos y 8 reportes vinculados ficticios; se reconocen por `dispositivo_id` «demo-…» y se quitan con los DELETE del final del script).
- **Seguridad del repositorio**: `config/` (claves locales de la base) y `uploads/` se agregaron al `.gitignore` (commit 043d302).
- **Diagramas**: arquitectura rev3 (con 10 DTO, `PuntoReporte` y `PasoEstado` incluidos) y trazabilidad secuencial rev2 regenerada. La jerarquía de componentes no cambió.
- **Integración**: PR #2 `feature/apf3-rev2` → `main`, merge d592b35, etiqueta `semana11-100pct` (16 commits en total).

## Qué se añadió en rev3 (27/10/2026, PR #3)
- **Librerías de apoyo**: Guava (huella SHA-256 de cada foto, en `FotoStorageDisco`), Apache POI (Excel de zonas, en `ExportacionService`), Apache Commons Lang 3 (`StringUtils.isBlank`, en `ValidadorReporte`) y Logback como dependencia explícita (registro en `logs/piuravigila.log`). Están declaradas en el `pom.xml`.
- **Exportar zonas a Excel (RF-04)**: nuevo flujo FV-07. `ExportacionController` (`GET /panel/exportar`) exige sesión municipal (401 si no hay), valida el periodo y devuelve el `.xlsx`; `ExportacionService` lo genera con Apache POI. El archivo solo lleva conteos por zona (sin datos personales ni fotos) y los nombres de zona se escriben como texto.
- **Seguridad**: el token CSRF ahora también protege los formularios públicos (registro y respuesta al duplicado: 403 si falta o es de otra sesión). La foto se decodifica antes de guardarse: solo JPG o PNG, el contenido debe coincidir con la extensión y se limita a 20 megapíxeles.
- **Pruebas**: 10 nuevas, PV-T46 a PV-T55 (`FotoStorageDiscoTest`, `MejorasSeguridadTest`, `ExportacionServiceTest`). Total: 54 (40 de service y 14 de controlador).
- **Automatización**: `.github/workflows/pruebas.yml` ejecuta `mvn clean verify` en cada push y pull request; el resultado de cada corrida está en la pestaña Actions de GitHub.
- **Documentos**: matriz de arquitectura rev4 (C-45 y C-46, Q-21 y Q-22), matriz de trazabilidad rev3 (FV-07, TR-06, PV-T46 a PV-T55, EV-21 a EV-25), diagrama de arquitectura rev4 y trazabilidad secuencial rev3.
- **Integración**: PR #3 `feature/apf3-rev3` → `main`, commit 52468ca, merge 9d70079, etiqueta `apf3-rev3` (18 commits en total).

## Qué ya se ejecutó (con evidencia en la carpeta Capturas)
- `mvn test` real el 23/10/2026: 41 pruebas, 0 fallos, BUILD SUCCESS (EV-03).
- `mvn test` el 24/10/2026: 42 pruebas (35 de service y 7 de `PanelControllerTest`, con PV-T43), 0 fallos (captura EV-14).
- `mvn test` final el 25/10/2026 12:08: 44 pruebas (37 de service y 7 de `PanelControllerTest`, con PV-T44 y PV-T45), 0 fallos, BUILD SUCCESS (captura EV-16).
- Aplicación con PostgreSQL real: registro de un reporte (PR-2026-0001), consulta, panel de administrador y de operador, mapa P04, fila en `reporte` y hashes BCrypt de `admin` y `operador` (EV-07, EV-09 a EV-12).
- Rev2 en pantalla: mapa con zonas por distrito y 15 puntos en el periodo de 7 días (EV-17) y consulta con línea de tiempo (EV-18), con los datos de demostración cargados (EV-20).
- `mvn clean test` de la revisión 3 en la laptop del equipo el 27/10/2026 16:03: 54 pruebas (40 de service y 14 de controlador), 0 fallos, BUILD SUCCESS (EV-21). Segunda corrida independiente con JDK 17 al preparar la revisión: 54 pruebas, 0 fallos (EV-22).
- Flujos en Verde en la matriz: FV-01 a FV-06. FV-07 (exportación) queda en Amarillo hasta tener la captura del archivo descargado desde el panel. Evidencias EV-06 a EV-25 Validadas.

## Alcance posterior y estado de cierre
- Las capturas EV-06 a EV-15 son del 24/10/2026 (versión anterior a rev2, con las zonas previas y la cuadrícula esquemática) y se conservan como evidencia histórica; EV-16 a EV-25 documentan el estado vigente (EV-21 a EV-25 son de rev3).
- Fuera de este avance: pruebas de integración con PostgreSQL (T-09, T-10, T-19 a T-21; PV-T36, PV-T37 y PV-T40) y rollback real (T-23), que permanecen «Por diseñar». El mapa con puntos tampoco tiene prueba automática propia.
- SOLID: ISP y LSP quedan «Parcial» (ReporteDAO con 16 métodos, porque `puntosPeriodo()` se añadió como método `default`; DAO JDBC sin pruebas de integración automáticas).
- Los flujos FV-02 (duplicado) y FV-03 (anti-spam) están en Verde con la evidencia completa.
- Revisión: el PR #2 lo fusionó su autor (SoyJust) y fue revisado y aprobado en GitHub por `justzonagamer-rgb` el 26/10/2026 (posterior a la fusión); queda registrado en `Ramas_PR` y en `Checklist_Rev2` de la Semana 11.
- Decisión sobre la versión: el pom usa Spring Boot 3.5.15; la última versión gratuita de la rama 3.5 es 3.5.16, y esa rama salió del soporte abierto en junio de 2026. Spring Boot 4.1.0 salió el 10/06/2026. Decidir entre subir a 3.5.16 o migrar a 4.x.
- Revisión del PR #3: lo revisó y fusionó su autor (SoyJust); no hay revisión de otro integrante, y así queda registrado en `Ramas_PR` y en `Checklist_Rev3` de la Semana 11.
- Honestidad sobre las pruebas: los 54 tests no acreditan una cobertura del 100 % ni un ciclo TDD histórico (las pruebas de rev3 se escribieron junto al código). Tampoco hay todavía pruebas de integración con PostgreSQL.
