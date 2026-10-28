# Validación de revisión 3 — 27/10/2026

## Corrida en la laptop del equipo (EV-21)
`mvn clean test`, 27/10/2026 16:03: 54 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas. BUILD SUCCESS. Salida guardada en Semana 10/Capturas/EV-21_mvn_test_54.txt. El pom exige Java 21.

## Segunda corrida independiente (EV-22)
Realizada al preparar la revisión, el mismo día. Entorno: OpenJDK 17.0.20, Maven 3.9.11, con `-Djava.version=17` para adecuarse al JDK disponible; el pom entregado conserva Java 21. Resultado: 54 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS. Se resolvieron las dependencias reales de Maven; no se utilizó el arnés de sustitución. Salida completa en `REV3_maven_test.txt`.

El primer intento encontró restricciones del mecanismo de auto-adjunción de Mockito. Se configuró `mock-maker-subclass` en src/test/resources; las pruebas usan clases no finales y no requieren instrumentación de métodos finales. Después se corrigió el resolvedor de vistas de los tests MockMvc y la suite completa pasó.

## Cobertura añadida (PV-T46 a PV-T55)
Excel válido con números y cadenas sin interpretación como fórmula; acceso a exportación con y sin sesión y periodo inválido; CSRF ausente, válido y de otra sesión; rechazo de texto disfrazado de foto y discrepancia entre extensión y contenido. Las pruebas de guardado existentes usan ahora imágenes reales. Total: 40 pruebas de service y 14 de controlador.

## Publicación
Rama `feature/apf3-rev3`, commit `52468ca`, pull request #3 fusionado (merge `9d70079`) y etiqueta `apf3-rev3`. La automatización `.github/workflows/pruebas.yml` está en el repositorio; su resultado se consulta en la pestaña Actions de GitHub.

## No ejecutado o pendiente
Servidor completo con PostgreSQL en el navegador con capturas de cada flujo, pruebas de integración y rollback real, y revisión del PR #3 por otro integrante (lo revisó su propio autor). La prueba de controlador verifica respuestas con MockMvc, no el renderizado visual de Thymeleaf. Las capturas históricas de 41, 42 y 44 pruebas son de revisiones anteriores y se conservan como historia.

Los 54 tests no acreditan una cobertura del 100 % ni TDD. Esta revisión se valida con `mvn clean test` y los pasos de demostración de REV3_GUIA.md.
