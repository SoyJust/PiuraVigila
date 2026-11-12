package pe.edu.utp.piuravigila.dao.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Objects;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import pe.edu.utp.piuravigila.model.RolUsuario;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;

/**
 * v1.0 · integración del DAO de usuarios y de la migración 05 contra PostgreSQL real (misma base {@code *_test} y
 * mismas variables de entorno que {@link ReporteDAOJdbcIT}; solo con {@code RUN_DB_TESTS=true}).
 */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class UsuarioMunicipalDAOJdbcIT {
    private static JdbcTemplate jdbc;
    private UsuarioMunicipalDAOJdbc dao;

    @BeforeAll
    static void conectar() {
        String url = System.getenv().getOrDefault("PIURA_TEST_DB_URL", "jdbc:postgresql://localhost:5432/piuravigila_test");
        assertTrue(url.endsWith("_test"), "La base de prueba debe terminar en _test: las pruebas recrean su esquema");
        DriverManagerDataSource ds = new DriverManagerDataSource(url,
                System.getenv().getOrDefault("PIURA_TEST_DB_USER", "postgres"), System.getenv().getOrDefault("PIURA_TEST_DB_PASSWORD", ""));
        ds.setDriverClassName("org.postgresql.Driver");
        jdbc = new JdbcTemplate(ds);
    }

    private static void ejecutar(String... archivos) {
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator();
        for (String a : archivos) scripts.addScript(new FileSystemResource(a));
        scripts.setSqlScriptEncoding("UTF-8");
        scripts.execute(Objects.requireNonNull(jdbc.getDataSource()));
    }

    @BeforeEach
    void esquemaLimpio() {
        jdbc.execute("DROP SCHEMA public CASCADE");
        jdbc.execute("CREATE SCHEMA public");
        ejecutar("database/01_schema.sql", "database/02_seed.sql");
        dao = new UsuarioMunicipalDAOJdbc(jdbc);
    }

    @Test @DisplayName("TC-U16 · crear, buscar por id y listar: la cuenta nueva nace activa")
    void crear_y_buscar() {
        int id = dao.crear("Rosa Chávez", "rchavez", "$2a$10$hashdeprueba", RolUsuario.OPERADOR);
        UsuarioMunicipal u = dao.buscarPorId(id).orElseThrow();
        assertEquals("rchavez", u.usuario());
        assertEquals(RolUsuario.OPERADOR, u.rol());
        assertTrue(u.activo());
        assertEquals(3, dao.listar().size()); // admin y operador del seed + la nueva
    }

    @Test @DisplayName("TC-U17 · desactivar y volver a activar se guarda; listar pone las activas primero")
    void cambiar_activo() {
        int op = dao.buscarPorUsuario("operador").orElseThrow().usuarioId();
        dao.cambiarActivo(op, false);
        assertFalse(dao.buscarPorId(op).orElseThrow().activo());
        assertFalse(dao.listar().get(dao.listar().size() - 1).activo(), "las desactivadas van al final");
        dao.cambiarActivo(op, true);
        assertTrue(dao.buscarPorUsuario("operador").orElseThrow().activo());
    }

    @Test @DisplayName("TC-U18 · la base rechaza un usuario repetido y un rol fuera del CHECK")
    void restricciones_de_la_base() {
        assertThrows(DataIntegrityViolationException.class, () -> dao.crear("Otro", "admin", "h", RolUsuario.OPERADOR));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO usuario_municipal (nombre, usuario, contrasena_hash, rol) VALUES ('X', 'x1', 'h', 'jefe')"));
    }

    @Test @DisplayName("TC-U19 · la migración 05 se puede ejecutar dos veces sobre una base v0.9 y deja 10 distritos")
    void migracion_idempotente_desde_v09() {
        // Simula una base anterior: sin columna activo y solo con los 6 distritos originales.
        jdbc.execute("ALTER TABLE usuario_municipal DROP COLUMN activo");
        jdbc.update("DELETE FROM zona WHERE nombre IN ('Cura Mori', 'El Tallán', 'Las Lomas', 'Tambo Grande')");
        ejecutar("database/05_migracion_v1_0.sql", "database/05_migracion_v1_0.sql");
        assertEquals(10, jdbc.queryForObject("SELECT COUNT(*) FROM zona", Integer.class));
        assertTrue(dao.buscarPorUsuario("admin").orElseThrow().activo(), "las cuentas existentes quedan activas");
    }
}
