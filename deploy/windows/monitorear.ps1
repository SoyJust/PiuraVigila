<#
Monitoreo del despliegue (Semana 16). Lo ejecuta la tarea programada "PiuraVigila\Monitoreo" cada 5 minutos.
Por cada corrida agrega una fila a logs\monitoreo.csv; si un indicador supera su umbral, escribe en logs\alertas.log;
si la aplicación no responde y no fue detenida a propósito, la reinicia con iniciar.ps1.
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [string]$Https = "https://localhost:8443",
    [int]$RespuestaMaxMs = 3000,
    [int]$CpuMaxPct = 80,
    [int]$MemoriaMaxPct = 75,
    [int]$DiscoMinPct = 10,
    [int]$ErroresMax = 0
)
$ErrorActionPreference = "Continue"
$ca = Join-Path $Base "certs\piuravigila.crt"
$logs = Join-Path $Base "logs"
$csv = Join-Path $logs "monitoreo.csv"
$alertas = Join-Path $logs "alertas.log"
$ahora = Get-Date

function Get-Url([string]$Ruta) {
    $salida = & curl.exe -s --max-time 10 --cacert $ca -o NUL -w "%{http_code} %{time_total}" "$Https$Ruta"
    $partes = "$salida".Trim() -split " "
    [pscustomobject]@{ Codigo = [int]$partes[0]; Ms = [math]::Round([double]::Parse($partes[1], [Globalization.CultureInfo]::InvariantCulture) * 1000) }
}
function Get-Estado([string]$Ruta) {
    $cuerpo = & curl.exe -s --max-time 10 --cacert $ca "$Https$Ruta"
    if ("$cuerpo" -match '"status":"([A-Z_]+)"') { $Matches[1] } else { "SIN_RESPUESTA" }
}
function Write-Alerta([string]$Texto) {
    Add-Content -Path $alertas -Encoding UTF8 -Value ("{0}  WARN  {1}" -f $ahora.ToString("yyyy-MM-dd HH:mm:ss"), $Texto)
}

# 1. Disponibilidad y tiempo de respuesta
$app = Get-Estado "/actuator/health"
$bd = Get-Estado "/actuator/health/db"
$inicio = Get-Url "/"

# 2. Proceso: CPU (muestra de 2 s) y memoria
$cpu = ""; $memMb = ""; $memPct = ""
$pidFile = Join-Path $Base "app\piuravigila.pid"
$proceso = if (Test-Path $pidFile) { Get-Process -Id (Get-Content $pidFile) -ErrorAction SilentlyContinue }
if ($proceso) {
    $t0 = $proceso.TotalProcessorTime; Start-Sleep -Seconds 2; $proceso.Refresh()
    $cpu = [math]::Round(($proceso.TotalProcessorTime - $t0).TotalMilliseconds / (2000 * [Environment]::ProcessorCount) * 100, 1)
    $memMb = [math]::Round($proceso.WorkingSet64 / 1MB)
    $total = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory
    $memPct = [math]::Round($proceso.WorkingSet64 / $total * 100, 1)
}

# 3. Disco donde están las fotos y los logs
$unidad = (Get-Item $Base).PSDrive.Name
$disco = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='${unidad}:'"
$discoPct = [math]::Round($disco.FreeSpace / $disco.Size * 100, 1)

# 4. Errores registrados en los últimos 5 minutos (errores.log solo tiene WARN y ERROR)
$errores = 0
$archivoErrores = Join-Path $logs "errores.log"
if (Test-Path $archivoErrores) {
    $desde = $ahora.AddMinutes(-5)
    foreach ($linea in Get-Content $archivoErrores -Tail 500 -Encoding UTF8) {
        if ($linea -match '^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)\S*\s+ERROR\s') {
            if ([datetime]::ParseExact($Matches[1], "yyyy-MM-ddTHH:mm:ss", $null) -ge $desde) { $errores++ }
        }
    }
}

# 5. Umbrales y acciones correctivas
$accion = "ninguna"
if ($app -ne "UP") {
    if (Test-Path (Join-Path $Base "app\detenido.flag")) {
        $accion = "sin reinicio (detenida a propósito)"
    } else {
        Write-Alerta "La aplicación no responde (health=$app). Se reinicia automáticamente."
        & (Join-Path $PSScriptRoot "iniciar.ps1") -Base $Base | Out-Null
        $accion = "reinicio automático"
    }
} else {
    if ($bd -ne "UP") { Write-Alerta "PostgreSQL no responde (health/db=$bd)." }
    if ($inicio.Ms -gt $RespuestaMaxMs) { Write-Alerta "Tiempo de respuesta de / = $($inicio.Ms) ms (umbral $RespuestaMaxMs ms)." }
    if ($cpu -ne "" -and $cpu -gt $CpuMaxPct) { Write-Alerta "CPU del proceso = $cpu % (umbral $CpuMaxPct %)." }
    if ($memPct -ne "" -and $memPct -gt $MemoriaMaxPct) { Write-Alerta "Memoria del proceso = $memPct % de la RAM (umbral $MemoriaMaxPct %)." }
}
if ($discoPct -lt $DiscoMinPct) { Write-Alerta "Espacio libre en disco ${unidad}: = $discoPct % (mínimo $DiscoMinPct %)." }
if ($errores -gt $ErroresMax) { Write-Alerta "$errores errores (ERROR) en los últimos 5 minutos; revisar logs\errores.log." }

if (-not (Test-Path $csv)) {
    Add-Content -Path $csv -Encoding UTF8 -Value "fecha,app,bd,http_inicio,respuesta_ms,cpu_pct,memoria_mb,memoria_pct,disco_libre_pct,errores_5min,accion"
}
$fila = "{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10}" -f $ahora.ToString("yyyy-MM-dd HH:mm:ss"), $app, $bd, $inicio.Codigo, $inicio.Ms, $cpu, $memMb, $memPct, $discoPct, $errores, $accion
Add-Content -Path $csv -Encoding UTF8 -Value $fila
Write-Output $fila
exit 0
