package pe.edu.utp.piuravigila.dao.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.utp.piuravigila.dto.FilaExportacion;
import pe.edu.utp.piuravigila.dto.MovimientoCaso;
import pe.edu.utp.piuravigila.model.*;

/** v1.1 · bloqueo por clave en PostgreSQL y consultas nuevas (TC-I01 a TC-I05). Solo con RUN_DB_TESTS=true. */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class ConcurrenciaYConsultasV11IT {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 2, 9, 0);
    private static JdbcTemplate jdbc;
    private static TransactionTemplate tx;
    private ReporteDAOJdbc reportes;
    private HistorialEstadoDAOJdbc historial;

    @BeforeAll
    static void conectar() {
        String url = System.getenv().getOrDefault("PIURA_TEST_DB_URL", "jdbc:postgresql://localhost:5432/piuravigila_test");
        assertTrue(url.endsWith("_test"), "La base de prueba debe terminar en _test: las pruebas recrean su esquema");
        DriverManagerDataSource ds = new DriverManagerDataSource(url,
                System.getenv().getOrDefault("PIURA_TEST_DB_USER", "postgres"), System.getenv().getOrDefault("PIURA_TEST_DB_PASSWORD", ""));
        ds.setDriverClassName("org.postgresql.Driver");
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    @BeforeEach
    void esquemaLimpio() {
        jdbc.execute("DROP SCHEMA public CASCADE");
        jdbc.execute("CREATE SCHEMA public");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new FileSystemResource("database/01_schema.sql"), new FileSystemResource("database/02_seed.sql"));
        scripts.setSqlScriptEncoding("UTF-8");
        scripts.execute(Objects.requireNonNull(jdbc.getDataSource()));
        reportes = new ReporteDAOJdbc(jdbc);
        historial = new HistorialEstadoDAOJdbc(jdbc);
    }

    private String caso(int cat, int zona, LocalDateTime f) {
        return reportes.insertarCaso(new Reporte(0, null, null, cat, zona, "Caso de prueba", "fotos/c.jpg", -5.19, -80.63,
                EstadoReporte.RECIBIDO, "it-dev", f));
    }

    @Test @DisplayName("TC-I01 · dos transacciones con la misma clave se atienden en fila: la segunda espera a que la primera termine")
    void bloqueo_serializa_la_misma_clave() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CountDownLatch primeraTieneElBloqueo = new CountDownLatch(1);
        try {
            Future<?> a = hilos.submit(() -> tx.executeWithoutResult(s -> {
                reportes.bloquear("dispositivo:it-1");
                primeraTieneElBloqueo.countDown();
                try { Thread.sleep(700); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
            assertTrue(primeraTieneElBloqueo.await(5, TimeUnit.SECONDS));
            long inicio = System.nanoTime();
            Future<Long> b = hilos.submit(() -> tx.execute(s -> { reportes.bloquear("dispositivo:it-1"); return System.nanoTime(); }));
            long esperaMs = (b.get(10, TimeUnit.SECONDS) - inicio) / 1_000_000;
            a.get(10, TimeUnit.SECONDS);
            assertTrue(esperaMs >= 400, "la segunda transacción debió esperar (esperó " + esperaMs + " ms)");
        } finally { hilos.shutdownNow(); }
    }

    @Test @DisplayName("TC-I02 · claves distintas no se bloquean entre sí")
    void claves_distintas_no_esperan() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CountDownLatch listo = new CountDownLatch(1);
        try {
            Future<?> a = hilos.submit(() -> tx.executeWithoutResult(s -> {
                reportes.bloquear("dispositivo:it-A"); listo.countDown();
                try { Thread.sleep(700); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            long inicio = System.nanoTime();
            hilos.submit(() -> tx.executeWithoutResult(s -> reportes.bloquear("dispositivo:it-B"))).get(10, TimeUnit.SECONDS);
            assertTrue((System.nanoTime() - inicio) / 1_000_000 < 400);
            a.get(10, TimeUnit.SECONDS);
        } finally { hilos.shutdownNow(); }
    }

    @Test @DisplayName("TC-I03 · el comentario del cambio de estado se guarda y el detalle trae el nombre del responsable")
    void comentario_y_responsable() {
        String cod = caso(1, 1, T0);
        int id = reportes.buscarPorCodigo(cod).orElseThrow().reporteId();
        historial.registrar(new HistorialEstado(0, id, null, EstadoReporte.RECIBIDO, T0));
        int op = jdbc.queryForObject("SELECT usuario_id FROM usuario_municipal WHERE usuario = 'operador'", Integer.class);
        historial.registrar(new HistorialEstado(0, id, op, EstadoReporte.EN_PROCESO, T0.plusDays(1), "Cuadrilla programada"));
        assertEquals("Cuadrilla programada", historial.listarPorReporte(id).get(1).comentario());
        List<MovimientoCaso> d = historial.listarDetalle(id);
        assertNull(d.get(0).responsable());
        assertEquals("Operador PiuraVigila", d.get(1).responsable());
    }

    @Test @DisplayName("TC-I04 · vinculados del caso y filas del Excel con nombres, vinculados y fecha de atención")
    void vinculados_y_exportacion() {
        String cod = caso(1, 2, T0);
        int id = reportes.buscarPorCodigo(cod).orElseThrow().reporteId();
        reportes.insertarVinculado(new Reporte(0, null, id, 1, 2, "Mismo bache", "fotos/v.jpg", null, null, EstadoReporte.RECIBIDO, "it-v", T0.plusHours(3)), id);
        int op = jdbc.queryForObject("SELECT usuario_id FROM usuario_municipal WHERE usuario = 'operador'", Integer.class);
        reportes.actualizarEstado(id, EstadoReporte.ATENDIDO);
        historial.registrar(new HistorialEstado(0, id, op, EstadoReporte.ATENDIDO, T0.plusDays(4)));
        assertEquals(1, reportes.listarVinculados(id).size());
        List<FilaExportacion> filas = reportes.casosParaExportar(T0.minusDays(1), T0.plusDays(1));
        assertEquals(1, filas.size()); // el vinculado no es una fila aparte
        FilaExportacion f = filas.get(0);
        assertEquals("Bache en la vía", f.categoria());
        assertEquals(1, f.vinculados());
        assertEquals(T0.plusDays(4), f.fechaAtendido());
    }

    @Test @DisplayName("TC-I05 · retención: solo fotos (caso y vinculados) de casos atendidos antes del límite")
    void fotos_para_retencion() {
        String viejo = caso(1, 1, T0.minusDays(400)), abierto = caso(4, 1, T0.minusDays(400));
        int idViejo = reportes.buscarPorCodigo(viejo).orElseThrow().reporteId();
        reportes.insertarVinculado(new Reporte(0, null, idViejo, 1, 1, "x", "fotos/vv.jpg", null, null, EstadoReporte.RECIBIDO, "it-v", T0.minusDays(399)), idViejo);
        int op = jdbc.queryForObject("SELECT usuario_id FROM usuario_municipal WHERE usuario = 'operador'", Integer.class);
        reportes.actualizarEstado(idViejo, EstadoReporte.ATENDIDO);
        historial.registrar(new HistorialEstado(0, idViejo, op, EstadoReporte.ATENDIDO, T0.minusDays(380)));
        List<String> fotos = reportes.fotosDeCasosAtendidosAntesDe(T0.minusDays(365));
        assertEquals(2, fotos.size());
        assertTrue(fotos.containsAll(List.of("fotos/c.jpg", "fotos/vv.jpg")));
        assertTrue(reportes.fotosDeCasosAtendidosAntesDe(T0.minusDays(390)).isEmpty(), "se atendió hace 380 días, no 390");
        assertNotNull(abierto);
    }
}
