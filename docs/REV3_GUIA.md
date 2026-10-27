# PiuraVigila — revisión 3

## Qué cambió y dónde demostrarlo

| Criterio | Implementación | Demostración |
|---|---|---|
| MVC | Vistas Thymeleaf, controladores, modelos y servicios separados | Seguir un registro desde reportar.html hasta ReporteController y ReporteService |
| DAO | Contratos dao/ e implementaciones dao/jdbc/ | Explicar cómo se ejecutan consultas parametrizadas en PostgreSQL |
| SOLID | FotoStorage como contrato; servicios con dependencias por constructor; exportación en un servicio separado | Explicar SRP y DIP. No afirmar cumplimiento completo de los cinco principios |
| Guava | SHA-256 en FotoStorageDisco | Registrar una foto y revisar su huella en el log |
| Apache POI | ExportacionService y ExportacionController | Descargar Excel desde el panel y contrastar sus conteos con el resumen del mismo periodo |
| Apache Commons Lang | StringUtils.isBlank en ValidadorReporte | Rechazar una descripción vacía o con espacios |
| Logback | Dependencia explícita y SLF4J | Mostrar logs/piuravigila.log sin publicar datos sensibles |
| Seguridad | BCrypt, roles, CSRF, validación de fotos y coordenadas | Probar sesión, acceso y archivo falso |

## Prueba manual antes de entregar

1. JDK 21 y Maven: `mvn clean test`. Conservar el resultado real.
2. Configurar PostgreSQL y variables como indica README. Ejecutar `mvn spring-boot:run`.
3. Registrar con JPG/PNG real, consultar código y comprobar el historial.
4. Intentar subir texto renombrado a .png. Debe rechazarse sin guardar el archivo.
5. Quitar el campo _csrf con las herramientas del navegador y enviar el formulario. Debe devolver 403. Recargar para volver a probar correctamente.
6. Repetir el flujo de duplicados con el token presente. El token ausente debe devolver 403.
7. Acceder como operador y administrador. Confirmar que solo administrador habilita categorías.
8. Descargar Excel. Comparar zonas y cantidades con el resumen del mismo periodo. Un acceso sin sesión a /panel/exportar debe devolver 401.
9. Confirmar registro en PostgreSQL y eventos de foto/exportación en logs.

Capturas propuestas (pendientes de generar en el equipo): REV3-01 pruebas, REV3-02 exportación, REV3-03 rechazo de foto, REV3-04 CSRF, REV3-05 GitHub. No están incluidas ni se declaran realizadas.

## GitHub: conservar el historial real

Trabajar dentro del clon existente de https://github.com/SoyJust/PiuraVigila. No borrar .git ni ejecutar git init para sustituir su historial.

1. Crear rama: `git switch -c feature/apf3-rev3`.
2. Copiar el contenido actualizado de PiuraVigila sobre el clon. Añadir los entregables APF3 a una carpeta `entregables/APF3`.
3. Revisar `git status` y `git diff`. No incluir contraseñas, config/, uploads/, fotos/, logs/ ni target/.
4. Ejecutar pruebas y registrar evidencia auténtica.
5. Seleccionar cambios: `git add pom.xml src .github README.md docs evidencias/REV3_VALIDACION.md entregables/APF3`.
6. Crear commit que describa lo realizado: `git commit -m "feat: exportacion Excel, librerias y validaciones de seguridad"`.
7. Subir: `git push -u origin feature/apf3-rev3`.
8. Abrir PR en GitHub, solicitar revisión real antes de fusionar y conservar enlace y hash reales.

No se incluyen commits inventados ni se ha subido esta revisión a GitHub. Las matrices Excel y diagramas de semanas 9–11 se conservan como línea base rev2. Este anexo documenta las adiciones; si el profesor exige una matriz única, consolidar estos componentes y evidencias en ella después de la demostración.

## Sustentación sugerida

Explicar primero el problema urbano y el alcance, después la arquitectura. Seguir un reporte a través de las capas. Mostrar registro, consulta, panel y mapa. Descargar el Excel y explicar cada librería señalando su uso en el código. Terminar con seguridad, pruebas y el historial real de GitHub.

Distinguir autoría y asistencia: hubo asistencia para esta revisión. Debes comprender, probar y poder explicar cada modificación conforme a las reglas del curso. No afirmar que los tests se escribieron antes de implementar: no se acredita un ciclo TDD histórico.

## Pendientes que no se ocultan

- Ejecución con JDK 21 y PostgreSQL real, demostración visual de los flujos y publicación de esta revisión en GitHub.
- Pruebas de integración/rollback real pendientes en rev2; esta revisión no las sustituye por mocks.
- La huella SHA-256 acredita igualdad de bytes si se compara; no es cifrado.
- Anti-spam por cookie, posible foto huérfana al fallar la transacción y límite del código de seguimiento permanecen según README.

## Automatización de pruebas
Se incluye `.github/workflows/pruebas.yml`: al subir el código, GitHub Actions ejecutará `mvn clean verify` con Java 21 y conservará los informes JUnit. Su ejecución está pendiente de subir esta revisión; el archivo por sí solo no acredita un resultado exitoso.
