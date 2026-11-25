<#
Restauración de un backup (Semana 17). Se ejecuta a mano.
Por defecto restaura el backup más reciente en una base de PRUEBA (piuravigila_restauracion) para verificarlo sin tocar
producción. Para recuperar producción: detener.ps1, luego -BaseDestino piuravigila_prod -ConfirmarProduccion, luego iniciar.ps1.
Requiere la clave del superusuario de PostgreSQL en PGPASSWORD (no se guarda en ningún archivo).
Uso:  $env:PGPASSWORD = "<clave de postgres>"; .\restaurar.ps1 [-Respaldo 20261124_020000] [-BaseDestino ...] [-ConFotos]
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [string]$Respaldo = "",
    [string]$BaseDestino = "piuravigila_restauracion",
    [string]$SuperUsuario = "postgres",
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin",
    [switch]$ConFotos,
    [switch]$ConfirmarProduccion
)
$ErrorActionPreference = "Continue"
. (Join-Path $Base "config\env.ps1")
$registro = Join-Path $Base "logs\mantenimiento.log"
function Write-Registro([string]$Nivel, [string]$Texto) {
    $linea = "{0}  {1,-5}  restaurar  {2}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Nivel, $Texto
    Add-Content -Path $registro -Encoding UTF8 -Value $linea
    Write-Output $linea
}
if (-not $env:PGPASSWORD) { throw "Defina PGPASSWORD con la clave de $SuperUsuario antes de restaurar." }
$prod = ($env:PIURA_DB_URL -split "/")[-1]
if ($BaseDestino -eq $prod -and -not $ConfirmarProduccion) { throw "Restaurar sobre $prod exige -ConfirmarProduccion (y detener la aplicación antes)." }

$carpeta = if ($Respaldo) { Join-Path $Base "backups\$Respaldo" } else {
    (Get-ChildItem (Join-Path $Base "backups") -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName }
$dump = Join-Path $carpeta "bd.dump"
if (-not (Test-Path $dump)) { throw "No existe $dump" }

$psql = Join-Path $PgBin "psql.exe"
$rol = $env:PIURA_DB_USER
& $psql -X -q -U $SuperUsuario -h localhost -d postgres -c "DROP DATABASE IF EXISTS $BaseDestino WITH (FORCE);"
& $psql -X -q -U $SuperUsuario -h localhost -d postgres -c "CREATE DATABASE $BaseDestino ENCODING 'UTF8' TEMPLATE template0;"
& (Join-Path $PgBin "pg_restore.exe") -h localhost -U $SuperUsuario -d $BaseDestino --no-owner --no-privileges --exit-on-error $dump
if ($LASTEXITCODE -ne 0) { Write-Registro "ERROR" "pg_restore de $(Split-Path $carpeta -Leaf) en $BaseDestino falló."; exit 1 }

# Permisos mínimos del rol de la aplicación (igual que preparar_bd.ps1)
$permisos = @"
REVOKE CONNECT ON DATABASE $BaseDestino FROM PUBLIC;
GRANT CONNECT ON DATABASE $BaseDestino TO $rol;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO $rol;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $rol;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $rol;
"@
$permisos | & $psql -X -q -U $SuperUsuario -h localhost -d $BaseDestino

$conteo = (& $psql -X -tA -U $SuperUsuario -h localhost -d $BaseDestino -c "SELECT count(*) || ' reportes, ' || (SELECT count(*) FROM usuario_municipal) || ' usuarios, ' || (SELECT count(*) FROM historial_estado) || ' movimientos' FROM reporte;")
Write-Registro "INFO" "Backup $(Split-Path $carpeta -Leaf) restaurado en $BaseDestino ($conteo)."

if ($ConFotos -and (Test-Path (Join-Path $carpeta "fotos.zip"))) {
    $destinoFotos = if ($BaseDestino -eq $prod) { Join-Path $Base "fotos" } else { Join-Path $Base "restauracion\fotos" }
    Expand-Archive -Path (Join-Path $carpeta "fotos.zip") -DestinationPath $destinoFotos -Force
    Write-Registro "INFO" "Fotos restauradas en $destinoFotos ($(@(Get-ChildItem $destinoFotos -File).Count) archivos)."
}
exit 0
