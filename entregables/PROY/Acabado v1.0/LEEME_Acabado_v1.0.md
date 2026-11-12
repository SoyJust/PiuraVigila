# PiuraVigila · Acabado v1.0 (12/11/2026)

Después de las pruebas de seguridad (Semana 14) se revisó el sistema completo con capturas de cada pantalla, en escritorio y en celular. El objetivo fue que se vea y funcione como un producto terminado de nivel universitario, sin cambiar la arquitectura ni las reglas de negocio ya aprobadas. El despliegue con HTTPS queda para la Semana 15.

## Qué se encontró en la revisión inicial (capturas `Capturas/antes`)

| # | Pantalla | Problema |
|---|---|---|
| 1 | Inicio | No había página de inicio: `/` saltaba directo al formulario. Nadie explicaba qué es PiuraVigila. |
| 2 | Panel | Sin cifras: había que contar filas para saber cuántos casos faltaban atender. |
| 3 | Panel | La descripción completa hacía que cada fila ocupara 6 o 7 líneas; 20 casos llenaban varias pantallas. |
| 4 | Panel en celular | La tabla se desbordaba a la derecha y la página medía más de 15 000 px. |
| 5 | Panel | El resumen R01–R03 estaba escondido bajo un botón «Ver / ocultar resumen», en tablas planas. |
| 6 | Mapa | Fechas en formato técnico (`2026-10-13`) distinto al resto (`13/10/2026`). |
| 7 | Consulta | El ciudadano no veía el distrito ni la descripción de su propio reporte. |
| 8 | Usuarios | No había forma de crear, desactivar ni cambiar contraseñas desde la aplicación: solo por SQL. |
| 9 | Datos | 20 casos en 6 distritos: el panel, el mapa y los resúmenes se veían vacíos en una demostración. |

## Qué se cambió (capturas `Capturas/despues`)

**Interfaz**
- Página de inicio (`/`): propósito, botones para reportar y consultar, «cómo funciona» en 3 pasos, cifras públicas agregadas y tipos de problema.
- Panel con encabezado, pestañas (Casos, Mapa de zonas, Resumen, Usuarios, Mi contraseña) y 4 tarjetas de cifras que filtran por estado al hacer clic.
- Tabla de casos compacta: fecha bajo el código, distrito bajo la categoría, descripción en 2 líneas (completa al pasar el cursor) y estado cerrado indicado como «Caso cerrado».
- En celular, cada caso se muestra como una tarjeta (sin desplazamiento horizontal).
- Resumen en su propia pestaña con rango de fechas, cifras de R03, barras por distrito para R01 y la tabla R02 (solo administrador).
- Textos de ayuda en el formulario, pantalla de código con botones «Ver el estado ahora» y «Registrar otro reporte», consulta con distrito y descripción, mensajes de error con orientación, pie de página.
- Sin estilos ni scripts en línea: la CSP estricta de la Semana 14 se mantiene (las barras usan `data-i` y `js/barras.js`).

**Gestión de usuarios (nuevo)**
- El administrador crea cuentas (nombre, usuario, rol y contraseña inicial), las activa o desactiva y restablece contraseñas.
- Cada usuario cambia su propia contraseña: exige la actual, 8 caracteres como mínimo con letras y números, y confirmación.
- Desactivar en lugar de borrar: el historial conserva quién cambió cada estado. Una cuenta desactivada no inicia sesión y pierde de inmediato la sesión que tenía abierta (`SesionVigente`).
- Nadie puede desactivarse a sí mismo (evita quedarse sin administrador). Todos los formularios llevan token CSRF. El log registra quién hizo qué, nunca la contraseña.

**Distritos**
- Se pasó de 6 a los **10 distritos de la provincia de Piura** (se agregan Cura Mori, El Tallán, Las Lomas y Tambo Grande). Se mantiene la decisión del docente del 15/10/2026 de usar zonas distritales. El distrito agrupa para el mapa y los resúmenes; la ubicación exacta la dan el punto en el mapa y la referencia de la descripción, por eso no hace falta bajar a urbanizaciones.

**Base de datos**
- `usuario_municipal.activo BOOLEAN NOT NULL DEFAULT TRUE` en `01_schema.sql`.
- `02_seed.sql` con los 10 distritos.
- `05_migracion_v1_0.sql` para actualizar una base existente sin perder datos (se puede ejecutar dos veces).
- `04_datos_demo.sql`: 59 casos y 13 reportes vinculados en los 10 distritos durante 5 meses, ordenados por fecha para que los códigos queden en orden cronológico (35 atendidos, 11 en proceso, 13 recibidos).

## Pruebas

| Tipo | Antes | Ahora | Nuevas |
|---|---|---|---|
| Unitarias (`mvn test`) | 68 | 83 | `UsuarioServiceTest` (TC-U01 a U10), `UsuarioControllerTest` (TC-U11 a U15) |
| Integración con PostgreSQL (`mvn verify`, `RUN_DB_TESTS=true`) | 7 | 11 | `UsuarioMunicipalDAOJdbcIT` (TC-U16 a U19, incluye la migración 05) |

Resultado: 0 fallos (`Evidencias/AV-01_mvn_verify.txt`).

Prueba manual de punta a punta contra la aplicación (`Evidencias/AV-02_e2e_gestion_usuarios.txt`):
1. Crear un operador.
2. Iniciar sesión con él; comprobar que no entra a «Usuarios».
3. Intentar cambiar la contraseña con la actual incorrecta: se rechaza.
4. Cambiarla bien: la vieja deja de funcionar.
5. Desactivarlo: su sesión abierta se cierra y no puede volver a entrar.
6. Comprobar que el administrador no puede desactivarse a sí mismo.

También se verificó en el navegador: 0 violaciones de CSP en las páginas nuevas, la fuente carga y las páginas no se dejan incrustar en otro sitio.

## Capturas

`Capturas/antes` y `Capturas/despues` tienen las mismas pantallas, en escritorio (1366 px) y celular (390 px):

| Archivo | Pantalla |
|---|---|
| `*_01_inicio` | Inicio (antes no existía: la captura muestra el formulario al que redirigía) |
| `*_02_reportar` | P01 Registrar reporte |
| `*_03_consultar_resultado` | P02 Consulta de un caso en proceso |
| `*_04_consultar_error` | P02 Código inexistente |
| `*_05_login` | Inicio de sesión del panel |
| `*_06_panel` | P03 Panel de casos |
| `*_07_panel_resumen` | Resumen R01–R03 |
| `*_08_mapa` | P04 Mapa de zonas críticas (último mes) |
| `*_09_usuarios`, `*_10_clave` | P05 Usuarios y P06 Mi contraseña (solo «después») |

Las capturas «antes» usan los 20 casos de demostración anteriores, y las «después», los 59 actuales. Todos los datos son ficticios. El usuario «Lucía Ramírez» de las capturas es una cuenta temporal creada solo para tomarlas y borrada al terminar.

## Pendiente (Semana 15)

HTTPS con HSTS y cookies `Secure` al desplegar, y Dependency-Check con la clave del NVD.
