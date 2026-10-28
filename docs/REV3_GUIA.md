# PiuraVigila — revisión 3

## Qué cambió y dónde demostrarlo

| Criterio | Implementación | Demostración |
|---|---|---|
| MVC | Vistas Thymeleaf, controladores, modelos y servicios separados | Seguir un registro desde reportar.html hasta ReporteController y ReporteService |
| DAO | Contratos dao/ e implementaciones dao/jdbc/ | Explicar cómo se ejecutan consultas parametrizadas en PostgreSQL |
| SOLID | FotoStorage como contrato; servicios con dependencias por constructor; exportación en un servicio separado | Explicar SRP y DIP. No afirmar cumplimiento completo de los cinco principios (ISP y LSP figuran como «Parcial») |
| Guava | SHA-256 en FotoStorageDisco | Registrar una foto y revisar su huella en el log |
| Apache POI | ExportacionService y ExportacionController | Descargar Excel desde el panel y contrastar sus conteos con el resumen del mismo periodo |
| Apache Commons Lang | StringUtils.isBlank en ValidadorReporte | Rechazar una descripción vacía o con espacios |
| Logback | Dependencia explícita y SLF4J | Mostrar logs/piuravigila.log sin publicar datos sensibles |
| Seguridad | BCrypt, roles, CSRF, validación de fotos y coordenadas | Probar sesión, acceso y archivo falso |

## Prueba manual para la demostración

1. `mvn clean test`: hecho el 27/10/2026, 54 pruebas, 0 fallos (EV-21).
2. Configurar PostgreSQL y variables como indica el README. Ejecutar `mvn spring-boot:run`.
3. Registrar con JPG/PNG real, consultar el código y comprobar la línea de tiempo.
4. Intentar subir texto renombrado a .png. Debe rechazarse sin guardar el archivo.
5. Quitar el campo _csrf con las herramientas del navegador y enviar el formulario. Debe devolver 403. Recargar para volver a probar correctamente.
6. Repetir el flujo de duplicados con el token presente. El token ausente debe devolver 403.
7. Acceder como operador y administrador. Confirmar que solo el administrador habilita categorías.
8. Descargar el Excel. Comparar zonas y cantidades con el resumen del mismo periodo. Un acceso sin sesión a /panel/exportar debe devolver 401.
9. Confirmar el registro en PostgreSQL y los eventos de foto y exportación en logs.

Capturas ya disponibles: EV-21 (salida de `mvn clean test`, 54 pruebas) y EV-23 (PR #3 en GitHub). Capturas que se pueden tomar para completar la evidencia: el Excel descargado desde el panel (pasa FV-07 de Amarillo a Verde), el rechazo de una foto falsa y la respuesta 403 sin token. No se declaran realizadas hasta que existan.

## GitHub: historial real de la revisión 3

Se trabajó dentro del clon existente de https://github.com/SoyJust/PiuraVigila, sin borrar `.git` ni ejecutar `git init`.

- Rama `feature/apf3-rev3`, commit `52468ca` («feat: exportacion Excel, librerias (Guava, POI, Commons, Logback) y validaciones de seguridad»).
- Pull request #3, fusionado con el merge `9d70079`. Etiqueta `apf3-rev3`. En total hay 18 commits en el historial documentado.
- `config/`, `uploads/`, `fotos/`, `logs/` y `target/` no se versionan.
- El PR #3 lo revisó y fusionó su autor; no hay revisión de otro integrante.
- Los entregables APF3 se agregaron al repositorio en `entregables/APF3` con un segundo PR pequeño (`feature/apf3-entregables`), que también actualiza esta guía y `evidencias/REV3_VALIDACION.md`.

Las matrices Excel y los diagramas de las semanas 9 a 11 ya están consolidados con esta revisión (arquitectura rev4, trazabilidad rev3, control de versiones rev3).

## Sustentación sugerida

Explicar primero el problema urbano y el alcance, después la arquitectura. Seguir un reporte a través de las capas. Mostrar registro, consulta, panel y mapa. Descargar el Excel y explicar cada librería señalando su uso en el código. Terminar con seguridad, pruebas y el historial real de GitHub.

Distinguir autoría y asistencia: hubo asistencia para esta revisión. Hay que comprender, probar y poder explicar cada modificación conforme a las reglas del curso. No afirmar que los tests se escribieron antes de implementar: no se acredita un ciclo TDD histórico (lo que sí existe es el diseño de las pruebas en la Semana 9).

## Pendientes que no se ocultan

- Demostración visual de los flujos con PostgreSQL real en el navegador y captura del Excel descargado.
- Pruebas de integración y rollback real: siguen «Por diseñar» desde rev2; esta revisión no las sustituye por mocks.
- La huella SHA-256 acredita igualdad de bytes si se compara; no es cifrado.
- Anti-spam por cookie, posible foto huérfana al fallar la transacción y límite del código de seguimiento permanecen según el README.

## Automatización de pruebas

El archivo `.github/workflows/pruebas.yml` ejecuta `mvn clean verify` con Java 21 en cada push y pull request y conserva los informes JUnit. El resultado de cada corrida se ve en la pestaña Actions de GitHub; el archivo por sí solo no acredita un resultado exitoso.
