package pe.edu.utp.piuravigila.dao.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import pe.edu.utp.piuravigila.dto.ConteoZona;
import pe.edu.utp.piuravigila.dto.TiempoCategoria;
import pe.edu.utp.piuravigila.model.*;

/**
 * Pruebas de integración de los DAO JDBC contra PostgreSQL real (Semana 13, ruta A · DAO/JDBC).
 *
 * <p>Se ejecutan solo con {@code RUN_DB_TESTS=true} (mvn verify). Usan una base de prueba propia
 * ({@code PIURA_TEST_DB_URL}, por defecto {@code jdbc:postgresql://localhost:5432/piuravigila_test}); el nombre
 * debe terminar en {@code _test} porque cada prueba borra el esquema y lo vuelve a crear con
 * {@code database/01_schema.sql} y {@code 02_seed.sql}. Las credenciales salen de variables de entorno.</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class ReporteDAOJdbcIT {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 2, 9, 0);
    // IDs del seed: zonas 1 Piura, 2 Castilla, 3 Veintiséis de Octubre... · categorías 1 Bache, 4 Luminaria...
    private static final int PIURA = 1, CASTILLA = 2, VEINTISEIS = 3, BACHE = 1, LUMINARIA = 4, BASURA = 7;

    private static JdbcTemplate jdbc;
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

    private static Reporte nuevo(int categoria, int zona, EstadoReporte estado, LocalDateTime fecha, String dispositivo) {
        return new Reporte(0, null, null, categoria, zona, "Reporte de prueba de integración", "fotos/prueba.jpg",
                -5.19, -80.63, estado, dispositivo, fecha);
    }

    private int idDe(String codigo) {
        return reportes.buscarPorCodigo(codigo).orElseThrow().reporteId();
    }

    @Test @DisplayName("PV-T36 · insertarCaso genera PR-AAAA-#### con la secuencia y guarda la fila")
    void insertar_caso_genera_codigo_con_la_secuencia() {
        String c1 = reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0, "it-dev-1"));
        String c2 = reportes.insertarCaso(nuevo(BACHE, CASTILLA, EstadoReporte.RECIBIDO, T0.plusMinutes(1), "it-dev-2"));

        assertEquals("PR-2026-0001", c1);
        assertEquals("PR-2026-0002", c2);
        Reporte guardado = reportes.buscarPorCodigo(c1).orElseThrow();
        assertTrue(guardado.esCasoOriginal());
        assertEquals(EstadoReporte.RECIBIDO, guardado.estado());
        assertEquals(PIURA, guardado.zonaId());
        assertEquals(-5.19, guardado.lat(), 1e-6);
    }

    @Test @DisplayName("PV-T37 · 100 envíos simultáneos reciben 100 códigos distintos (RNF-03)")
    void envios_simultaneos_no_repiten_codigo() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int n = i;
            futuros.add(hilos.submit(() -> {
                salida.await();
                return reportes.insertarCaso(nuevo(BACHE, 1 + n % 6, EstadoReporte.RECIBIDO, T0.plusSeconds(n), "it-con-" + n));
            }));
        }
        salida.countDown();
        Set<String> codigos = new HashSet<>();
        for (Future<String> f : futuros) codigos.add(f.get(60, TimeUnit.SECONDS));
        hilos.shutdown();

        assertEquals(100, codigos.size(), "ningún código repetido");
        assertTrue(codigos.stream().allMatch(c -> c.matches("^PR-2026-\\d{4}$")));
        assertEquals(100, jdbc.queryForObject("SELECT COUNT(*) FROM reporte", Integer.class), "no se perdió ningún envío");
    }

    @Test @DisplayName("PV-T38 · buscarCasoAbierto solo devuelve casos originales Recibido o En proceso")
    void buscar_caso_abierto_respeta_estado_y_original() {
        int atendido = idDe(reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0, "it-a")));
        reportes.actualizarEstado(atendido, EstadoReporte.ATENDIDO);
        String enProceso = reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.EN_PROCESO, T0.plusHours(1), "it-b"));
        reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0.plusHours(2), "it-c"));
        reportes.insertarVinculado(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0.plusHours(3), "it-d"), idDe(enProceso));
        int soloAtendido = idDe(reportes.insertarCaso(nuevo(LUMINARIA, CASTILLA, EstadoReporte.RECIBIDO, T0, "it-e")));
        reportes.actualizarEstado(soloAtendido, EstadoReporte.ATENDIDO);

        Reporte abierto = reportes.buscarCasoAbierto(BACHE, PIURA).orElseThrow();
        assertEquals(enProceso, abierto.codigoSeguimiento(), "el caso abierto más antiguo, nunca el Atendido");
        assertTrue(abierto.esCasoOriginal());
        assertTrue(reportes.buscarCasoAbierto(LUMINARIA, CASTILLA).isEmpty(), "un caso Atendido no se ofrece como duplicado");
        assertTrue(reportes.buscarCasoAbierto(BASURA, PIURA).isEmpty());
    }

    @Test @DisplayName("PV-T56 · listarCasos devuelve 20 casos por página y ningún vinculado (T-19)")
    void listar_casos_pagina_de_20_sin_vinculados() {
        String primero = null;
        for (int i = 0; i < 45; i++) {
            String c = reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0.plusMinutes(i), "it-p" + i));
            if (primero == null) primero = c;
        }
        for (int i = 0; i < 5; i++) {
            reportes.insertarVinculado(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0.plusHours(5).plusMinutes(i), "it-v" + i), idDe(primero));
        }

        List<Reporte> pagina1 = reportes.listarCasos(null, null, null, 0, 20);
        List<Reporte> pagina3 = reportes.listarCasos(null, null, null, 2, 20);
        assertEquals(20, pagina1.size());
        assertEquals(5, pagina3.size());
        assertTrue(pagina1.stream().allMatch(Reporte::esCasoOriginal));
        assertEquals(45, reportes.contarCasos(null, null, null));
        assertEquals(5, reportes.contarVinculados(idDe(primero)));
        assertEquals(45, reportes.contarCasos(PIURA, BACHE, EstadoReporte.RECIBIDO), "los filtros se aplican como parámetros");
    }

    @Test @DisplayName("PV-T40 · R01 cuenta vinculados, R02 promedia solo Atendidos y R03 cuenta por estado")
    void consultas_r01_r02_r03_con_datos_conocidos() {
        // Piura: 2 casos + 3 vinculados = 5 · Castilla: 1 caso
        int c1 = idDe(reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0, "it-r1")));
        int c2 = idDe(reportes.insertarCaso(nuevo(LUMINARIA, PIURA, EstadoReporte.RECIBIDO, T0, "it-r2")));
        int c3 = idDe(reportes.insertarCaso(nuevo(BACHE, CASTILLA, EstadoReporte.RECIBIDO, T0, "it-r3")));
        for (int i = 0; i < 3; i++) reportes.insertarVinculado(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0.plusHours(i + 1), "it-rv" + i), c1);
        // c1 se atiende en 2 días y c3 en 4 días (Bache: 2 casos, 3,0 días); c2 queda En proceso y no cuenta en R02
        cambiar(c1, EstadoReporte.EN_PROCESO, T0.plusDays(1)); cambiar(c1, EstadoReporte.ATENDIDO, T0.plusDays(2));
        cambiar(c3, EstadoReporte.EN_PROCESO, T0.plusDays(1)); cambiar(c3, EstadoReporte.ATENDIDO, T0.plusDays(4));
        cambiar(c2, EstadoReporte.EN_PROCESO, T0.plusDays(1));
        LocalDateTime desde = T0.minusDays(1), hasta = T0.plusDays(10);

        List<ConteoZona> r01 = reportes.reportesPorZona(desde, hasta);
        assertEquals(new ConteoZona("Piura", 5), r01.get(0));
        assertEquals(new ConteoZona("Castilla", 1), r01.get(1));

        List<TiempoCategoria> r02 = historial.tiempoPromedio(desde, hasta);
        assertEquals(1, r02.size(), "solo la categoría con casos Atendidos");
        assertEquals("Bache en la vía", r02.get(0).categoria());
        assertEquals(2, r02.get(0).casosAtendidos());
        assertEquals(3.0, r02.get(0).diasPromedio(), 1e-9);

        Map<EstadoReporte, Integer> r03 = reportes.casosPorEstado(desde, hasta);
        assertEquals(2, r03.get(EstadoReporte.ATENDIDO));
        assertEquals(1, r03.get(EstadoReporte.EN_PROCESO));
        assertNull(r03.get(EstadoReporte.RECIBIDO));
        assertEquals(3, reportes.contarVinculadosPeriodo(desde, hasta), "duplicados evitados");
        assertEquals(100.0, reportes.porcentajeActualizados(desde, hasta), 1e-9);
        assertTrue(reportes.reportesPorZona(T0.plusDays(20), T0.plusDays(30)).isEmpty(), "fuera del periodo no cuenta");
    }

    @Test @DisplayName("PV-T57 · la BD rechaza un vinculado con código propio y un historial sin responsable (RN-01, S03)")
    void restricciones_de_integridad_de_la_bd() {
        int caso = idDe(reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0, "it-i1")));
        reportes.insertarVinculado(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0.plusMinutes(5), "it-i2"), caso);
        assertNull(jdbc.queryForObject("SELECT codigo_seguimiento FROM reporte WHERE reporte_original_id = ?", String.class, caso),
                "el vinculado no tiene código propio");

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO reporte(codigo_seguimiento, reporte_original_id, categoria_id, zona_id, descripcion, foto_url, estado, dispositivo_id) "
                        + "VALUES ('PR-2026-9999', ?, 1, 1, 'x', 'f', 'Recibido', 'it-i3')", caso));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO reporte(codigo_seguimiento, categoria_id, zona_id, descripcion, foto_url, estado, dispositivo_id) "
                        + "VALUES ('CODIGO-MALO', 1, 1, 'x', 'f', 'Recibido', 'it-i4')"));
        assertThrows(DataIntegrityViolationException.class, () -> historial.registrar(
                new HistorialEstado(0, caso, null, EstadoReporte.ATENDIDO, T0.plusDays(1))),
                "solo el «Recibido» automático puede no tener responsable");
    }

    @Test @DisplayName("TC-SEC-07 · entradas con SQL malicioso se tratan como datos (consultas parametrizadas)")
    void inyeccion_sql_se_trata_como_dato() {
        reportes.insertarCaso(nuevo(BACHE, PIURA, EstadoReporte.RECIBIDO, T0, "it-s1"));

        assertTrue(reportes.buscarPorCodigo("' OR '1'='1").isEmpty(), "la condición inyectada no devuelve filas");
        assertTrue(reportes.buscarPorCodigo("PR-2026-0001'; DROP TABLE reporte; --").isEmpty());
        assertEquals(0, reportes.contarEnDia("x' OR '1'='1", T0.toLocalDate()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM reporte", Integer.class), "la tabla sigue intacta");
        assertTrue(reportes.buscarPorCodigo("PR-2026-0001").isPresent(), "la búsqueda legítima sigue funcionando");
    }

    private void cambiar(int reporteId, EstadoReporte estado, LocalDateTime fecha) {
        reportes.actualizarEstado(reporteId, estado);
        historial.registrar(new HistorialEstado(0, reporteId, 1, estado, fecha));
    }
}
