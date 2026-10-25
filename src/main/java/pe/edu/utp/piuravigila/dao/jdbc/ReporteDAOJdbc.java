package pe.edu.utp.piuravigila.dao.jdbc;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.edu.utp.piuravigila.dao.ReporteDAO;
import pe.edu.utp.piuravigila.dto.ConteoZona;
import pe.edu.utp.piuravigila.dto.PuntoReporte;
import pe.edu.utp.piuravigila.model.*;

/** SQL parametrizado (sin concatenar entradas del usuario). Las consultas del dashboard vienen del SQL físico de APF2. */
@Repository
public class ReporteDAOJdbc implements ReporteDAO {
    private static final String COLUMNAS = "reporte_id, codigo_seguimiento, reporte_original_id, categoria_id, zona_id, "
            + "descripcion, foto_url, lat, lng, estado, dispositivo_id, fecha_registro";
    private final JdbcTemplate jdbc;

    public ReporteDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Reporte> MAPEO = (rs, n) -> new Reporte(
            rs.getInt("reporte_id"), rs.getString("codigo_seguimiento"),
            (Integer) rs.getObject("reporte_original_id"), rs.getInt("categoria_id"), rs.getInt("zona_id"),
            rs.getString("descripcion"), rs.getString("foto_url"),
            rs.getObject("lat") == null ? null : rs.getDouble("lat"),
            rs.getObject("lng") == null ? null : rs.getDouble("lng"),
            EstadoReporte.desdeEtiqueta(rs.getString("estado")), rs.getString("dispositivo_id"),
            rs.getTimestamp("fecha_registro").toLocalDateTime());

    @Override
    public String insertarCaso(Reporte r) {
        // El número sale de la secuencia de la BD: dos envíos simultáneos nunca reciben el mismo código (RNF-03).
        String sql = "INSERT INTO reporte(codigo_seguimiento, categoria_id, zona_id, descripcion, foto_url, lat, lng, estado, dispositivo_id, fecha_registro) "
                + "VALUES ('PR-' || EXTRACT(YEAR FROM CAST(? AS TIMESTAMP))::int || '-' || LPAD(nextval('seq_codigo_reporte')::text, 4, '0'), "
                + "?, ?, ?, ?, CAST(? AS DECIMAL(9,6)), CAST(? AS DECIMAL(9,6)), ?, ?, ?) RETURNING codigo_seguimiento";
        Timestamp t = Timestamp.valueOf(r.fechaRegistro());
        return jdbc.queryForObject(sql, String.class, t, r.categoriaId(), r.zonaId(), r.descripcion(), r.fotoUrl(),
                r.lat(), r.lng(), r.estado().etiqueta(), r.dispositivoId(), t);
    }

    @Override
    public void insertarVinculado(Reporte r, int casoId) {
        jdbc.update("INSERT INTO reporte(reporte_original_id, categoria_id, zona_id, descripcion, foto_url, lat, lng, estado, dispositivo_id, fecha_registro) "
                + "VALUES (?, ?, ?, ?, ?, CAST(? AS DECIMAL(9,6)), CAST(? AS DECIMAL(9,6)), ?, ?, ?)",
                casoId, r.categoriaId(), r.zonaId(), r.descripcion(), r.fotoUrl(), r.lat(), r.lng(),
                r.estado().etiqueta(), r.dispositivoId(), Timestamp.valueOf(r.fechaRegistro()));
    }

    @Override public Optional<Reporte> buscarPorCodigo(String codigo) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM reporte WHERE codigo_seguimiento = ?", MAPEO, codigo).stream().findFirst();
    }

    @Override public Optional<Reporte> buscarPorId(int id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM reporte WHERE reporte_id = ?", MAPEO, id).stream().findFirst();
    }

    @Override public Optional<Reporte> buscarCasoAbierto(int categoriaId, int zonaId) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM reporte WHERE categoria_id = ? AND zona_id = ? "
                + "AND reporte_original_id IS NULL AND estado IN ('Recibido','En proceso') ORDER BY fecha_registro LIMIT 1",
                MAPEO, categoriaId, zonaId).stream().findFirst();
    }

    @Override public Optional<LocalDateTime> ultimoEnvio(String dispositivoId) {
        Timestamp t = jdbc.queryForObject("SELECT MAX(fecha_registro) FROM reporte WHERE dispositivo_id = ?", Timestamp.class, dispositivoId);
        return Optional.ofNullable(t).map(Timestamp::toLocalDateTime);
    }

    @Override public int contarEnDia(String dispositivoId, LocalDate dia) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM reporte WHERE dispositivo_id = ? AND fecha_registro >= ? AND fecha_registro < ?",
                Integer.class, dispositivoId, Timestamp.valueOf(dia.atStartOfDay()), Timestamp.valueOf(dia.plusDays(1).atStartOfDay()));
        return n == null ? 0 : n;
    }

    @Override public List<Reporte> listarCasos(Integer zonaId, Integer categoriaId, EstadoReporte estado, int pagina, int tamano) {
        List<Object> p = new ArrayList<>();
        String where = filtros(zonaId, categoriaId, estado, p);
        p.add(tamano); p.add((long) pagina * tamano);
        return jdbc.query("SELECT " + COLUMNAS + " FROM reporte WHERE reporte_original_id IS NULL" + where
                + " ORDER BY fecha_registro DESC LIMIT ? OFFSET ?", MAPEO, p.toArray());
    }

    @Override public int contarCasos(Integer zonaId, Integer categoriaId, EstadoReporte estado) {
        List<Object> p = new ArrayList<>();
        String where = filtros(zonaId, categoriaId, estado, p);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM reporte WHERE reporte_original_id IS NULL" + where, Integer.class, p.toArray());
        return n == null ? 0 : n;
    }

    /** Solo se agregan fragmentos fijos; los valores van como parámetros. */
    private static String filtros(Integer zonaId, Integer categoriaId, EstadoReporte estado, List<Object> p) {
        StringBuilder sb = new StringBuilder();
        if (zonaId != null) { sb.append(" AND zona_id = ?"); p.add(zonaId); }
        if (categoriaId != null) { sb.append(" AND categoria_id = ?"); p.add(categoriaId); }
        if (estado != null) { sb.append(" AND estado = ?"); p.add(estado.etiqueta()); }
        return sb.toString();
    }

    @Override public int contarVinculados(int casoId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM reporte WHERE reporte_original_id = ?", Integer.class, casoId);
        return n == null ? 0 : n;
    }

    @Override public void actualizarEstado(int reporteId, EstadoReporte nuevo) {
        jdbc.update("UPDATE reporte SET estado = ? WHERE reporte_id = ?", nuevo.etiqueta(), reporteId);
    }

    // ---- R01 · R03 ----
    @Override public List<ConteoZona> reportesPorZona(LocalDateTime desde, LocalDateTime hasta) {
        return jdbc.query("SELECT z.nombre AS zona, COUNT(*) AS reportes FROM reporte r JOIN zona z ON z.zona_id = r.zona_id "
                + "WHERE r.fecha_registro >= ? AND r.fecha_registro < ? GROUP BY z.nombre ORDER BY reportes DESC",
                (rs, n) -> new ConteoZona(rs.getString("zona"), rs.getInt("reportes")),
                Timestamp.valueOf(desde), Timestamp.valueOf(hasta));
    }

    @Override public List<PuntoReporte> puntosPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        // Un vinculado muestra el código y el estado de su caso original (RN-01).
        return jdbc.query("SELECT r.lat, r.lng, z.nombre AS zona, c.nombre AS categoria, "
                + "COALESCE(o.estado, r.estado) AS estado, COALESCE(o.codigo_seguimiento, r.codigo_seguimiento) AS codigo "
                + "FROM reporte r JOIN zona z ON z.zona_id = r.zona_id JOIN categoria c ON c.categoria_id = r.categoria_id "
                + "LEFT JOIN reporte o ON o.reporte_id = r.reporte_original_id "
                + "WHERE r.lat IS NOT NULL AND r.lng IS NOT NULL AND r.fecha_registro >= ? AND r.fecha_registro < ? "
                + "ORDER BY r.fecha_registro DESC LIMIT 1000",
                (rs, n) -> new PuntoReporte(rs.getDouble("lat"), rs.getDouble("lng"), rs.getString("zona"),
                        rs.getString("categoria"), rs.getString("estado"), rs.getString("codigo")),
                Timestamp.valueOf(desde), Timestamp.valueOf(hasta));
    }

    @Override public Map<EstadoReporte, Integer> casosPorEstado(LocalDateTime desde, LocalDateTime hasta) {
        Map<EstadoReporte, Integer> m = new EnumMap<>(EstadoReporte.class);
        jdbc.query("SELECT estado, COUNT(*) AS casos FROM reporte WHERE reporte_original_id IS NULL "
                + "AND fecha_registro >= ? AND fecha_registro < ? GROUP BY estado",
                rs -> { m.put(EstadoReporte.desdeEtiqueta(rs.getString("estado")), rs.getInt("casos")); },
                Timestamp.valueOf(desde), Timestamp.valueOf(hasta));
        return m;
    }

    @Override public int contarVinculadosPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM reporte WHERE reporte_original_id IS NOT NULL "
                + "AND fecha_registro >= ? AND fecha_registro < ?", Integer.class, Timestamp.valueOf(desde), Timestamp.valueOf(hasta));
        return n == null ? 0 : n;
    }

    @Override public double porcentajeActualizados(LocalDateTime desde, LocalDateTime hasta) {
        Double d = jdbc.queryForObject("SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE estado <> 'Recibido') / NULLIF(COUNT(*), 0), 1) "
                + "FROM reporte WHERE reporte_original_id IS NULL AND fecha_registro >= ? AND fecha_registro < ?",
                Double.class, Timestamp.valueOf(desde), Timestamp.valueOf(hasta));
        return d == null ? 0 : d;
    }
}
