<#
Crea la base del despliegue y un rol propio con permisos mínimos (sin DDL).
Uso:  $env:PGPASSWORD = "<clave de postgres>"; .\preparar_bd.ps1 [-ConDemo]
La clave del rol se toma de PIURA_DB_PASSWORD (config\env.ps1).
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [string]$Repo = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$SuperUsuario = "postgres",
    [string]$PsqlDir = "C:\Program Files\PostgreSQL\18\bin",
    [switch]$ConDemo
)
$ErrorActionPreference = "Stop"
. (Join-Path $Base "config\env.ps1")
$psql = Join-Path $PsqlDir "psql.exe"
$env:PGCLIENTENCODING = "UTF8"
$bd = "piuravigila_prod"
$rol = $env:PIURA_DB_USER

function Invoke-Psql([string]$BaseDatos, [string]$Sql) {
    $Sql | & $psql -X -q -v ON_ERROR_STOP=1 -U $SuperUsuario -h localhost -d $BaseDatos -v "clave=$env:PIURA_DB_PASSWORD"
    if ($LASTEXITCODE -ne 0) { throw "psql terminó con código $LASTEXITCODE" }
}

$existeRol = & $psql -X -tA -U $SuperUsuario -h localhost -d postgres -c "SELECT 1 FROM pg_roles WHERE rolname = '$rol'"
if ($existeRol -ne "1") { Invoke-Psql "postgres" "CREATE ROLE $rol LOGIN PASSWORD :'clave';" }
else { Invoke-Psql "postgres" "ALTER ROLE $rol PASSWORD :'clave';" }

$existeBd = & $psql -X -tA -U $SuperUsuario -h localhost -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '$bd'"
if ($existeBd -eq "1") { throw "La base $bd ya existe; no se modifica. Bórrela a mano si quiere recrearla." }
Invoke-Psql "postgres" "CREATE DATABASE $bd ENCODING 'UTF8' TEMPLATE template0;"

Invoke-Psql $bd (Get-Content -Raw -Encoding UTF8 "$Repo\database\01_schema.sql")
Invoke-Psql $bd (Get-Content -Raw -Encoding UTF8 "$Repo\database\02_seed.sql")
if ($ConDemo) { Invoke-Psql $bd (Get-Content -Raw -Encoding UTF8 "$Repo\database\04_datos_demo.sql") }

Invoke-Psql $bd @"
REVOKE CONNECT ON DATABASE $bd FROM PUBLIC;
GRANT CONNECT ON DATABASE $bd TO $rol;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO $rol;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $rol;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $rol;
"@
Write-Output "Base $bd lista; el rol $rol solo puede leer y escribir datos."
