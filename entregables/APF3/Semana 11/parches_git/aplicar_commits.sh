#!/usr/bin/env bash
# Crea (o completa) la rama feature/apf3-rev1 con los commits del avance sobre el codigo v0.8.
# Se puede ejecutar varias veces: salta los commits que ya existen y agrega solo los que faltan.
# Uso (Git Bash / Linux / Mac), desde la carpeta del repositorio:
#   bash /ruta/a/parches_git/aplicar_commits.sh                       (si pom.xml esta en la raiz del repo)
#   bash /ruta/a/parches_git/aplicar_commits.sh PiuraVigila_Proyecto  (si el proyecto esta en una subcarpeta)
set -e
RAMA="feature/apf3-rev1"
SUB="${1:-}"
DIR="$(cd "$(dirname "$0")" && pwd)"
RAIZ="${SUB:-.}"
[ -f "$RAIZ/pom.xml" ] || { echo "No encuentro $RAIZ/pom.xml. Ejecuta el script desde la raiz del repositorio."; exit 1; }
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "Esto no es un repositorio Git."; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "Hay cambios sin confirmar. Haz commit o git stash antes de seguir."; exit 1; }
OPT=""; [ -n "$SUB" ] && OPT="--directory=$SUB"
if [ "$(git rev-parse --abbrev-ref HEAD)" != "$RAMA" ]; then
  git checkout -b "$RAMA" || { echo "La rama $RAMA ya existe. Ejecuta: git checkout $RAMA   y vuelve a correr el script."; exit 1; }
fi
HECHOS="$(git log --format=%s)"
aplicar() {
  MSG="$(head -n 1 "$DIR/$1.msg")"
  if printf '%s\n' "$HECHOS" | grep -qxF -- "$MSG"; then echo "YA ESTA  $1"; return; fi
  git apply --index --ignore-whitespace $OPT "$DIR/$1.patch" || { echo "FALLO al aplicar $1. Pegame este mensaje y el resultado de: git log --oneline -10"; exit 1; }
  git commit -q -F "$DIR/$1.msg"
  echo "OK       $1"
}
aplicar 0001-fix-panel-rol-administrador
aplicar 0002-feat-seed-usuario-operador
aplicar 0003-feat-panel-mapa-p04
aplicar 0004-test-panel-mockmvc
aplicar 0005-docs-readme-arnes
aplicar 0006-fix-login-crear-sesion
aplicar 0007-test-login-sin-sesion
aplicar 0008-fix-ui-contraste-enlaces
echo; git log --oneline -9
