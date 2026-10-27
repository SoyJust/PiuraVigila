# PiuraVigila

Portal web para reportar incidencias urbanas en Piura (baches, luminarias, basura, etc.) y que el personal municipal las gestione. Proyecto Integrador · UTP · APF3 · Semanas 9 a 11.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 (Web, Thymeleaf, JDBC) |
| Acceso a datos | `JdbcTemplate` (sin ORM) |
| Base de datos | PostgreSQL 15+ |
| Seguridad | BCrypt (`spring-security-crypto`), token CSRF en el panel y formularios de reportes |
| Pruebas | JUnit 5 (`spring-boot-starter-test`) |
| Build | Maven |

## Arquitectura

Arquitectura en capas: **Presentación → Controlador → Servicio → DAO → Persistencia**.

| Capa | Ubicación | Contenido |
|---|---|---|
| Presentación | `src/main/resources/templates`, `static/css` | Vistas Thymeleaf y estilos |
| Controlador | `controller/` | `ReporteController`, `PanelController`, `Csrf`, `ManejoErrores` |
| Servicio | `service/` | `ReporteService`, `DashboardService`, `AuthService`, `CategoriaService`, `ValidadorReporte`, `ControlAntiSpam`, `FotoStorage` |
| DAO | `dao/` (contratos), `dao/jdbc/` (implementación) | `ReporteDAO`, `ZonaDAO`, `CategoriaDAO`, `UsuarioMunicipalDAO`, `HistorialEstadoDAO` |
| Persistencia | `database/` | Esquema, datos iniciales y consultas del dashboard |

Paquete base: `pe.edu.utp.piuravigila` (`model`, `dto`, `dao`, `service`, `controller`, `config`, `app`).

## Estructura del repositorio

```
├── database/
│   ├── 01_schema.sql              # tablas, índices y secuencia del código de reporte
│   ├── 02_seed.sql                # zonas (distritos), categorías y usuarios admin y operador
│   ├── 03_consultas_dashboard.sql # consultas R01–R03 del panel
│   └── 04_datos_demo.sql          # datos ficticios opcionales para demostración
├── src/main/java/...              # código fuente por capas
├── src/main/resources/            # plantillas, CSS y application.properties
├── src/test/java/...              # pruebas del servicio y del panel
├── evidencias/                    # evidencias de pruebas
└── pom.xml
```

## Modelo de datos

Tablas: `zona`, `categoria`, `usuario_municipal`, `reporte`, `historial_estado`. Secuencia `seq_codigo_reporte` para el código `PR-AAAA-####`.

Las zonas son distritos de Piura: Piura, Castilla, Veintiséis de Octubre, Catacaos, La Arena y La Unión. Para agregar otra basta un `INSERT INTO zona (nombre, sector) VALUES ('Nombre', NULL);` y aparece en el formulario.

## Rutas

| Ruta | Método | Descripción |
|---|---|---|
| `/reportar` | GET, POST | Formulario público de registro de incidencia, con selector de ubicación en el mapa |
| `/reportar/duplicado` | POST | Vincular un reporte a un caso existente |
| `/consultar` | GET | Consulta de estado por código, con la línea de tiempo del caso |
| `/login`, `/logout` | GET, POST | Acceso del personal municipal |
| `/panel` | GET | Panel municipal (tabla y dashboard) |
| `/panel/mapa` | GET | Mapa de zonas críticas (P04) por periodo: conteo por zona y reportes ubicados en el mapa |
| `/panel/estado` | POST | Cambiar el estado de un caso |
| `/panel/categoria` | POST | Habilitar una categoría (solo administrador) |

## Requisitos

- JDK 21
- Maven 3.9+
- PostgreSQL 15+

## Instalación y ejecución

1. Crear la base de datos y cargar los scripts (con codificación UTF-8):

```powershell
$env:PGCLIENTENCODING="UTF8"
psql -U postgres -c "CREATE DATABASE piuravigila ENCODING 'UTF8' TEMPLATE template0;"
psql -U postgres -d piuravigila -f database/01_schema.sql
psql -U postgres -d piuravigila -f database/02_seed.sql
```

Opcional: cargar datos de demostración (ficticios) para ver el panel y el mapa con contenido. Se ejecuta una sola vez; al final del archivo están las instrucciones para quitarlos.

```powershell
psql -U postgres -d piuravigila -f database/04_datos_demo.sql
```

2. Definir las variables de entorno:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PIURA_DB_USER` | Sí | Usuario de PostgreSQL |
| `PIURA_DB_PASSWORD` | Sí | Contraseña de PostgreSQL |
| `PIURA_DB_URL` | No | Por defecto `jdbc:postgresql://localhost:5432/piuravigila` |
| `PIURA_ADMIN_PASSWORD` | Solo la primera vez | Contraseña inicial de `admin`; se guarda como hash BCrypt al arrancar |
| `PIURA_OPERADOR_PASSWORD` | Solo la primera vez | Contraseña inicial de `operador`; se guarda como hash BCrypt al arrancar |
| `PIURA_FOTOS_DIR` | No | Carpeta de fotos, por defecto `fotos` |

3. Ejecutar:

```powershell
$env:PIURA_DB_USER="postgres"
$env:PIURA_DB_PASSWORD="<contraseña>"
$env:PIURA_ADMIN_PASSWORD="<contraseña del admin>"
$env:PIURA_OPERADOR_PASSWORD="<contraseña del operador>"
mvn spring-boot:run
```

4. Abrir http://localhost:8080/reportar. El panel está en `/login` (usuarios `admin` y `operador`) y el mapa de zonas críticas en `/panel/mapa`.

Las contraseñas nunca se guardan en el código ni en el repositorio.

## Pruebas

```powershell
mvn test
```

Las pruebas unitarias cubren validación de reportes, control anti-spam, almacenamiento de fotos y los servicios, incluida la línea de tiempo de la consulta de estado (con DAOs simulados). El panel (sesión, token CSRF, rol de administrador y R02) se prueba con MockMvc en `PanelControllerTest`. Ver `evidencias/`.

## Funcionalidades

- Registro de incidencias con categoría, zona (distrito), descripción (máx. 300), foto JPG/PNG real (máx. 2 MB y 20 megapíxeles), ubicación opcional (se marca en el mapa o con «Usar mi ubicación») y verificación aritmética.
- Código de seguimiento `PR-AAAA-####` y consulta pública de estado con línea de tiempo (Recibido → En proceso → Atendido) y la fecha de cada paso.
- Detección de posibles duplicados y vinculación a un caso original.
- Control anti-spam por dispositivo: 60 s entre reportes y máximo 3 por día.
- Panel municipal con login (BCrypt), cambio de estado con historial y habilitación de categorías (solo administrador).
- Dashboard: reportes por zona, tiempo de atención por categoría (R02, solo administrador) y resumen de gestión.
- Mapa de zonas críticas (P04) para 7 días, mes, 3 meses y año: cuadrícula con el conteo por zona y mapa con cada reporte ubicado, coloreado por estado. Lo ven operador y administrador.
- Registro de eventos en `logs/piuravigila.log`.

## Limitaciones conocidas

- El "dispositivo" se identifica con una cookie anónima; borrarla reinicia los límites anti-spam. Es una medida simple, no de seguridad.
- La verificación anti-spam es una suma simple y la cookie se puede borrar; no constituye una protección robusta contra bots.
- El código `PR-AAAA-####` usa 4 dígitos; pasado el 9999 se truncaría.
- La foto se guarda en disco antes de confirmar la transacción; si falla la base de datos puede quedar un archivo huérfano.
- Los usuarios `admin` y `operador` quedan con hash pendiente en el seed: sin su variable de entorno no se puede iniciar sesión con ese usuario.
- El selector de ubicación y el mapa del panel cargan Leaflet y los mosaicos de OpenStreetMap por internet; sin conexión no se dibujan (el formulario sigue funcionando, la ubicación es opcional).
- Los datos son simulados; no registrar datos personales reales. En `04_datos_demo.sql` las coordenadas son aproximadas.

## Autores

Proyecto Integrador I · UTP Sede Piura.

## Revisión 3: librerías, exportación y seguridad

- Guava 33.4.8-jre: `FotoStorageDisco` calcula SHA-256 de cada foto guardada y lo registra junto al nombre aleatorio. Sirve para comparar integridad; no cifra ni anonimiza la foto.
- Apache POI 5.4.1: `ExportacionService` genera XLSX de conteos por zona. El panel ofrece **Descargar zonas en Excel**. Usa el periodo del resumen cuando está seleccionado; por defecto, hoy menos 30 días hasta hoy. No aplica los filtros de zona/categoría de la tabla.
- Apache Commons Lang 3.20.0: `ValidadorReporte` comprueba descripciones nulas, vacías o compuestas solo por espacios con `StringUtils.isBlank`.
- Logback: dependencia explícita con versión administrada por Spring Boot; salida SLF4J a `logs/piuravigila.log`. Registra la huella de fotos y las exportaciones, sin contraseñas ni contenido de las fotos.
- `/panel/exportar` exige sesión municipal, rechaza periodos invertidos o mayores de cinco años y devuelve `Cache-Control: no-store`. Exporta solo agregados, por lo que pueden usarlo operador y administrador.
- Los POST de registro y confirmación de duplicado exigen el token CSRF de la sesión. Un rechazo devuelve HTTP 403.
- Las fotos se decodifican antes de guardarse: contenido y extensión deben coincidir. Solo JPG/PNG. WEBP se retiró porque ImageIO estándar no lo decodifica.
- Coordenadas NaN e infinitas son rechazadas en el servidor.

### Validación y evidencias

Consulta `evidencias/REV3_VALIDACION.md` y `docs/REV3_GUIA.md`. Las capturas anteriores corresponden a rev2, no prueban estos cambios. Ejecuta `mvn clean test` con JDK 21 en tu equipo y demuestra también la aplicación con PostgreSQL antes de entregar.

El arnés antiguo es histórico: no compila las dependencias nuevas y no reemplaza Maven/JUnit. No se afirma un ciclo TDD retroactivo. La arquitectura se sustenta con MVC, DAO, responsabilidad única e inversión de dependencias.
