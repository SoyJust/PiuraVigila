<#
Construye la versión indicada desde una copia limpia del repositorio y deja el JAR en <Base>\app.
Uso:  .\desplegar.ps1 -Version v1.3.0
#>
param(
    [string]$Version = "v1.3.0",
    [string]$Base = "C:\PiuraVigila-deploy",
    [string]$Repo = "https://github.com/SoyJust/PiuraVigila.git"
)
# git y mvn escriben avisos por stderr; en PowerShell 5.1 eso no debe detener el script.
# Los fallos se detectan por el código de salida.
$ErrorActionPreference = "Continue"
. (Join-Path $Base "config\env.ps1")

$fuente = Join-Path $Base "build\$Version"
if (Test-Path $fuente) { Remove-Item -Recurse -Force $fuente }
git clone --quiet --depth 1 --branch $Version $Repo $fuente
if ($LASTEXITCODE -ne 0) { throw "No se pudo clonar $Version" }

Push-Location $fuente
try {
    mvn -B clean package
    if ($LASTEXITCODE -ne 0) { throw "mvn clean package falló" }
} finally { Pop-Location }

$jar = Get-ChildItem "$fuente\target\piuravigila-*.jar" | Select-Object -First 1
New-Item -ItemType Directory -Force "$Base\app", "$Base\logs", "$Base\fotos" | Out-Null
if (-not $jar) { throw "No se generó el JAR" }
Copy-Item $jar.FullName "$Base\app\piuravigila.jar" -Force -ErrorAction Stop
$hash = (Get-FileHash "$Base\app\piuravigila.jar" -Algorithm SHA256).Hash
$linea = "{0}  {1}  {2}  {3} bytes  SHA-256 {4}" -f (Get-Date -Format s), $Version, $jar.Name, $jar.Length, $hash
Add-Content "$Base\app\despliegues.log" $linea
Write-Output $linea
