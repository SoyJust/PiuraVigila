# Semana 16 · Monitoreo y avance al 90 %

PiuraVigila · Curso Integrador I: Sistemas - Software · 2026-2 · 24/11/2026

## Qué se evalúa (rúbrica) y dónde está

| Criterio de la rúbrica | Dónde se ve |
|---|---|
| Implementa logs estructurados con niveles adecuados | `logback-spring.xml`: `piuravigila.log` (texto), `piuravigila.json` (JSON ECS) y `errores.log` (solo WARN y ERROR), con rotación. Nuevos eventos: cierre de sesión, login bloqueado o con CSRF inválido, reportes rechazados, límites por IP. Evidencia: S16-E1; pruebas TC-MON-01 y 02 |
| Configura una herramienta de monitoreo de rendimiento y de salud del sistema | Actuator (`/actuator/health`, `/health/db`, `/info`), métricas por JMX para JConsole o VisualVM (memoria, CPU, hilos, `http.server.requests`, pool de BD), `jcmd`/`jstat`, y `monitorear.ps1` como tarea programada cada 5 minutos con alertas y reinicio automático. Evidencia: S16-E2 y S16-E3; prueba TC-MON-03 |
| Define métricas clave alineadas al sistema | 10 métricas con umbral (disponibilidad, BD, respuesta < 3 s, CPU < 80 %, memoria < 75 %, disco > 10 %, errores, abuso, pool de BD, certificado) en el plan, sección 2 |
| Presenta un plan documentado de monitoreo | `Plan_Monitoreo_PiuraVigila.md`, con el esquema de la clase: proyecto, objetivo, componentes, indicadores, herramientas, frecuencia, responsable y acciones correctivas |

## Prueba real del monitoreo

| Hora | Qué pasó | Registro |
|---|---|---|
| 19:22 | La aplicación estaba caída (la PC se había reiniciado). La primera corrida de `monitorear.ps1` lo detectó y la levantó | `monitoreo.csv`: `SIN_RESPUESTA … reinicio automático`; alerta en `alertas.log` |
| 19:27 | Corrida lanzada por el Programador de tareas: todo normal (respuesta 40 ms, 245 MB, 3 % de la RAM) | `monitoreo.csv`: `UP,UP,200,40,…` |
| 19:28 | Se terminó el proceso Java a la fuerza para simular una caída | `health` sin respuesta |
| 19:32 | La tarea programada, sin intervención, detectó la caída y reinició la aplicación | `monitoreo.csv` y `alertas.log`; a las 19:33 `health` volvió a `UP` |

## Versión

`v1.4.0` (commit `c99fa58`, PR #19). 124 pruebas unitarias (3 nuevas: TC-MON-01 a 03) y 20 de integración, local y en CI. Desplegada desde la etiqueta; `health_check.ps1` 7 de 7.

## Contenido

| Archivo | Contenido |
|---|---|
| `Plan_Monitoreo_PiuraVigila.md` | Plan de monitoreo |
| `Evidencias/S16-E1_logs_estructurados.txt` | Los tres archivos de log con eventos reales y conteo por nivel |
| `Evidencias/S16-E2_herramientas_rendimiento_salud.txt` | Actuator, `jcmd` (memoria, tiempo en marcha) y `jstat` sobre el proceso desplegado |
| `Evidencias/S16-E3_monitoreo_automatico.txt` | `monitoreo.csv` y `alertas.log` con la caída y el reinicio automático |
| `Evidencias/S16-E4_monitoreo.csv` | Datos del monitoreo para abrir en Excel |
