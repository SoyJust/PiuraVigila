<# Detiene el proceso iniciado por iniciar.ps1. #>
param([string]$Base = "C:\PiuraVigila-deploy")
$pidFile = Join-Path $Base "app\piuravigila.pid"
if (-not (Test-Path $pidFile)) { Write-Output "No hay PID registrado."; exit 0 }
$id = Get-Content $pidFile
$proceso = Get-Process -Id $id -ErrorAction SilentlyContinue
if ($proceso) { Stop-Process -Id $id; Write-Output "PiuraVigila detenido (PID $id)." }
else { Write-Output "El proceso $id ya no estaba en ejecución." }
Remove-Item $pidFile
# El monitoreo no reinicia una aplicación detenida a propósito; iniciar.ps1 quita esta marca.
Set-Content (Join-Path $Base "app\detenido.flag") (Get-Date -Format s)
