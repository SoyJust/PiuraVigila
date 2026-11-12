# Semana 14 · Pruebas de seguridad y avance al 70 %

PiuraVigila · Curso Integrador I: Sistemas - Software · 2026-2 · 12/11/2026

Alcance autorizado: solo la aplicación propia en `http://localhost:8080`. ZAP se usó en modo pasivo (sin escaneo activo).

## Ciclo seguido

`línea inicial (FAIL)` → `hallazgo` → `corrección en commit` → `reprueba (misma prueba + regresión)` → `evidencia`

| Paso | Commit | Resultado |
|---|---|---|
| Línea inicial del código | `fef1156` (main tras la Semana 13) | ZAP: 53 alertas de 9 tipos · OSV: 23 vulnerabilidades en dependencias |
| Pruebas de seguridad en rojo | `7785a7e` | 5 de 5 pruebas nuevas fallan (`S14-E4`) |
| Correcciones | `4a251a3` | cabeceras, CSRF en login, SameSite, SRI, scripts a `/js`, dependencias parche |
| Corrección de la reprueba | `c8648c4` | estilos en línea a clases CSS → CSP sin `unsafe-inline` |
| Reprueba final | rama `feature/s14-seguridad` | 67 unitarias + 7 de integración en verde · ZAP: solo 2 alertas informativas · OSV: 2 no aplicables |

## Hallazgos

| ID | Hallazgo | Severidad | Estado |
|---|---|---|---|
| SEC-01 | Sin Content-Security-Policy | Media | Cerrado |
| SEC-02 | Sin anti-clickjacking | Media | Cerrado |
| SEC-03 | Sin X-Content-Type-Options | Baja | Cerrado |
| SEC-04 | Leaflet sin SRI y scripts en línea | Media | Cerrado |
| SEC-05 | Cookies sin SameSite | Baja | Cerrado |
| SEC-06 | Login sin token CSRF | Media | Cerrado |
| SEC-07 | 23 vulnerabilidades conocidas en dependencias | Alta | Cerrado (2 no aplicables, aceptadas) |
| SEC-08 | CSP con `style-src 'unsafe-inline'` (detectado en la reprueba) | Media | Cerrado |
| SEC-09 | `.gitignore` excluía el paquete Java `config/` | Baja | Cerrado |

## Archivos

| Archivo | Contenido |
|---|---|
| `PiuraVigila_Reporte_Pruebas_Seguridad_Semana14.docx` | Reporte sobre la plantilla del curso: identificación, alcance, controles, 14 pruebas, 9 hallazgos, dependencias, ZAP, riesgos y conclusión |
| `Evidencias/S14-E1_cabeceras_inicial.txt` | Cabeceras HTTP y reflejo de entrada antes de corregir |
| `Evidencias/S14-E2_inicial_zap_*.json/.html` | Alertas y reporte de ZAP pasivo antes de corregir |
| `Evidencias/S14-E3_SCA_OSV_inicial.*` | Dependencias con vulnerabilidades (OSV) antes de corregir |
| `Evidencias/S14-E4_pruebas_seguridad_ROJO.txt` | Las 5 pruebas de seguridad nuevas fallando (línea inicial) |
| `Evidencias/S14-E5_SCA_OSV_reprueba.*` | Dependencias después de actualizar (2 no aplicables) |
| `Evidencias/S14-E6_cabeceras_reprueba.txt` | Cabeceras finales, login sin token → 403, salida escapada |
| `Evidencias/S14-E7_reprueba_zap_*.json/.html` | ZAP pasivo final (solo alertas informativas) |
| `Evidencias/S14-E8_navegador_CSP.txt` | El mapa de P01 y P04 funciona con la CSP estricta, sin violaciones en consola |
| `Evidencias/S14-E9_log_sin_secretos.txt` | El log no guarda contraseñas ni tokens |
| `Evidencias/S14-E10_mvn_verify_final.txt` | Ejecución final de todas las pruebas |

En las evidencias se reemplazaron los ids de sesión, los tokens CSRF y la cookie de dispositivo por `<valor omitido>`.

## Cómo repetir

```powershell
mvn test                                   # incluye SeguridadWebTest (TC-SEC-04, 05, 06, 08 y 10)
$env:RUN_DB_TESTS="true"; mvn verify       # + TC-SEC-07 (inyección SQL) contra PostgreSQL
$env:NVD_API_KEY="<clave del NVD>"; mvn -Psecurity-sca verify   # Dependency-Check (requiere clave gratuita del NVD)
```

ZAP 2.17.0: abrir la app en `http://localhost:8080`, usar *Manual Explore* (o un navegador configurado con el proxy de ZAP), navegar el flujo real y revisar *Alerts*. No hacer escaneo activo sobre sitios de terceros.

## Revisión final (12/11/2026, después del PR #9)

En una prueba de punta a punta (registrar reporte → consultar código → login del panel → cambiar estado → resumen R01–R03 → mapa → exportar Excel → cerrar sesión) aparecieron dos detalles más, ya corregidos y con prueba:

| ID | Hallazgo | Corrección | Prueba |
|---|---|---|---|
| SEC-10 | Al redirigir al login, Tomcat ponía el id de sesión en la URL (`/login;jsessionid=…`), visible en el historial y en los registros | `server.servlet.session.tracking-modes=cookie` | Reprueba manual: la redirección ahora es `/login?requerida` |
| SEC-11 | La CSP bloqueaba la hoja de Google Fonts que importa el CSS; la fuente Atkinson Hyperlegible no cargaba | `style-src` permite `https://fonts.googleapis.com` y `font-src` permite `https://fonts.gstatic.com` (los scripts siguen restringidos) | TC-SEC-05b (`SeguridadWebTest`), falla sin la corrección y pasa con ella; en el navegador la fuente carga sin violaciones de CSP |

Resultado: 68 pruebas unitarias + 7 de integración, 0 fallos. El reporte .docx describe la reprueba del PR #9; estos dos hallazgos se documentan aquí.

## Pendiente para la Semana 15 (despliegue)

HTTPS con HSTS y cookies `Secure`, Dependency-Check con la clave del NVD, y planificar la migración a Spring Boot 4 (riesgo R1).
