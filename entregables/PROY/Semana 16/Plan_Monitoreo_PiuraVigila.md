# Plan de Monitoreo · PiuraVigila

Curso Integrador I: Sistemas - Software · 2026-2 · versión desplegada `v1.4.0` · 24/11/2026

## 1. Esquema del plan

| Elemento | Descripción |
|---|---|
| Proyecto | PiuraVigila: portal de reportes ciudadanos de incidencias urbanas en Piura y panel de gestión municipal |
| Objetivo | Verificar que el portal esté disponible para los vecinos, que responda rápido, que la base de datos funcione y detectar errores, abusos o falta de recursos antes de que afecten a los usuarios |
| Componentes | Aplicación Java (Spring Boot 3.5, JAR en `C:\PiuraVigila-deploy\app`), servidor web embebido (Tomcat, HTTPS 8443), base de datos PostgreSQL 18 (`piuravigila_prod`), disco del servidor (fotos y logs), certificado TLS |
| Indicadores | Disponibilidad, estado de la BD, tiempo de respuesta, CPU, memoria, espacio en disco, errores en los logs, intentos de abuso (ver sección 2) |
| Herramientas | Logs estructurados (`piuravigila.log`, `piuravigila.json`, `errores.log`), Spring Boot Actuator (`/actuator/health`, `/actuator/health/db`, `/actuator/info`), métricas por JMX con JConsole o VisualVM, `jcmd`/`jstat` del JDK, script `monitorear.ps1` en el Programador de tareas, Administrador de tareas y Monitor de recursos de Windows |
| Frecuencia | Automática cada 5 minutos (`monitorear.ps1`); revisión diaria de `alertas.log` y `errores.log`; revisión semanal de tendencias en `monitoreo.csv` y de rendimiento con JConsole; revisión mensual del espacio en disco y del vencimiento del certificado |
| Responsable | Administrador del sistema (integrante del equipo de turno); el equipo de desarrollo atiende los errores de código |
| Acciones correctivas | Ver sección 5: reinicio automático si la aplicación cae, revisión de logs, reinicio de PostgreSQL, liberar disco, restaurar backup |

## 2. Métricas clave y umbrales

| # | Métrica | Cómo se mide | Umbral normal | Alerta cuando |
|---|---|---|---|---|
| M1 | Disponibilidad de la aplicación | `GET /actuator/health` cada 5 min | `UP` | Distinto de `UP` (se reinicia sola) |
| M2 | Disponibilidad de la base de datos | `GET /actuator/health/db` | `UP` | Distinto de `UP` |
| M3 | Tiempo de respuesta de la página de inicio | `curl` mide `/` | < 3 s | ≥ 3 s |
| M4 | CPU del proceso Java | muestra de 2 s del proceso | < 80 % | ≥ 80 % |
| M5 | Memoria del proceso Java | memoria usada / RAM total | < 75 % | ≥ 75 % |
| M6 | Espacio libre en disco | unidad de `C:\PiuraVigila-deploy` | > 10 % | ≤ 10 % |
| M7 | Errores de la aplicación | líneas `ERROR` en `errores.log`, últimos 5 min | 0 | ≥ 1 |
| M8 | Intentos de abuso | `WARN` de login fallido o bloqueado, anti-spam y límites por IP | pocos y aislados | ráfagas desde la misma IP (revisión diaria) |
| M9 | Uso del pool de conexiones a BD | métrica `hikaricp.connections.active` por JMX | < 10 (máximo del pool) | igual al máximo de forma sostenida (revisión semanal) |
| M10 | Vencimiento del certificado TLS | `keytool -printcert` | > 30 días | ≤ 30 días (revisión mensual) |

Los valores de M1 a M7 se guardan en cada corrida en `logs\monitoreo.csv` (una fila cada 5 minutos), que permite ver la tendencia en Excel.

## 3. Logs estructurados con niveles

| Archivo | Contenido | Para qué |
|---|---|---|
| `piuravigila.log` | Texto: fecha, nivel, proceso, hilo, clase y mensaje | Lectura directa por el equipo |
| `piuravigila.json` | El mismo evento en JSON (formato ECS): `@timestamp`, `log.level`, `log.logger`, `message`, `service.version` | Filtrar y contar por nivel o por clase |
| `errores.log` | Solo `WARN` y `ERROR` | Revisar incidentes sin ruido |

Rotación diaria o a los 10 MB, con 30 días de historial (90 para `errores.log`), en `logs\archivo`.

| Nivel | Cuándo se usa | Ejemplos reales |
|---|---|---|
| `INFO` | Operaciones normales | Inicio de la aplicación, caso registrado, reporte vinculado, estado actualizado, inicio y cierre de sesión, usuario creado, contraseña cambiada, backup y limpieza de fotos |
| `WARN` | Rechazos y situaciones que deben vigilarse | Inicio de sesión fallido o bloqueado, token CSRF inválido, reporte rechazado por anti-spam, límite de envíos o consultas por IP superado |
| `ERROR` | Fallos que impiden atender una petición | Error no controlado (`ManejoErrores`), con la traza en el log y un mensaje genérico al usuario |

Los logs no registran contraseñas, tokens ni el contenido de las fotos.

## 4. Herramientas

| Herramienta | Qué muestra | Cómo se usa |
|---|---|---|
| `monitorear.ps1` (tarea programada cada 5 min) | M1 a M7, alertas y reinicio automático | Automático; resultados en `logs\monitoreo.csv` y `logs\alertas.log` |
| Actuator | Salud de la aplicación y de la BD, versión desplegada | `https://localhost:8443/actuator/health`, `/health/db`, `/info` |
| JConsole o VisualVM (JMX) | Memoria heap, hilos, CPU, clases, y en la pestaña MBeans `org.springframework.boot > Endpoint > Metrics`: `jvm.memory.used`, `process.cpu.usage`, `http.server.requests`, `hikaricp.connections.active` | `jconsole` (incluido en el JDK) → conectar al proceso local `piuravigila.jar` |
| `jcmd` / `jstat` (JDK) | Tiempo en marcha, uso del heap, recolector de basura | `jcmd <PID> GC.heap_info`, `jstat -gcutil <PID> 1000 5` |
| `deploy/MetricasJmx.java` | Las mismas métricas de JConsole en texto (60 métricas; las clave con su valor actual) | `java --add-modules jdk.attach deploy\MetricasJmx.java <PID>` |
| Administrador de tareas y Monitor de recursos | CPU, RAM, disco y red de todo el equipo | Revisión manual cuando hay una alerta |
| `health_check.ps1` | 7 comprobaciones del despliegue (HTTPS, HSTS, redirección, versión) | Después de cada despliegue |

## 5. Acciones correctivas

| Situación | Acción |
|---|---|
| La aplicación no responde (M1) | `monitorear.ps1` la reinicia automáticamente y lo anota en `alertas.log`. Si vuelve a caer: revisar `errores.log` y `consola-error.log`, corregir y redesplegar |
| La BD no responde (M2) | Verificar el servicio `postgresql-x64-18` en Servicios de Windows y reiniciarlo; revisar el log de PostgreSQL |
| Respuesta lenta (M3) o CPU alta (M4) | Revisar en JConsole hilos y `http.server.requests`; buscar en los logs ráfagas de envíos; revisar consultas del panel |
| Memoria alta (M5) | Revisar el heap en JConsole; si no baja tras una recolección, reiniciar con `detener.ps1` + `iniciar.ps1` y registrar la incidencia |
| Disco bajo (M6) | Ejecutar `limpiar.ps1`; revisar el tamaño de `fotos` y `backups`; la retención de fotos borra las de casos atendidos hace más de 365 días |
| Errores en el log (M7) | Leer la traza en `errores.log`, reproducir, corregir en una rama, PR con pruebas y redesplegar |
| Abuso (M8) | Confirmar que los límites actuaron (respuestas 429); si persiste, bajar los límites en la configuración |
| Pérdida de datos | Restaurar el último backup con `restaurar.ps1` (ver procedimientos de mantenimiento, Semana 17) |

Toda incidencia se registra con fecha, síntoma, causa, corrección y reprueba, igual que en la hoja Incidencias de la Semana 15.
