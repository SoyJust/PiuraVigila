# PiuraVigila — revisión 3

## Criterios y dónde se ven

| Criterio | Implementación | Dónde se ve |
|---|---|---|
| MVC | Vistas Thymeleaf, controladores, modelos y servicios separados | Un registro recorre `reportar.html` → `ReporteController` → `ReporteService` |
| DAO | Contratos `dao/` e implementaciones `dao/jdbc/` | Consultas parametrizadas con `JdbcTemplate` sobre PostgreSQL |
| SOLID | `FotoStorage` como contrato; servicios con dependencias por constructor; exportación en un servicio aparte | SRP y DIP aplicados; ISP y LSP figuran como «Parcial» en la matriz |
| Guava | SHA-256 en `FotoStorageDisco` | Huella de cada foto registrada en el log |
| Apache POI | `ExportacionService` y `ExportacionController` | Excel descargado desde el panel, con los mismos conteos que el resumen del periodo |
| Apache Commons Lang | `StringUtils.isBlank` en `ValidadorReporte` | Descripción vacía o con solo espacios rechazada |
| Logback | Dependencia explícita y SLF4J | `logs/piuravigila.log` sin datos sensibles |
| Seguridad | BCrypt, roles, CSRF, validación de fotos y coordenadas | Sesión, control de acceso y rechazo de archivo falso |

## Prueba manual

1. `mvn clean test`: 27/10/2026, 54 pruebas, 0 fallos (EV-21).
2. Con PostgreSQL y las variables del README, `mvn spring-boot:run`.
3. Registro con JPG/PNG real, consulta del código y línea de tiempo.
4. Texto renombrado a .png: se rechaza sin guardar el archivo.
5. Formulario enviado sin el campo `_csrf`: HTTP 403.
6. Flujo de duplicados: con token funciona; sin token, HTTP 403.
7. Operador y administrador: solo el administrador habilita categorías.
8. Excel descargado: zonas y cantidades iguales al resumen del mismo periodo. `/panel/exportar` sin sesión: HTTP 401.
9. Registro en PostgreSQL y eventos de foto y exportación en el log.

Evidencias: EV-21 (`mvn clean test`, 54 pruebas), EV-23 (PR #3 en GitHub) y EV-27 (Excel descargado desde el panel, 12/11/2026).

## Historial en GitHub

Repositorio https://github.com/SoyJust/PiuraVigila.

- Rama `feature/apf3-rev3`, commit `52468ca` («feat: exportacion Excel, librerias (Guava, POI, Commons, Logback) y validaciones de seguridad»).
- Pull request #3, fusionado con el merge `9d70079`. Etiqueta `apf3-rev3`. 18 commits en total.
- `config/`, `uploads/`, `fotos/`, `logs/` y `target/` no se versionan.
- El PR #3 lo revisó y fusionó su autor.
- Los entregables APF3 se agregaron en `entregables/APF3` con el PR `feature/apf3-entregables`.

Las matrices y diagramas de las semanas 9 a 11 corresponden a esta revisión (arquitectura rev4, trazabilidad rev3, control de versiones rev3).

## Limitaciones conocidas en esta revisión

- Pruebas de integración contra PostgreSQL y rollback real: «Por diseñar» (se implementaron en la Semana 13).
- La huella SHA-256 permite comparar archivos; no es cifrado.
- Anti-spam por cookie, posible foto huérfana si falla la transacción y límite de 4 dígitos del código de seguimiento, según el README.

## Automatización de pruebas

`.github/workflows/pruebas.yml` ejecuta `mvn clean verify` con Java 21 en cada push y pull request y conserva los informes JUnit. El resultado de cada corrida está en la pestaña Actions de GitHub.
