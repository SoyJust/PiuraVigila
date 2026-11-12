# Semana 13 · Pruebas de software y avance al 60 %

PiuraVigila · Curso Integrador I: Sistemas - Software · 2026-2 · revisión del 12/11/2026

## Línea base y versión probada

| Elemento | Valor |
|---|---|
| Línea base corregida (Semana 12) | etiqueta `v0.9-R1` (commit `bbba9a9`): APF1 (PR #5), APF2 (PR #6) y verificación APF3 (PR #7) |
| Versión probada | commit `c8acd86` en la rama `feature/s13-pruebas` |
| Ruta tecnológica | Ruta A · DAO/JDBC/PostgreSQL (Service con DAO simulados; DAO JDBC con PostgreSQL real) |
| CI | GitHub Actions run [43289407099](https://github.com/SoyJust/PiuraVigila/actions/runs/43289407099): 62 unitarias + 6 de integración, 0 fallos |

## Qué se agregó en el código

- `ReporteDAOJdbcIT`: 6 pruebas de integración contra PostgreSQL (PV-T36, PV-T37, PV-T38, PV-T40, PV-T56 y PV-T57). Cubren el código por secuencia, 100 envíos simultáneos sin repetir código (RNF-03), el caso abierto, la paginación, las consultas R01-R03 y las restricciones de la BD. Eran las pruebas «Por diseñar» de APF3.
- `ReporteControllerTest`: 7 pruebas del flujo P01/P02. JaCoCo mostraba solo 26,5 % de ramas cubiertas en `ReporteController`, el flujo público de mayor riesgo; con estas pruebas el paquete `controller` pasó de 42,2 % a 65,6 % de ramas.
- `pom.xml`: Failsafe (pruebas `*IT` en `mvn verify`) y JaCoCo 0.8.15 (reportes `target/site/jacoco` y `target/site/jacoco-it`).
- CI: servicio PostgreSQL 16, `setup-java@v5` y artefacto `resultados-pruebas` con los reportes.

## Cómo repetir las pruebas

```powershell
mvn test                                   # 62 unitarias + reporte JaCoCo (target/site/jacoco/index.html)
psql -U postgres -c "CREATE DATABASE piuravigila_test ENCODING 'UTF8' TEMPLATE template0;"
$env:RUN_DB_TESTS="true"; $env:PIURA_TEST_DB_PASSWORD="<contraseña>"
mvn verify                                 # + 6 de integración (target/site/jacoco-it/index.html)
```

La base de prueba debe terminar en `_test`: cada prueba borra su esquema y lo vuelve a crear con `database/01_schema.sql` y `02_seed.sql`. Nunca apuntar estas pruebas a la base `piuravigila`.

## Archivos

| Archivo | Contenido |
|---|---|
| `PiuraVigila_Semana13_Pruebas_Avance_60.xlsx` | Plantilla de la semana: 18 casos, 20 ejecuciones, 3 defectos, trazabilidad de 9 requisitos, cobertura y checklist del 60 % |
| `Evidencias/S13-E1_mvn_verify_local.txt` | Salida local de `mvn verify` (commit `c8acd86`) |
| `Evidencias/S13-E2_CI_GitHub_run_43289407099.txt` | Salida del CI con PostgreSQL |
| `Evidencias/S13-E3_jacoco_unitarias/index.html` | Reporte JaCoCo de las pruebas unitarias |
| `Evidencias/S13-E4_jacoco_integracion/index.html` | Reporte JaCoCo de las pruebas de integración |
| `Evidencias/S13-E5_FAIL_TC-006_y_reprueba.txt` | FAIL de TC-006 (DEF-002), análisis y reprueba |
| `entregables/APF3/Semana 10/Capturas/EV-26`, `EV-27` | Ejecución funcional: panel con nombres (DEF-001) y descarga del Excel |

## Defectos

| ID | Origen | Severidad | Estado |
|---|---|---|---|
| DEF-001 | TC-017: la tabla del panel mostraba IDs de categoría y zona | Media | Cerrado (PR #5, prueba de regresión) |
| DEF-002 | TC-006: la prueba no veía la redirección porque su resolver de vistas recibía «redirect:…» | Baja (prueba) | Cerrado (sin cambiar producción) |
| DEF-003 | APF3: HTTP 500 al registrar sin variables de entorno | Alta | Cerrado |

## Equivalencias de nombres

Los nombres de `ReporteControllerTest` usan TC-P01-xx y TC-P02-01; en la plantilla equivalen a TC-001 (P01-01), TC-005 (P01-02), TC-006 (P01-03 y P01-04), TC-004/TC-003 (P01-05), TC-010 (P01-06, cookie) y TC-008 (P02-01).

## Lectura de la cobertura

La cobertura se usó para encontrar huecos, no como meta. Resultado: se priorizó `ReporteController` por riesgo. Quedan sin prueba propia `AdminInicializador`, `BCryptHash` y los DAO JDBC de categoría, zona y usuario. Para la Semana 14 se recomienda probar el DAO de usuario (login) junto con las pruebas de seguridad.
