# Procedimientos de mantenimiento · PiuraVigila

Curso Integrador I: Sistemas - Software · 2026-2 · versión desplegada `v1.4.0` · 24/11/2026

Todo se ejecuta sobre el despliegue local `C:\PiuraVigila-deploy`. Los scripts están versionados en `deploy/windows` del repositorio y `programar_tareas.ps1` los copia a `C:\PiuraVigila-deploy\scripts`, que es la carpeta que usan las tareas programadas.

## 1. Tareas programadas (cron jobs)

En Windows el equivalente a cron es el **Programador de tareas**. Se registran con `programar_tareas.ps1` y corren con el usuario del equipo, sin permisos de administrador.

| Tarea | Frecuencia | Script | Qué hace | Registro |
|---|---|---|---|---|
| `PiuraVigila\Monitoreo` | Cada 5 minutos | `monitorear.ps1` | Mide disponibilidad, BD, tiempo de respuesta, CPU, memoria, disco y errores; alerta si algo supera su umbral; reinicia la aplicación si cayó | `logs\monitoreo.csv`, `logs\alertas.log` |
| `PiuraVigila\Backup diario` | Todos los días, 02:00 | `respaldar.ps1` | Copia la base `piuravigila_prod` y las fotos; conserva los 7 backups más recientes | `logs\mantenimiento.log` |
| `PiuraVigila\Limpieza semanal` | Domingos, 03:00 | `limpiar.ps1` | Borra logs archivados de más de 30 días y builds de versiones anteriores; archiva `monitoreo.csv` y `alertas.log` si pasan de 5 MB; informa el espacio libre | `logs\mantenimiento.log` |

Además, la aplicación tiene su propia tarea interna (`@Scheduled`, `RetencionFotos`): todos los días a las 03:15 borra las fotos de casos cerrados hace más de 365 días.

```powershell
.\deploy\windows\programar_tareas.ps1           # registrar o actualizar las tres tareas
schtasks /query /tn "PiuraVigila\" /fo LIST       # ver estado, última y próxima ejecución
schtasks /run /tn "PiuraVigila\Backup diario"     # ejecutar una tarea ahora
.\deploy\windows\programar_tareas.ps1 -Quitar   # eliminar las tareas
```

## 2. Scripts

| Script | Uso | Cuándo |
|---|---|---|
| `preparar_bd.ps1` | Crea `piuravigila_prod` y el rol `piuravigila_app` sin permisos de DDL | Una vez, al instalar |
| `desplegar.ps1 -Version vX.Y.Z` | Clona la etiqueta en una carpeta limpia, `mvn clean package` y copia el JAR | Cada versión nueva |
| `iniciar.ps1` / `detener.ps1` | Arranca o detiene la aplicación (`detener` deja una marca para que el monitoreo no la reinicie) | A mano |
| `health_check.ps1` | 7 comprobaciones del despliegue | Después de desplegar |
| `monitorear.ps1` | Monitoreo (ver Plan de Monitoreo) | Tarea cada 5 min |
| `respaldar.ps1` | Backup de BD y fotos | Tarea diaria |
| `restaurar.ps1` | Restaura un backup | A mano, al probar o recuperar |
| `limpiar.ps1` | Limpieza de logs y builds | Tarea semanal |
| `programar_tareas.ps1` | Registra las tareas programadas | Una vez, o tras cambiar los scripts |

## 3. Procedimiento: backup

1. Lo hace la tarea `PiuraVigila\Backup diario` a las 02:00. Para uno manual: `schtasks /run /tn "PiuraVigila\Backup diario"` o `.\deploy\windows\respaldar.ps1`.
2. Resultado en `C:\PiuraVigila-deploy\backups\AAAAMMDD_HHMMSS\`:
   - `bd.dump`: `pg_dump` en formato custom con el rol de la aplicación.
   - `fotos.zip`: todas las fotos.
   - `manifiesto.txt`: número de objetos del dump, cantidad de fotos, tamaño y SHA-256 de cada archivo.
3. Si `pg_dump` falla o el dump queda vacío, el backup incompleto se borra y se registra `ERROR` en `logs\mantenimiento.log`.
4. Se conservan los últimos 7. Para conservar uno por más tiempo, copiarlo fuera de `backups` (por ejemplo a un disco externo).

## 4. Procedimiento: restauración

**Prueba de restauración (recomendada una vez al mes; no toca producción):**

```powershell
$env:PGPASSWORD = "<clave de postgres>"
.\deploy\windows\restaurar.ps1 -ConFotos          # último backup → base piuravigila_restauracion
```

Comprobar en `logs\mantenimiento.log` que el número de reportes, usuarios y movimientos coincide con producción.

**Recuperación de producción:**

1. `.\deploy\windows\detener.ps1`
2. `$env:PGPASSWORD = "<clave de postgres>"`
3. `.\deploy\windows\restaurar.ps1 -Respaldo <AAAAMMDD_HHMMSS> -BaseDestino piuravigila_prod -ConfirmarProduccion -ConFotos`
4. `.\deploy\windows\iniciar.ps1` y `.\deploy\windows\health_check.ps1`
5. Registrar la incidencia: qué pasó, qué backup se usó y qué datos posteriores se perdieron.

Sin `-ConfirmarProduccion` el script se niega a restaurar sobre `piuravigila_prod`. La clave de `postgres` no se guarda en ningún archivo.

## 5. Procedimiento: nueva versión

1. Rama `feature/*` → PR → CI en verde → merge a `main` → etiqueta `vX.Y.Z`.
2. Backup previo: `schtasks /run /tn "PiuraVigila\Backup diario"`.
3. `.\deploy\windows\detener.ps1` → `.\deploy\windows\desplegar.ps1 -Version vX.Y.Z` → `.\deploy\windows\iniciar.ps1`.
4. `.\deploy\windows\health_check.ps1` (7 de 7 PASS) y revisar `logs\errores.log`.
5. Si algo falla: `desplegar.ps1 -Version <versión anterior>` e `iniciar.ps1` (vuelta atrás).

## 6. Revisión periódica

| Frecuencia | Qué revisar |
|---|---|
| Diaria | `logs\alertas.log` y `logs\errores.log`; que exista el backup del día en `mantenimiento.log` |
| Semanal | Tendencia de `monitoreo.csv`; resultado de la limpieza; `schtasks /query /tn "PiuraVigila\"` sin errores |
| Mensual | Prueba de restauración; espacio en disco; actualizaciones de dependencias (SCA con OSV); vencimiento del certificado TLS (vence el 17/11/2027) |
