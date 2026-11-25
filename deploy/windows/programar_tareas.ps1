<#
Registra las tareas programadas del despliegue en el Programador de tareas de Windows (equivalente a cron).
Copia los scripts a <Base>\scripts para que las tareas no dependan de la carpeta del repositorio.
  PiuraVigila\Monitoreo         cada 5 minutos             monitorear.ps1   (fila en monitoreo.csv, alertas y reinicio;
                                                                            tras reiniciar la PC levanta la app en 5 min)
  PiuraVigila\Backup diario     todos los días 02:00       respaldar.ps1    (BD + fotos, conserva 7)
  PiuraVigila\Limpieza semanal  domingos 03:00             limpiar.ps1      (logs viejos, builds anteriores)
No requiere permisos de administrador: las tareas corren con el usuario actual mientras tiene la sesión iniciada.
Uso:  .\programar_tareas.ps1          registrar o actualizar
      .\programar_tareas.ps1 -Quitar  eliminar las tres tareas
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [switch]$Quitar
)
$ErrorActionPreference = "Continue"
$tareas = @(
    @{ Nombre = "PiuraVigila\Monitoreo";        Script = "monitorear.ps1"; Programa = @("/sc", "minute", "/mo", "5") },
    @{ Nombre = "PiuraVigila\Backup diario";    Script = "respaldar.ps1";  Programa = @("/sc", "daily", "/st", "02:00") },
    @{ Nombre = "PiuraVigila\Limpieza semanal"; Script = "limpiar.ps1";    Programa = @("/sc", "weekly", "/d", "SUN", "/st", "03:00") }
)

if ($Quitar) {
    foreach ($t in $tareas) { schtasks /delete /tn $t.Nombre /f 2>$null | Out-Null; Write-Output "Eliminada: $($t.Nombre)" }
    exit 0
}

$scripts = Join-Path $Base "scripts"
New-Item -ItemType Directory -Force $scripts | Out-Null
Copy-Item "$PSScriptRoot\*.ps1" $scripts -Force

foreach ($t in $tareas) {
    $comando = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$scripts\$($t.Script)`""
    $argumentos = @("/create", "/f", "/tn", $t.Nombre, "/tr", $comando) + $t.Programa
    $salida = & schtasks.exe @argumentos 2>&1
    if ($LASTEXITCODE -eq 0) { Write-Output "Registrada: $($t.Nombre) ($($t.Programa -join ' '))" }
    else { Write-Output "ERROR al registrar $($t.Nombre): $salida" }
}
schtasks /query /tn "PiuraVigila\" /fo LIST 2>$null | Select-String "Nombre de tarea|TaskName|Próxima|Next Run|Estado|Status"
