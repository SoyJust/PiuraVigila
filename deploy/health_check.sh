#!/usr/bin/env bash
# Comprobaciones del despliegue (equivalente a windows/health_check.ps1).
# Uso: CA=/ruta/piuravigila.crt ./health_check.sh [https://localhost:8443] [http://localhost:8080]
HTTPS=${1:-https://localhost:8443}
HTTP=${2:-http://localhost:8080}
CA=${CA:-/c/PiuraVigila-deploy/certs/piuravigila.crt}
fallos=0

probar() { # id, nombre, patrón, argumentos de curl...
  local id=$1 nombre=$2 patron=$3; shift 3
  local salida
  salida=$(curl -s --max-time 10 --cacert "$CA" "$@" | tr -s '\r\n' '  ')
  if grep -qiE "$patron" <<<"$salida"; then
    echo "PASS  $id  $nombre"
  else
    echo "FAIL  $id  $nombre  -> $salida"; fallos=$((fallos+1))
  fi
}

probar HC-01 "Proceso web"             '"status":"UP".*HTTP 200' -w ' HTTP %{http_code}' "$HTTPS/actuator/health"
probar HC-02 "PostgreSQL"              '"status":"UP".*HTTP 200' -w ' HTTP %{http_code}' "$HTTPS/actuator/health/db"
probar HC-03 "Versión desplegada"      '"commit".*"version":"[0-9.]+"|"version":"[0-9.]+".*"commit"' "$HTTPS/actuator/info"
probar HC-04 "Inicio por HTTPS"        '^HTTP 200$' -o /dev/null -w 'HTTP %{http_code}' "$HTTPS/"
probar HC-05 "HTTP redirige a HTTPS"   '^HTTP 30[12] -> https://' -o /dev/null -w 'HTTP %{http_code} -> %{redirect_url}' "$HTTP/"
probar HC-06 "Cabecera HSTS"           'strict-transport-security: max-age=31536000' -o /dev/null -D - "$HTTPS/"
probar HC-07 "Actuator interno oculto" '^HTTP 404$' -o /dev/null -w 'HTTP %{http_code}' "$HTTPS/actuator/env"

echo "Fallos: $fallos"
exit $fallos
