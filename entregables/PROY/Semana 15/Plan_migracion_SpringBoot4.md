# PiuraVigila · Plan de migración a Spring Boot 4 (riesgo R1)

PiuraVigila · Curso Integrador I: Sistemas - Software · 2026-2 · 17/11/2026

## Por qué

En la reprueba de dependencias de la Semana 14 (S14-E5) quedaron dos avisos críticos sobre `spring-webmvc 6.2.19`, la versión que trae Spring Boot 3.5.16:

| CVE | Qué afecta | ¿Aplica a PiuraVigila? |
|---|---|---|
| CVE-2026-47890 | Server-Sent Events al renderizar fragmentos | No: la aplicación no usa SSE (`SseEmitter`) |
| CVE-2026-47884 | `XsltView` (rutas de hojas XSLT) | No: las vistas son Thymeleaf; no hay XSLT |

La corrección solo existe en Spring Framework 7.0.9, que llega con Spring Boot 4. Por eso no se migró en la Semana 14, y tampoco antes del despliegue del 80 %: cambiar de versión mayor justo antes de desplegar arriesgaba la versión estable sin reducir un riesgo real.

## Qué cambia con Spring Boot 4

| Área | Hoy (3.5.16) | Con Spring Boot 4 | Impacto en PiuraVigila |
|---|---|---|---|
| Spring Framework | 6.2 | 7.0 | Revisar APIs obsoletas en controladores y filtros |
| Jakarta EE | 10 (Servlet 6.0) | 11 (Servlet 6.1) | Tomcat embebido pasa a 11; `RedireccionHttps` usa API de Tomcat que se mantiene |
| Jackson | 2.x | 3.x (paquete `tools.jackson`) | Bajo: la aplicación no serializa JSON propio; solo Actuator |
| Thymeleaf | 3.1 (integración para Spring 6) | la integración que administre Boot 4 | Verificar que las 13 plantillas rendericen igual |
| Actuator | rutas actuales | se mantienen `health` e `info` | Revisar propiedades `management.*` |
| Pruebas | JUnit Jupiter, MockMvc | versiones que administre Boot 4 | Ajustar imports si cambian |
| Java | 21 | 17+ (se mantiene 21) | Ninguno |

## Pasos

1. Rama `feature/spring-boot-4` desde `main` (después de la entrega del 80 %).
2. Cambiar el parent a la última 4.0.x y retirar las versiones fijadas a mano en la Semana 14 que Boot 4 ya administra.
3. `mvn verify` con `RUN_DB_TESTS=true`: las 141 pruebas (121 unitarias y 20 de integración) son la red de seguridad.
4. Repetir la SCA (OSV y, si se tiene la clave, Dependency-Check) y confirmar que los dos CVE desaparecen.
5. Desplegar con `deploy/windows/desplegar.ps1` en la carpeta de despliegue, correr `health_check.ps1` y ZAP pasivo.
6. Si todo pasa, PR, CI en verde y tag `v1.4.0`. Si algo falla, el despliegue vuelve a `v1.3.0` con el mismo script (`-Version v1.3.0`).

## Criterio para decidir

Se migra en la Semana 16 o 17 si las pruebas pasan sin cambios grandes de código. Si la migración exige reescribir partes del panel o de las pruebas, se queda en la última 3.5.x y se documenta como riesgo aceptado: los dos CVE no tienen vía de explotación en esta aplicación. Aun así conviene no postergarla más allá del proyecto: la rama 3.5 recibe cada vez menos parches gratuitos.
