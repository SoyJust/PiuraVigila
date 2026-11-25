<# Inicia el JAR desplegado con el perfil prod. La salida de consola queda en <Base>\logs. #>
param([string]$Base = "C:\PiuraVigila-deploy")
$ErrorActionPreference = "Stop"
. (Join-Path $Base "config\env.ps1")

$pidFile = Join-Path $Base "app\piuravigila.pid"
if (Test-Path $pidFile) {
    $anterior = Get-Process -Id (Get-Content $pidFile) -ErrorAction SilentlyContinue
    if ($anterior) { Write-Output "PiuraVigila ya está en ejecución (PID $($anterior.Id))."; exit 0 }
}
Remove-Item (Join-Path $Base "app\detenido.flag") -ErrorAction SilentlyContinue
$java = Join-Path $env:JAVA_HOME "bin\java.exe"
$proceso = Start-Process -FilePath $java -WorkingDirectory $Base -WindowStyle Hidden -PassThru `
    -ArgumentList "-jar", "`"$Base\app\piuravigila.jar`"", "--spring.profiles.active=prod" `
    -RedirectStandardOutput "$Base\logs\consola.log" -RedirectStandardError "$Base\logs\consola-error.log"
$proceso.Id | Set-Content $pidFile
Write-Output "PiuraVigila iniciado (PID $($proceso.Id)). Comprobar con health_check.ps1."
