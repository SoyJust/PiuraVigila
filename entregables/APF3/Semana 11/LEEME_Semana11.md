# Semana 11 · PiuraVigila · avance 40 %, rev2 y rev3

El codigo vive solo en el repositorio (`PiuraVigila`, ramas `feature/apf3-rev1`, `feature/apf3-rev2` y `feature/apf3-rev3`); APF3.zip contiene solo documentos.

Repositorio: https://github.com/SoyJust/PiuraVigila (público, rama `main`, commits base `21ef549` y `8ba19aa`, merge `0186074`; tag `semana11-40pct`; 11 commits al cierre del hito 40 %). Con el incremento rev2: PR #2, merge `d592b35`, tag `semana11-100pct` y 16 commits en total. Con rev3: PR #3, commit `52468ca`, merge `9d70079`, tag `apf3-rev3` y 18 commits en total.

Contenido del zip:
- `REV3_GUIA.md`, `REV3_VALIDACION.md` y `REV3_maven_test.txt`: la guía de demostración de la revisión 3, su validación y la salida de la corrida de Maven hecha al prepararla.
- `PiuraVigila_Semana_11_Control_Versiones_rev3.xlsx`: la plantilla rellenada con lo que se sabe del proyecto y de tu repositorio. Conserva el registro del hito 40 % y de rev2 y agrega rev3 (commits 52468ca y 9d70079, PR #3, 5 filas de trazabilidad y la hoja `Checklist_Rev3`). Las hojas `Checklist_40` y `Checklist_Rev2` quedan como historia.
- `parches_git/`: los 8 commits del hito 40 % como parches (`0001` a `0008`) y los scripts que los aplican sobre tu `main`. Los 4 commits de rev2 ya están en GitHub; si necesitas parches, se generan con `git format-patch 0186074..b3892b5`.

## 1. Crear la rama y los 8 commits (o completar los que falten)
Desde la carpeta de tu copia local del repositorio (donde está `pom.xml`), con `main` al día. Usa **una** de las dos:

```
git checkout main
git pull

# Git Bash / Linux / Mac
bash ruta/a/parches_git/aplicar_commits.sh

# PowerShell
powershell -ExecutionPolicy Bypass -File ruta\a\parches_git\aplicar_commits.ps1
```
El script se puede ejecutar más de una vez: salta los commits que ya existen (`YA ESTA`) y agrega solo los que faltan. Si un parche falla, pégame el mensaje.

El commit `docs` edita **tu** README final (no lo reemplaza): agrega `operador`, `PIURA_OPERADOR_PASSWORD`, la ruta `/panel/mapa`, el rol de administrador y las pruebas del panel, y quita las dos limitaciones que dejan de ser ciertas. También corrige dos frases que no coincidían con el código: «recategorización» (no existe esa función) y que `/panel/categoria` «cambia la categoría de un caso» (en realidad habilita una categoría nueva).

## 2. Evidencia funcional (antes de integrar)
```
psql -U postgres -c "CREATE DATABASE piuravigila ENCODING 'UTF8' TEMPLATE template0;"
psql -U postgres -d piuravigila -f database/01_schema.sql
psql -U postgres -d piuravigila -f database/02_seed.sql
$env:PIURA_DB_USER="postgres"; $env:PIURA_DB_PASSWORD="tu_clave"
$env:PIURA_ADMIN_PASSWORD="elige_una"; $env:PIURA_OPERADOR_PASSWORD="elige_otra"
mvn test
mvn spring-boot:run
```
Capturas: `mvn test`, `/login` con `admin` y con `operador`, y `/panel/mapa`.

## 3. Subir, revisar e integrar (solo con la evidencia validada)
```
git push -u origin feature/apf3-rev1
```
En GitHub abre el Pull Request `feature/apf3-rev1` → `main`. Que lo revise un integrante que no escribió el cambio (revisa el cambio y su evidencia, no solo que compile). Después:
```
git checkout main
git pull
git merge --no-ff feature/apf3-rev1
git push origin main
git tag -a semana11-40pct -m "Avance 40 %: rol administrador, operador y mapa P04"
git push origin semana11-40pct
```

## 4. Excel completado con datos reales (hecho)
```
git log -8 --reverse --pretty=format:"%h | %ad | %an | %s" --date=short feature/apf3-rev1
```

| Hoja y celdas | Qué poner |
|---|---|
| Commits A7:C14 | Hash, fecha y autor de los 8 commits nuevos (en el orden de la salida) |
| Commits H7:H14 | Resultado real de cada commit (completado con las capturas y el mvn test de 42 pruebas) |
| Trazabilidad F5:F9 | Hash del commit correspondiente; J5:J9 → «Trazado» cuando haya evidencia |
| Ramas_PR D5, F5, G5, H5, J5 | Responsable, estado (Abierta, En revisión o Fusionada), quién revisó, conflictos (No / Sí - resuelto) y URL del PR |
| Checklist_40 filas 6 a 8, 11 y 16 a 19 | Sí/No, evidencia y semáforo |

## Qué se verificó
- `mvn test` con JUnit real: 42 pruebas ejecutadas y aprobadas, 0 fallos, BUILD SUCCESS (24/10/2026, captura EV-14). Incluye las 7 pruebas de `PanelControllerTest` (PV-T43 incluida).
- Evidencias EV-06 a EV-14 Validadas (BD, log, P01 a P04, hash BCrypt, control de rol y `mvn test`).
- PR #1 revisado, fusionado en `main` (merge `0186074`) y tag `semana11-40pct` publicado en GitHub; 11 commits en total.
- Sin contraseñas ni tokens asignados en el código; `target/`, `logs/`, `fotos/` e `.idea/` no están versionados.
- Fuera de este avance: pruebas de integración con PostgreSQL (PV-T36 a PV-T38 y PV-T40), que siguen «Por diseñar».
- No copies el `README.md` del zip de la Semana 10 sobre tu repositorio: el de tu repo es el bueno.

## 5. Incremento rev2 (25/10/2026)
Cambios: mapa P04 con puntos (RF-04), línea de tiempo en Consultar (RF-02), zonas por distritos reales, datos de demostración (`database/04_datos_demo.sql`) y `config/` y `uploads/` fuera de GitHub. Se hizo en la rama `feature/apf3-rev2`, creada desde `main` (base `0186074`):

```
Add-Content .gitignore "`nconfig/`nuploads/"
git checkout -b feature/apf3-rev2
git add .gitignore
git commit -m "chore: ignorar config/ y uploads/"                                   # 043d302
git add src
git commit -m "feat: mapa de reportes con ubicacion, linea de tiempo en Consultar y mejoras de interfaz"   # 32bcc2b
git add database README.md
git commit -m "feat: zonas por distritos reales, datos de demostración y README actualizado"                # b64ffd2
git add README.md
git commit -m "docs: actualizar README (zonas, datos de demo, mapa y linea de tiempo)"                      # b3892b5
git push -u origin feature/apf3-rev2
```
En GitHub: PR #2 `feature/apf3-rev2` → `main`, «Merge pull request» (merge `d592b35`). Después, la etiqueta `semana11-100pct` sobre `d592b35` y `git push origin semana11-100pct`.

Para una base ya creada, las zonas se renombraron con un `UPDATE` (el seed solo corre en bases nuevas) y los datos de demostración se cargan una sola vez:
```
psql -U postgres -d piuravigila -f database/04_datos_demo.sql
```

Excel (rev2): `Commits` filas 16 a 20 (043d302, 32bcc2b, b64ffd2, b3892b5, d592b35), `Ramas_PR` fila 6, `Trazabilidad` filas 10 a 14 y la hoja `Checklist_Rev2`. Los hashes salen de:
```
git log -8 --reverse --pretty=format:"%h | %ad | %an | %s" --date=short
```

**Revisión del PR #2:** lo fusionó su autor (SoyJust) el 25/10/2026 y fue revisado y aprobado en GitHub por `justzonagamer-rgb` el 26/10/2026 (revisión posterior a la fusión). Está registrado en `Ramas_PR` (G6) y en `Checklist_Rev2` (fila 17: «Sí» y «Verde»).

## Qué se verificó en rev2
- `mvn test` con JUnit real: 44 pruebas ejecutadas y aprobadas, 0 fallos, BUILD SUCCESS (25/10/2026 12:08, captura EV-16). Incluye PV-T44 y PV-T45.
- Mapa con zonas por distrito y 15 puntos en 7 días (EV-17) y línea de tiempo en Consultar (EV-18).
- `config/` y `uploads/` agregados al `.gitignore` antes de subir los cambios; el seed solo trae `HASH_PENDIENTE` y los datos de demostración son ficticios.

## 6. Incremento rev3 (27/10/2026)
Cambios: librerías de apoyo (Guava, Apache POI, Commons Lang 3 y Logback), exportación de zonas a Excel, token CSRF en los formularios públicos y validación de fotos. Rama `feature/apf3-rev3`, creada desde `main` (base `d592b35`):

```
git switch main
git pull
git switch -c feature/apf3-rev3
# copiar el contenido actualizado del proyecto sobre el clon (sin tocar .git ni config/)
git status
mvn clean test                      # 54 pruebas, 0 fallos (EV-21)
git add pom.xml src .github README.md docs database evidencias
git commit -m "feat: exportacion Excel, librerias (Guava, POI, Commons, Logback) y validaciones de seguridad"   # 52468ca
git push -u origin feature/apf3-rev3
```
En GitHub: PR #3 `feature/apf3-rev3` → `main`, «Merge pull request» (merge `9d70079`). Después, la etiqueta `apf3-rev3` sobre el merge.

Excel (rev3): `Commits` filas 21 y 22, `Ramas_PR` fila 7, `Trazabilidad` filas 15 a 19 y la hoja `Checklist_Rev3`. Los hashes salen de:
```
git log -8 --reverse --pretty=format:"%h | %ad | %an | %s" --date=short
```

**Revisión del PR #3:** lo revisó y fusionó su autor (SoyJust); no hay revisión de otro integrante y así está registrado en `Ramas_PR` (G7) y en `Checklist_Rev3` (fila 17: «No», Amarillo). **Opcional:** una captura del Excel descargado desde el panel pasaría FV-07 de Amarillo a Verde.

## Qué se verificó en rev3
- `mvn clean test` en la laptop del equipo: 54 pruebas ejecutadas y aprobadas, 0 fallos, BUILD SUCCESS (27/10/2026 16:03, EV-21). Segunda corrida independiente con JDK 17: 54 pruebas, 0 fallos (EV-22).
- Las cuatro librerías están declaradas en el `pom.xml` y se usan en el código (EV-25).
- `config/` y `uploads/` siguen fuera de GitHub; los datos de demostración son ficticios y el Excel exportado solo lleva conteos por zona.

