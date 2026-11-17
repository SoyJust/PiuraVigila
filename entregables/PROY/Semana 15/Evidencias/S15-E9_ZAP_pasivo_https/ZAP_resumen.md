# S15-E9 · ZAP pasivo contra el despliegue HTTPS

- Fecha: 17/11/2026. Herramienta: OWASP ZAP 2.17.0 en modo daemon, **solo spider y escaneo pasivo** (sin escaneo activo).
- Objetivo autorizado: `https://localhost:8443/` (versión desplegada v1.3.0). Se recorrieron 17 URL, incluidas `/consultar`, `/reportar`, `/login`, `/actuator/health` e `/actuator/info`.
- Reporte completo: `ZAP_reporte_pasivo_https.html`.

| Riesgo | Alertas |
|---|---|
| Alto | 0 |
| Medio | 0 |
| Bajo | 0 |
| Informativo | 16 (3 tipos) |

| Alerta informativa | Id | URL | Lectura |
|---|---|---|---|
| Re-examine Cache-control Directives | 10015 | 8 (inicio, actuator, recursos) | Las páginas públicas no tienen datos personales; la exportación del panel ya responde con `no-store` (S14). |
| Session Management Response Identified | 10112 | 2 (`/login`, `/reportar`) | ZAP solo identifica la cookie `JSESSIONID`, que ahora lleva `Secure`, `HttpOnly` y `SameSite=Lax`. |
| User Controllable HTML Element Attribute (Potential XSS) | 10031 | 4 (`/consultar`, `/login`) | El código consultado vuelve al campo del formulario. Thymeleaf lo escapa (`th:value`) y la CSP bloquea scripts en línea; TC-SEC de S14 lo cubre. |

Comparación con la Semana 14 (línea inicial): 53 alertas de 9 tipos, entre ellas falta de CSP, anti-clickjacking y nosniff. En la versión desplegada ya no quedan alertas de riesgo bajo o mayor, y HSTS se emite en todas las respuestas HTTPS.
