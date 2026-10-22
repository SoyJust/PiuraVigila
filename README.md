# PiuraVigila · Proyecto Integrador (APF3 · Semana 10)

Portal web para reportar incidencias urbanas (baches, luminarias, basura) y que el personal municipal las atienda.
Patrón: View (Thymeleaf) → Controller → Service → DAO (JdbcTemplate) → PostgreSQL. Spring Boot, Java 21.

## Estado real (léelo antes de entregar)
| Parte | Estado |
|---|---|
| Model, DTO, interfaces DAO, Service, FotoStorage | Compilan en JDK 21 (con stubs de Spring/JUnit) |
| 35 pruebas (`src/test`) | **35 de 35 pasan** con arnés mínimo en JDK 21; ver `evidencias/ejecucion_pruebas_arnes.txt`. **Falta repetir con `mvn test` (JUnit real).** |
| Controladores, DAO JDBC, plantillas, `application.properties` | Escritos y revisados a mano; **sin compilar ni ejecutar** (aquí no hay Maven ni PostgreSQL) |
| Persistencia real, log, capturas | **Pendiente**: ver hoja Evidencias de la matriz |

## Preparación
1. JDK 21, Maven, PostgreSQL 15+.
2. Crear la base `piuravigila` y ejecutar `database/01_schema.sql` y luego `database/02_seed.sql`.
3. Variables de entorno (no pongas contraseñas en el código ni en Git):
   `PIURA_DB_USER`, `PIURA_DB_PASSWORD`, opcional `PIURA_DB_URL`, `PIURA_ADMIN_PASSWORD` (contraseña inicial del usuario `admin`; se guarda como hash BCrypt al arrancar).
4. `mvn test` y luego `mvn spring-boot:run` → http://localhost:8080
   - Registrar: `/reportar` · Consultar: `/consultar` · Panel: `/login` (usuario `admin`).
5. Comprobar en BD con `database/03_consultas_dashboard.sql` y revisar `logs/piuravigila.log`.

## Paquetes (`pe.edu.utp.piuravigila`)
`model` · `dto` · `dao` (contratos) · `dao.jdbc` (@Repository) · `service` (@Service, @Transactional) · `controller` (@Controller) · `config` · `app`.

## Límites conocidos
- Spring Boot 3.5.15 en el `pom.xml`: confirmar en start.spring.io (no pude resolver Maven aquí); la rama 3.5 ya terminó su soporte abierto.
- «Dispositivo» = cookie anónima: borrarla reinicia los límites de 60 s y 3 por día (RF-06 es anti-spam simple, no seguridad).
- El formulario público no lleva token CSRF (sí el panel). La verificación es una suma simple.
- El código `PR-AAAA-####` usa `LPAD` a 4 dígitos: pasado el 9999 se truncaría (suficiente para la demo).
- La foto se guarda en disco antes de confirmar la transacción; si falla la BD puede quedar un archivo huérfano.
- Mapa esquemático P04 (RF-04) **no implementado**; solo la tabla R01. No hay usuario `operador` en el seed.
- Datos simulados; no registrar datos personales reales.
