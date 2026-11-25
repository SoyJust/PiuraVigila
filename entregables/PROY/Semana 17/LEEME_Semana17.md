# Semana 17 · Mantenimiento y avance al 100 %

PiuraVigila · Curso Integrador I: Sistemas - Software · 2026-2 · 24/11/2026

## Qué se evalúa (rúbrica) y dónde está

| Criterio de la rúbrica | Dónde se ve |
|---|---|
| Implementa cron jobs funcionales y automatizados | Tres tareas en el Programador de tareas de Windows: `PiuraVigila\Monitoreo` (cada 5 min), `PiuraVigila\Backup diario` (02:00), `PiuraVigila\Limpieza semanal` (domingos 03:00). Las registra `deploy/windows/programar_tareas.ps1`. Evidencia: S17-E1 |
| Configura backups programados correctamente | `respaldar.ps1`: `pg_dump` de `piuravigila_prod` + fotos + manifiesto con SHA-256; conserva 7. Restauración probada en una base aparte con los mismos 76 reportes y 76 fotos. Evidencia: S17-E2 y S17-E3 |
| Desarrolla scripts funcionales | `deploy/windows`: `monitorear`, `respaldar`, `restaurar`, `limpiar`, `programar_tareas`, además de los de despliegue. Todos ejecutados contra el despliegue real (S17-E3) |
| Documenta los procedimientos | `Procedimientos_Mantenimiento_PiuraVigila.md`: tareas, scripts, backup, restauración (prueba y recuperación de producción), nueva versión y revisión periódica |

## Versión

`v1.4.0` (commit `c99fa58`, PR #19), desplegada desde la etiqueta con `desplegar.ps1` en `C:\PiuraVigila-deploy`. 124 pruebas unitarias y 20 de integración en verde, local y en CI.

## Contenido

| Archivo | Contenido |
|---|---|
| `Procedimientos_Mantenimiento_PiuraVigila.md` | Procedimientos documentados |
| `Evidencias/S17-E1_tareas_programadas.txt` | Estado, frecuencia, última y próxima ejecución de cada tarea |
| `Evidencias/S17-E2_backups.txt` | Backups generados y manifiesto del último |
| `Evidencias/S17-E3_mantenimiento_log.txt` | Registro de backups (manual y por tarea), restauración de prueba y limpieza |

## Decisiones

- Windows no tiene cron; el Programador de tareas cumple el mismo papel. Las tareas corren con el usuario del equipo, sin permisos de administrador. Por eso no se usó una tarea "al iniciar sesión": el monitoreo cada 5 minutos ya levanta la aplicación después de reiniciar la PC.
- El backup usa el rol de la aplicación (solo lectura y escritura de datos). La restauración pide la clave de `postgres` en el momento y no la guarda.
- Restaurar sobre producción exige `-ConfirmarProduccion`; la prueba mensual restaura en `piuravigila_restauracion`.
