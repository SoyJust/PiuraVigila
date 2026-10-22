package pe.edu.utp.piuravigila.service;

import java.time.*;
import java.util.*;
import pe.edu.utp.piuravigila.dao.*;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;

/** DAO simulados en memoria: las pruebas de reglas no necesitan PostgreSQL ni Spring (DIP/LSP). */
final class Fakes {
    private Fakes() {}
    static final ZoneId LIMA = ZoneId.of("America/Lima");
    static Clock reloj(String iso) { return Clock.fixed(LocalDateTime.parse(iso).atZone(LIMA).toInstant(), LIMA); }

    static final class FakeReporteDAO implements ReporteDAO {
        final List<Reporte> filas = new ArrayList<>();
        int insertadosCaso = 0, insertadosVinculado = 0, siguiente = 1;
        Optional<LocalDateTime> ultimo = Optional.empty();
        int enElDia = 0;
        @Override public String insertarCaso(Reporte r) {
            String codigo = String.format("PR-%d-%04d", r.fechaRegistro().getYear(), siguiente);
            filas.add(new Reporte(siguiente++, codigo, null, r.categoriaId(), r.zonaId(), r.descripcion(), r.fotoUrl(), r.lat(), r.lng(), r.estado(), r.dispositivoId(), r.fechaRegistro()));
            insertadosCaso++; return codigo;
        }
        @Override public void insertarVinculado(Reporte r, int casoId) {
            filas.add(new Reporte(siguiente++, null, casoId, r.categoriaId(), r.zonaId(), r.descripcion(), r.fotoUrl(), r.lat(), r.lng(), r.estado(), r.dispositivoId(), r.fechaRegistro()));
            insertadosVinculado++;
        }
        @Override public Optional<Reporte> buscarPorCodigo(String c) { return filas.stream().filter(r -> c.equals(r.codigoSeguimiento())).findFirst(); }
        @Override public Optional<Reporte> buscarPorId(int id) { return filas.stream().filter(r -> r.reporteId() == id).findFirst(); }
        @Override public Optional<Reporte> buscarCasoAbierto(int cat, int zona) {
            return filas.stream().filter(r -> r.esCasoOriginal() && r.estaAbierto() && r.categoriaId() == cat && r.zonaId() == zona).findFirst();
        }
        @Override public Optional<LocalDateTime> ultimoEnvio(String d) { return ultimo; }
        @Override public int contarEnDia(String d, LocalDate dia) { return enElDia; }
        @Override public List<Reporte> listarCasos(Integer z, Integer c, EstadoReporte e, int p, int t) { return filas; }
        @Override public int contarCasos(Integer z, Integer c, EstadoReporte e) { return filas.size(); }
        @Override public int contarVinculados(int casoId) { return (int) filas.stream().filter(r -> Objects.equals(r.reporteOriginalId(), casoId)).count(); }
        @Override public void actualizarEstado(int id, EstadoReporte nuevo) {
            for (int i = 0; i < filas.size(); i++) { Reporte r = filas.get(i);
                if (r.reporteId() == id) filas.set(i, new Reporte(id, r.codigoSeguimiento(), r.reporteOriginalId(), r.categoriaId(), r.zonaId(), r.descripcion(), r.fotoUrl(), r.lat(), r.lng(), nuevo, r.dispositivoId(), r.fechaRegistro())); }
        }
        List<ConteoZona> zonas = List.of(); Map<EstadoReporte, Integer> porEstado = Map.of(); int vinculadosPeriodo = 0; double pctAct = 0;
        @Override public List<ConteoZona> reportesPorZona(LocalDateTime d, LocalDateTime h) { return zonas; }
        @Override public Map<EstadoReporte, Integer> casosPorEstado(LocalDateTime d, LocalDateTime h) { return porEstado; }
        @Override public int contarVinculadosPeriodo(LocalDateTime d, LocalDateTime h) { return vinculadosPeriodo; }
        @Override public double porcentajeActualizados(LocalDateTime d, LocalDateTime h) { return pctAct; }
        /** Atajo para preparar un caso existente. */
        Reporte caso(int cat, int zona, EstadoReporte estado) {
            insertarCaso(new Reporte(0, null, null, cat, zona, "caso previo", "fotos/x.jpg", null, null, EstadoReporte.RECIBIDO, "otro", LocalDateTime.of(2026, 10, 1, 8, 0)));
            if (estado != EstadoReporte.RECIBIDO) actualizarEstado(siguiente - 1, estado);
            return filas.get(filas.size() - 1).estado() == estado ? filas.get(filas.size() - 1) : null;
        }
    }

    static final class FakeHistorialDAO implements HistorialEstadoDAO {
        final List<HistorialEstado> filas = new ArrayList<>();
        @Override public void registrar(HistorialEstado h) { filas.add(h); }
        @Override public List<HistorialEstado> listarPorReporte(int id) { return filas.stream().filter(h -> h.reporteId() == id).toList(); }
        @Override public Optional<LocalDateTime> ultimaActualizacion(int id) { return listarPorReporte(id).stream().map(HistorialEstado::fechaCambio).max(Comparator.naturalOrder()); }
        @Override public List<TiempoCategoria> tiempoPromedio(LocalDateTime d, LocalDateTime h) { return List.of(); }
    }

    static final class FakeCategoriaDAO implements CategoriaDAO {
        final List<Categoria> filas = new ArrayList<>(List.of(new Categoria(1, "Bache en la vía", true), new Categoria(2, "Luminaria dañada", true), new Categoria(3, "Otra", true)));
        double pctOtra = 0;
        @Override public List<Categoria> listarActivas() { return filas.stream().filter(Categoria::activo).toList(); }
        @Override public Optional<Categoria> buscarPorId(int id) { return filas.stream().filter(c -> c.categoriaId() == id).findFirst(); }
        @Override public void insertar(Categoria c) { filas.add(new Categoria(filas.size() + 1, c.nombre(), c.activo())); }
        @Override public boolean existeNombre(String n) { return filas.stream().anyMatch(c -> c.nombre().equalsIgnoreCase(n)); }
        @Override public double porcentajeOtraMes() { return pctOtra; }
    }

    static final class FakeZonaDAO implements ZonaDAO {
        final List<Zona> filas = List.of(new Zona(1, "Centro", null), new Zona(2, "Castilla", null));
        @Override public List<Zona> listar() { return filas; }
        @Override public Optional<Zona> buscarPorId(int id) { return filas.stream().filter(z -> z.zonaId() == id).findFirst(); }
    }

    static final class FakeUsuarioDAO implements UsuarioMunicipalDAO {
        final Map<String, UsuarioMunicipal> m = new HashMap<>();
        @Override public Optional<UsuarioMunicipal> buscarPorUsuario(String u) { return Optional.ofNullable(m.get(u)); }
        @Override public void actualizarHash(String u, String h) { }
    }

    static final class FakeFotos implements FotoStorage {
        int guardadas = 0;
        @Override public String guardar(String nombre, byte[] bytes) { guardadas++; return "fotos/fake-" + guardadas + ".jpg"; }
    }
}
