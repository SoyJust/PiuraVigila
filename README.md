# PiuraVigila

Portal web para reportar incidencias urbanas en Piura (baches, luminarias, basura, etc.) y que el personal municipal las gestione. Proyecto Integrador · UTP · APF3 · Semana 10 (v0.8).

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 (Web, Thymeleaf, JDBC) |
| Acceso a datos | `JdbcTemplate` (sin ORM) |
| Base de datos | PostgreSQL 15+ |
| Seguridad | BCrypt (`spring-security-crypto`), token CSRF en el panel |
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
│   ├── 02_seed.sql                # zonas, categorías y usuario admin
│   └── 03_consultas_dashboard.sql # consultas R01–R03 del panel
├── src/main/java/...              # código fuente por capas
├── src/main/resources/            # plantillas, CSS y application.properties
├── src/test/java/...              # pruebas unitarias del servicio
├── evidencias/                    # evidencias de pruebas
└── pom.xml
```

## Modelo de datos

Tablas: `zona`, `categoria`, `usuario_municipal`, `reporte`, `historial_estado`. Secuencia `seq_codigo_reporte` para el código `PR-AAAA-####`.

## Rutas

| Ruta | Método | Descripción |
|---|---|---|
| `/reportar` | GET, POST | Formulario público de registro de incidencia |
| `/reportar/duplicado` | POST | Vincular un reporte a un caso existente |
| `/consultar` | GET | Consulta de estado por código |
| `/login`, `/logout` | GET, POST | Acceso del personal municipal |
| `/panel` | GET | Panel municipal (tabla y dashboard) |
| `/panel/estado`, `/panel/categoria` | POST | Cambiar estado o categoría de un caso |

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

2. Definir las variables de entorno:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PIURA_DB_USER` | Sí | Usuario de PostgreSQL |
| `PIURA_DB_PASSWORD` | Sí | Contraseña de PostgreSQL |
| `PIURA_DB_URL` | No | Por defecto `jdbc:postgresql://localhost:5432/piuravigila` |
| `PIURA_ADMIN_PASSWORD` | Solo la primera vez | Contraseña inicial de `admin`; se guarda como hash BCrypt al arrancar |
| `PIURA_FOTOS_DIR` | No | Carpeta de fotos, por defecto `fotos` |

3. Ejecutar:

```powershell
$env:PIURA_DB_USER="postgres"
$env:PIURA_DB_PASSWORD="<contraseña>"
$env:PIURA_ADMIN_PASSWORD="<contraseña del admin>"
mvn spring-boot:run
```

4. Abrir http://localhost:8080/reportar. El panel está en `/login` (usuario `admin`).

Las contraseñas nunca se guardan en el código ni en el repositorio.

## Pruebas

```powershell
mvn test
```

Las pruebas unitarias cubren validación de reportes, control anti-spam, almacenamiento de fotos y los servicios (con DAOs simulados). Ver `evidencias/`.

## Funcionalidades

- Registro de incidencias con categoría, zona, descripción (máx. 300), foto (máx. 2 MB), ubicación GPS opcional y verificación aritmética.
- Código de seguimiento `PR-AAAA-####` y consulta pública de estado.
- Detección de posibles duplicados y vinculación a un caso original.
- Control anti-spam por dispositivo: 60 s entre reportes y máximo 3 por día.
- Panel municipal con login (BCrypt), cambio de estado con historial y recategorización.
- Dashboard: reportes por zona, tiempo de atención por categoría y resumen de gestión.
- Registro de eventos en `logs/piuravigila.log`.

## Limitaciones conocidas

- El "dispositivo" se identifica con una cookie anónima; borrarla reinicia los límites anti-spam. Es una medida simple, no de seguridad.
- El formulario público no incluye token CSRF (el panel sí). La verificación es una suma simple.
- El código `PR-AAAA-####` usa 4 dígitos; pasado el 9999 se truncaría.
- La foto se guarda en disco antes de confirmar la transacción; si falla la base de datos puede quedar un archivo huérfano.
- Mapa esquemático de zonas (RF-04) no implementado: solo la tabla R01.
- No hay usuario `operador` en el seed.
- Los datos son simulados; no registrar datos personales reales.

## Autores

Proyecto Integrador I · UTP Sede Piura.
