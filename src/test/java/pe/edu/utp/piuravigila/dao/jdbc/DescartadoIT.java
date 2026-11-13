package pe.edu.utp.piuravigila.dao.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Objects;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import pe.edu.utp.piuravigila.model.*;

/** v1.2 · estado «Descartado» contra PostgreSQL real (TC-D08 a TC-D11). Solo con RUN_DB_TESTS=true. */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class DescartadoIT {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 2, 9, 0);
    private static JdbcTemplate jdbc;
    private ReporteDAOJdbc reportes;
    private HistorialEstadoDAOJdbc historial;
    private int admin;

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
        ResourceDatabasePopulator s = new ResourceDatabasePopulator();
        for (String a : archivos) s.addScript(new FileSystemResource(a));
        s.setSqlScriptEncoding("UTF-8");
        s.execute(Objects.requireNonNull(jdbc.getDataSource()));
    }

    @BeforeEach
    void esquemaLimpio() {
        jdbc.execute("DROP SCHEMA public CASCADE");
        jdbc.execute("CREATE SCHEMA public");
        ejecutar("database/01_schema.sql", "database/02_seed.sql");
        reportes = new ReporteDAOJdbc(jdbc);
        historial = new HistorialEstadoDAOJdbc(jdbc);
        admin = jdbc.queryForObject("SELECT usuario_id FROM usuario_municipal WHERE usuario = 'admin'", Integer.class);
    }

    private int caso(int cat, int zona, LocalDateTime f) {
        String cod = reportes.insertarCaso(new Reporte(0, null, null, cat, zona, "Caso", "fotos/c" + cat + zona + ".jpg", -5.19, -80.63,
                EstadoReporte.RECIBIDO, "it", f));
        return reportes.buscarPorCodigo(cod).orElseThrow().reporteId();
    }

    @Test @DisplayName("TC-D08 · la base guarda «Descartado» con motivo y rechaza descartar sin motivo (ck_historial_motivo)")
    void la_base_exige_motivo() {
        int id = caso(1, 1, T0);
        reportes.actualizarEstado(id, EstadoReporte.DESCARTADO);
        assertThrows(DataIntegrityViolationException.class,
                () -> historial.registrar(new HistorialEstado(0, id, admin, EstadoReporte.DESCARTADO, T0.plusDays(1), "corto")));
        historial.registrar(new HistorialEstado(0, id, admin, EstadoReporte.DESCARTADO, T0.plusDays(1), "Reporte de prueba sin problema real"));
        assertEquals(EstadoReporte.DESCARTADO, reportes.buscarPorId(id).orElseThrow().estado());
        assertTrue(reportes.buscarCasoAbierto(1, 1).isEmpty(), "un caso descartado no está abierto");
    }

    @Test @DisplayName("TC-D09 · R01 (zonas críticas) no cuenta los descartados ni sus vinculados; R03 sí los cuenta como estado")
    void r01_sin_descartados() {
        int bueno = caso(1, 1, T0), malo = caso(4, 1, T0.plusHours(1));
        reportes.insertarVinculado(new Reporte(0, null, malo, 4, 1, "x", "fotos/v.jpg", null, null, EstadoReporte.RECIBIDO, "it-v", T0.plusHours(2)), malo);
        reportes.actualizarEstado(malo, EstadoReporte.DESCARTADO);
        historial.registrar(new HistorialEstado(0, malo, admin, EstadoReporte.DESCARTADO, T0.plusDays(1), "No corresponde a la vía pública"));
        var r01 = reportes.reportesPorZona(T0.minusDays(1), T0.plusDays(2));
        assertEquals(1, r01.size());
        assertEquals(1, r01.get(0).reportes(), "solo cuenta el caso no descartado");
        assertEquals(1, reportes.casosPorEstado(T0.minusDays(1), T0.plusDays(2)).get(EstadoReporte.DESCARTADO));
        assertTrue(bueno > 0);
    }

    @Test @DisplayName("TC-D10 · la retención también borra las fotos de casos descartados hace más de N días")
    void retencion_incluye_descartados() {
        int id = caso(1, 1, T0.minusDays(400));
        reportes.actualizarEstado(id, EstadoReporte.DESCARTADO);
        historial.registrar(new HistorialEstado(0, id, admin, EstadoReporte.DESCARTADO, T0.minusDays(399), "Reporte de prueba sin problema real"));
        assertEquals(java.util.List.of("fotos/c11.jpg"), reportes.fotosDeCasosAtendidosAntesDe(T0.minusDays(365)));
    }

    @Test @DisplayName("TC-D11 · la migración 05 sobre una base v1.1 (sin «Descartado») habilita el estado y se puede repetir")
    void migracion_habilita_descartado() {
        jdbc.execute("ALTER TABLE reporte DROP CONSTRAINT ck_reporte_estado");
        jdbc.execute("ALTER TABLE reporte ADD CONSTRAINT ck_reporte_estado CHECK (estado IN ('Recibido','En proceso','Atendido'))");
        jdbc.execute("ALTER TABLE historial_estado DROP CONSTRAINT ck_historial_estado");
        jdbc.execute("ALTER TABLE historial_estado ADD CONSTRAINT ck_historial_estado CHECK (estado_nuevo IN ('Recibido','En proceso','Atendido'))");
        jdbc.execute("ALTER TABLE historial_estado DROP CONSTRAINT ck_historial_motivo");
        int id = caso(1, 1, T0);
        assertThrows(DataIntegrityViolationException.class, () -> reportes.actualizarEstado(id, EstadoReporte.DESCARTADO));
        ejecutar("database/05_migracion_v1_0.sql", "database/05_migracion_v1_0.sql");
        reportes.actualizarEstado(id, EstadoReporte.DESCARTADO);
        assertThrows(DataIntegrityViolationException.class,
                () -> historial.registrar(new HistorialEstado(0, id, admin, EstadoReporte.DESCARTADO, T0, null)));
    }
}
