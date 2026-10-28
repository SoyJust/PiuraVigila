# Crea (o completa) la rama feature/apf3-rev1 con los commits del avance sobre el codigo v0.8.
# Se puede ejecutar varias veces: salta los commits que ya existen y agrega solo los que faltan.
# Uso (PowerShell), desde la carpeta del repositorio:
#   powershell -ExecutionPolicy Bypass -File C:\ruta\parches_git\aplicar_commits.ps1
#   (si el proyecto esta en una subcarpeta, agrega su nombre al final)
# Este archivo es solo ASCII a proposito: los mensajes de commit viven en los .msg (UTF-8).
param([string]$Sub = "")
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"
$Rama = "feature/apf3-rev1"
$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Raiz = if ($Sub) { $Sub } else { "." }
if (-not (Test-Path "$Raiz/pom.xml")) { Write-Host "No encuentro $Raiz/pom.xml. Ejecuta el script desde la raiz del repositorio."; exit 1 }
git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) { Write-Host "Esto no es un repositorio Git."; exit 1 }
if (git status --porcelain) { Write-Host "Hay cambios sin confirmar. Haz commit o git stash antes de seguir."; exit 1 }
$actual = (git rev-parse --abbrev-ref HEAD).Trim()
if ($actual -ne $Rama) {
  git checkout -b $Rama
  if ($LASTEXITCODE -ne 0) { Write-Host "La rama $Rama ya existe. Ejecuta: git checkout $Rama   y vuelve a correr el script."; exit 1 }
}
$hechos = @(git log --format=%s)
function Aplicar($nombre) {
  $msg = (Get-Content -LiteralPath "$Dir/$nombre.msg" -Encoding UTF8 -TotalCount 1).Trim()
  if ($hechos -contains $msg) { Write-Host "YA ESTA  $nombre"; return }
  $opts = @("apply", "--index", "--ignore-whitespace")
  if ($Sub) { $opts += "--directory=$Sub" }
  $opts += "$Dir/$nombre.patch"
  & git @opts
  if ($LASTEXITCODE -ne 0) { Write-Host "FALLO al aplicar $nombre. Pegame este mensaje y el resultado de: git log --oneline -10"; exit 1 }
  git commit -q -F "$Dir/$nombre.msg"
  if ($LASTEXITCODE -ne 0) { Write-Host "FALLO al crear el commit de $nombre"; exit 1 }
  Write-Host "OK       $nombre"
}
Aplicar "0001-fix-panel-rol-administrador"
Aplicar "0002-feat-seed-usuario-operador"
Aplicar "0003-feat-panel-mapa-p04"
Aplicar "0004-test-panel-mockmvc"
Aplicar "0005-docs-readme-arnes"
Aplicar "0006-fix-login-crear-sesion"
Aplicar "0007-test-login-sin-sesion"
Aplicar "0008-fix-ui-contraste-enlaces"
git log --oneline -9
