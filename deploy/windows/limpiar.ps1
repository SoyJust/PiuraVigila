<#
Limpieza semanal (Semana 17). La ejecuta la tarea programada "PiuraVigila\Limpieza semanal".
- Borra logs archivados con más de $DiasLogs días (Logback ya rota; esto cubre los .gz antiguos y la salida de consola).
- Archiva monitoreo.csv y alertas.log cuando superan 5 MB.
- Borra las carpetas de build de versiones anteriores (queda solo la desplegada).
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [int]$DiasLogs = 30
)
$ErrorActionPreference = "Continue"
$logs = Join-Path $Base "logs"
$registro = Join-Path $logs "mantenimiento.log"
function Write-Registro([string]$Texto) {
    $linea = "{0}  INFO   limpieza  {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Texto
    Add-Content -Path $registro -Encoding UTF8 -Value $linea
    Write-Output $linea
}
$limite = (Get-Date).AddDays(-$DiasLogs)

$viejos = @(Get-ChildItem (Join-Path $logs "archivo"), $logs -File -ErrorAction SilentlyContinue |
    Where-Object { $_.LastWriteTime -lt $limite -and $_.Name -match '\.(gz|log|txt|csv)$' -and $_.Name -notin @("piuravigila.log", "errores.log", "mantenimiento.log", "monitoreo.csv", "alertas.log") })
$liberado = [math]::Round((($viejos | Measure-Object Length -Sum).Sum) / 1MB, 2)
$viejos | Remove-Item -Force
Write-Registro "Logs con más de $DiasLogs días eliminados: $($viejos.Count) ($liberado MB)."

foreach ($nombre in "monitoreo.csv", "alertas.log") {
    $f = Join-Path $logs $nombre
    if ((Test-Path $f) -and (Get-Item $f).Length -gt 5MB) {
        New-Item -ItemType Directory -Force (Join-Path $logs "archivo") | Out-Null
        $archivo = Join-Path $logs ("archivo\{0}.{1}{2}" -f [IO.Path]::GetFileNameWithoutExtension($nombre), (Get-Date -Format "yyyyMMdd"), [IO.Path]::GetExtension($nombre))
        Move-Item $f $archivo
        Write-Registro "$nombre archivado en $archivo."
    }
}

$actual = if (Test-Path (Join-Path $Base "app\despliegues.log")) { ((Get-Content (Join-Path $Base "app\despliegues.log") -Tail 1) -split "\s+")[1] }
$builds = @(Get-ChildItem (Join-Path $Base "build") -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne $actual })
foreach ($b in $builds) { Remove-Item -Recurse -Force $b.FullName }
Write-Registro "Carpetas de build anteriores eliminadas: $($builds.Count) (se conserva $actual)."
$disco = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='$((Get-Item $Base).PSDrive.Name):'"
Write-Registro ("Espacio libre en disco: {0} GB ({1} %)." -f [math]::Round($disco.FreeSpace / 1GB, 1), [math]::Round($disco.FreeSpace / $disco.Size * 100, 1))
exit 0
