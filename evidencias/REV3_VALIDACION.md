# Validación de revisión 3 — 27/10/2026

Resultado real: 54 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas. BUILD SUCCESS.

Entorno: OpenJDK 17.0.20, Maven 3.9.11. Se ejecutó Maven con `-Djava.version=17` para adecuarse al JDK disponible en este entorno. El pom entregado conserva Java 21. Se resolvieron las dependencias reales de Maven; no se utilizó el arnés de sustitución.

El primer intento encontró restricciones del mecanismo de auto-adjunción de Mockito. Se configuró `mock-maker-subclass` en src/test/resources; las pruebas usan clases no finales y no requieren instrumentación de métodos finales. Después se corrigió el resolvedor de vistas de los tests MockMvc y la suite completa pasó.

Cobertura añadida: Excel válido con números y cadenas sin interpretación como fórmula; acceso a exportación con/sin sesión y periodo inválido; CSRF ausente, válido y de otra sesión; rechazo de texto disfrazado de foto y discrepancia extensión/contenido. Las pruebas de guardado existentes usan ahora imágenes reales.

No ejecutado: servidor completo con PostgreSQL, pruebas de integración/rollback, navegador, prueba con JDK 21 y GitHub Actions. La prueba de controlador verifica respuestas con MockMvc, no el renderizado visual de Thymeleaf. La captura histórica de 44 tests sigue siendo rev2.

Los 54 tests no acreditan 100% de cobertura ni TDD. Para entregar, ejecutar `mvn clean verify` con JDK 21 y los pasos de demostración de REV3_GUIA.md. No se ha publicado esta revisión en GitHub.
