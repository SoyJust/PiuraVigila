<#
Comprobaciones del despliegue. El certificado se valida contra <Base>\certs\piuravigila.crt (sin -k).
Devuelve código 0 si todo pasa.
#>
param(
    [string]$Base = "C:\PiuraVigila-deploy",
    [string]$Https = "https://localhost:8443",
    [string]$Http = "http://localhost:8080"
)
$ca = Join-Path $Base "certs\piuravigila.crt"
$resultados = New-Object System.Collections.ArrayList

function Test-Paso([string]$Id, [string]$Nombre, [string[]]$CurlArgs, [string]$Patron) {
    $salida = ((& curl.exe -s --max-time 10 --cacert $ca @CurlArgs) -join " ") -replace "\s+", " "
    $pasa = $salida -match $Patron
    $estado = if ($pasa) { "PASS" } else { "FAIL" }
    $obtenido = $salida.Trim()
    if ($pasa -and $obtenido.Length -gt 160 -and $Matches[0]) { $obtenido = $Matches[0] }
    [void]$resultados.Add([pscustomobject]@{ Id = $Id; Comprobacion = $Nombre; Estado = $estado; Obtenido = $obtenido })
}

Test-Paso "HC-01" "Proceso web (/actuator/health)" @("-w", " HTTP %{http_code}", "$Https/actuator/health") '"status":"UP".*HTTP 200'
Test-Paso "HC-02" "PostgreSQL (/actuator/health/db)" @("-w", " HTTP %{http_code}", "$Https/actuator/health/db") '"status":"UP".*HTTP 200'
Test-Paso "HC-03" "Versión desplegada (/actuator/info)" @("$Https/actuator/info") '^(?=.*"commit":\{"id":"[0-9a-f]+").*"version":"[0-9.]+"'
Test-Paso "HC-04" "Página de inicio por HTTPS" @("-o", "NUL", "-w", "HTTP %{http_code}", "$Https/") '^HTTP 200$'
Test-Paso "HC-05" "HTTP redirige a HTTPS" @("-o", "NUL", "-w", "HTTP %{http_code} -> %{redirect_url}", "$Http/") '^HTTP 30[12] -> https://'
Test-Paso "HC-06" "Cabecera HSTS" @("-o", "NUL", "-D", "-", "$Https/") '(?i)strict-transport-security: max-age=31536000; includeSubDomains'
Test-Paso "HC-07" "Endpoints internos ocultos (/actuator/env)" @("-o", "NUL", "-w", "HTTP %{http_code}", "$Https/actuator/env") '^HTTP 404$'

$resultados | Format-Table -AutoSize -Wrap
$fallos = @($resultados | Where-Object { $_.Estado -eq "FAIL" }).Count
Write-Output ("{0} de {1} comprobaciones en PASS" -f ($resultados.Count - $fallos), $resultados.Count)
exit $fallos
