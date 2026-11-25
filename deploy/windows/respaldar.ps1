<#
Backup del despliegue (Semana 17). Lo ejecuta la tarea programada "PiuraVigila\Backup diario".
Genera <Base>\backups\AAAAMMDD_HHMMSS\ con:
  bd.dump      copia de piuravigila_prod (pg_dump, formato custom, se restaura con pg_restore)
  fotos.zip    las fotos de los reportes
  manifiesto.txt  tamaños, SHA-256 y número de objetos del dump
Conserva los últimos $Conservar backups y borra los más antiguos.
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin",
    [int]$Conservar = 7
)
$ErrorActionPreference = "Continue"
. (Join-Path $Base "config\env.ps1")
$registro = Join-Path $Base "logs\mantenimiento.log"
function Write-Registro([string]$Nivel, [string]$Texto) {
    $linea = "{0}  {1,-5}  backup  {2}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Nivel, $Texto
    Add-Content -Path $registro -Encoding UTF8 -Value $linea
    Write-Output $linea
}

$marca = Get-Date -Format "yyyyMMdd_HHmmss"
$destino = Join-Path $Base "backups\$marca"
New-Item -ItemType Directory -Force $destino | Out-Null
$bd = ($env:PIURA_DB_URL -split "/")[-1]

# 1. Base de datos
$env:PGPASSWORD = $env:PIURA_DB_PASSWORD
& (Join-Path $PgBin "pg_dump.exe") -h localhost -U $env:PIURA_DB_USER -d $bd -Fc -f (Join-Path $destino "bd.dump")
$codigo = $LASTEXITCODE
$objetos = (& (Join-Path $PgBin "pg_restore.exe") -l (Join-Path $destino "bd.dump") | Where-Object { $_ -notmatch '^;' -and $_.Trim() }).Count
$env:PGPASSWORD = $null
if ($codigo -ne 0 -or $objetos -eq 0) {
    Write-Registro "ERROR" "pg_dump de $bd falló (código $codigo); se borra el backup incompleto $marca."
    Remove-Item -Recurse -Force $destino
    exit 1
}

# 2. Fotos
$fotos = Join-Path $Base "fotos"
$cantidadFotos = @(Get-ChildItem $fotos -File -ErrorAction SilentlyContinue).Count
if ($cantidadFotos -gt 0) { Compress-Archive -Path "$fotos\*" -DestinationPath (Join-Path $destino "fotos.zip") -CompressionLevel Optimal }

# 3. Manifiesto
$lineas = @("Backup PiuraVigila $marca", "Base de datos: $bd ($objetos objetos en el dump)", "Fotos: $cantidadFotos archivos", "")
foreach ($f in Get-ChildItem $destino -File) {
    $lineas += "{0}  {1} bytes  SHA-256 {2}" -f $f.Name, $f.Length, (Get-FileHash $f.FullName -Algorithm SHA256).Hash
}
Set-Content -Path (Join-Path $destino "manifiesto.txt") -Value $lineas -Encoding UTF8
$tamano = [math]::Round(((Get-ChildItem $destino -File | Measure-Object Length -Sum).Sum) / 1MB, 2)
Write-Registro "INFO" "Backup $marca creado: $objetos objetos de BD, $cantidadFotos fotos, $tamano MB."

# 4. Rotación
$antiguos = Get-ChildItem (Join-Path $Base "backups") -Directory | Sort-Object Name -Descending | Select-Object -Skip $Conservar
foreach ($d in $antiguos) {
    Remove-Item -Recurse -Force $d.FullName
    Write-Registro "INFO" "Backup antiguo eliminado: $($d.Name) (se conservan $Conservar)."
}
exit 0
