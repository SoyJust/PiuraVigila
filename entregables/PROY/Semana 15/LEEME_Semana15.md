# Semana 15 · Despliegue con Maven y avance al 80 %

PiuraVigila · Curso Integrador I: Sistemas - Software · 2026-2 · 17/11/2026

## Versión desplegada

| Elemento | Valor |
|---|---|
| Versión | `1.3.0`, etiqueta `v1.3.0` = commit `91384a2` (merge del PR #16) |
| Ruta | A · JAR ejecutable de Spring Boot (Tomcat 10.1.61 embebido) |
| Artefacto | `piuravigila-1.3.0.jar`, 50 276 378 bytes, SHA-256 `60C4C2DC…806B14A3` |
| Entorno | PC del grupo, Windows 11, JDK 21.0.13, PostgreSQL 18.6 |
| URL | `https://localhost:8443/` (HTTP 8080 solo redirige a HTTPS) |
| Base de datos | `piuravigila_prod`, rol `piuravigila_app` sin permisos de DDL |
| CI | PR, `main` y tag en verde (S15-E10) |

## Cadena de despliegue

`v1.3.0` → `git clone --depth 1` en carpeta limpia → `mvn -B clean package` (121 pruebas) → `piuravigila-1.3.0.jar` → `C:\PiuraVigila-deploy\app` → `java -jar --spring.profiles.active=prod` → `/actuator/health`, `/actuator/health/db`, `/actuator/info` → flujo P01/P02 → fila en PostgreSQL → `logs\piuravigila.log` → evidencias.

Los scripts están en `deploy/`:

```powershell
$env:PGPASSWORD = "<clave de postgres>"; .\deploy\windows\preparar_bd.ps1 -ConDemo   # una sola vez
.\deploy\windows\desplegar.ps1 -Version v1.3.0
.\deploy\windows\iniciar.ps1
.\deploy\windows\health_check.ps1                                                     # 7 comprobaciones
.\deploy\windows\detener.ps1
```

Las variables (base, usuario, contraseñas iniciales, almacén TLS, carpetas) se definen en `C:\PiuraVigila-deploy\config\env.ps1`, que queda fuera del repositorio. Plantilla sin secretos: `deploy/env.ejemplo.ps1`.

## Qué se agregó al código (PR #16)

- Actuator con solo `health` e `info` expuestos. `health` no muestra detalles; `/actuator/health/db` comprueba PostgreSQL por separado; `/actuator/info` muestra versión, commit y fecha del build.
- Perfil `prod` (`application-prod.properties`): HTTPS en 8443, conector HTTP que redirige a HTTPS (`RedireccionHttps`), caché de plantillas y cookie de sesión `Secure`.
- `CabecerasSeguridad` envía HSTS en respuestas HTTPS; la cookie de dispositivo lleva `Secure` cuando la petición llega por HTTPS.
- Pruebas nuevas: TC-SEC-15, TC-SEC-16 y TC-DEP-01 a TC-DEP-03 (121 unitarias y 20 de integración en total).

## Observaciones de la Semana 14

| Observación | Estado | Evidencia |
|---|---|---|
| HTTPS en el despliegue | Cerrada | S15-E5: HTTP 8080 → 302 a `https://localhost:8443/`; TLS validado con el certificado del despliegue |
| HSTS | Cerrada | S15-E5 y HC-06: `Strict-Transport-Security: max-age=31536000; includeSubDomains`; TC-SEC-15 |
| Cookies `Secure` | Cerrada | S15-E5: `JSESSIONID` y `pv_dispositivo` con `Secure; HttpOnly; SameSite=Lax`; TC-SEC-16 |
| Dependency-Check (NVD) | Pendiente justificado | Requiere una clave del NVD a nombre del grupo. El perfil `security-sca` está listo; mientras tanto la SCA se hace con OSV (S14-E3/E5) |
| CAPTCHA real | Pendiente justificado | Requiere claves de un proveedor (reCAPTCHA/hCaptcha). Hoy funcionan la suma de verificación, los límites por dispositivo e IP y el bloqueo de envíos simultáneos |
| Migración a Spring Boot 4 (R1) | Planificada | `Plan_migracion_SpringBoot4.md`. Los dos CVE de `spring-webmvc` no aplican a esta aplicación |

ZAP pasivo contra el despliegue (S15-E9): 0 alertas altas, medias o bajas; 16 informativas de 3 tipos (en la línea inicial de S14 hubo 53 alertas de 9 tipos).

## Incidencias del despliegue

| ID | Síntoma | Causa | Corrección |
|---|---|---|---|
| INC-001 | HC-03 en FAIL en la prueba previa | El patrón del script esperaba los campos de `/actuator/info` en otro orden | Patrón independiente del orden |
| INC-002 | Fecha de build 1980-02-01 en `/actuator/info` | Fecha fija de los builds reproducibles | `build-info` con la fecha real del build |
| INC-003 | `desplegar.ps1` se detuvo al clonar el tag | PowerShell 5.1 trata como error un aviso de git por stderr | El script valida el código de salida de git y mvn |

Las tres se repitieron con resultado PASS.

## Contenido de la carpeta

| Archivo | Contenido |
|---|---|
| `PiuraVigila_Semana15_Despliegue_Avance_80.xlsx` | Plantilla del curso: entorno, build, despliegue, 14 comprobaciones, incidencias, evidencias y checklist del 80 % (16 de 16) |
| `Evidencias/S15-E1` a `S15-E10` | Build, entorno, arranque, health checks, HTTPS/HSTS/cookies, PostgreSQL, flujo mínimo, capturas, ZAP pasivo, pruebas y CI |
| `Plan_migracion_SpringBoot4.md` | Plan y criterio para la migración |

## Para la Semana 16 (monitoreo)

Señales que se van a vigilar sobre el despliegue:

- Disponibilidad: `/actuator/health` y `/actuator/health/db` cada minuto; alerta si dejan de responder `UP`.
- Errores: líneas `ERROR` y `WARN` en `logs\piuravigila.log` (incluidos los inicios de sesión fallidos y los envíos rechazados por los límites).
- Rendimiento: tiempo de respuesta de `/`, `/reportar` y `/panel`, y tiempo de arranque (hoy unos 6 s).
- Recursos: memoria de la JVM, conexiones del pool a PostgreSQL y espacio en disco de `fotos\` y `logs\`.
- Certificado: vence el 17/11/2027.
