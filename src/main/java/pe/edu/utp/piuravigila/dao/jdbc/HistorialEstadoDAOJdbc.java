package pe.edu.utp.piuravigila.dao.jdbc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.utp.piuravigila.dao.HistorialEstadoDAO;
import pe.edu.utp.piuravigila.dto.TiempoCategoria;
import pe.edu.utp.piuravigila.model.*;

@Repository
public class HistorialEstadoDAOJdbc implements HistorialEstadoDAO {
    private final JdbcTemplate jdbc;
    public HistorialEstadoDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void registrar(HistorialEstado h) {
        jdbc.update("INSERT INTO historial_estado(reporte_id, usuario_id, estado_nuevo, fecha_cambio) VALUES (?, CAST(? AS INT), ?, ?)",
                h.reporteId(), h.usuarioId(), h.estadoNuevo().etiqueta(), Timestamp.valueOf(h.fechaCambio()));
    }

    @Override public List<HistorialEstado> listarPorReporte(int reporteId) {
        return jdbc.query("SELECT historial_id, reporte_id, usuario_id, estado_nuevo, fecha_cambio FROM historial_estado "
                + "WHERE reporte_id = ? ORDER BY fecha_cambio, historial_id",
                (rs, n) -> new HistorialEstado(rs.getInt("historial_id"), rs.getInt("reporte_id"), (Integer) rs.getObject("usuario_id"),
                        EstadoReporte.desdeEtiqueta(rs.getString("estado_nuevo")), rs.getTimestamp("fecha_cambio").toLocalDateTime()), reporteId);
    }

    @Override public Optional<LocalDateTime> ultimaActualizacion(int reporteId) {
        Timestamp t = jdbc.queryForObject("SELECT MAX(fecha_cambio) FROM historial_estado WHERE reporte_id = ?", Timestamp.class, reporteId);
        return Optional.ofNullable(t).map(Timestamp::toLocalDateTime);
    }

    @Override public List<TiempoCategoria> tiempoPromedio(LocalDateTime desde, LocalDateTime hasta) {
        return jdbc.query("SELECT c.nombre AS categoria, COUNT(*) AS casos_atendidos, "
                + "ROUND(AVG(EXTRACT(EPOCH FROM (h.fecha_cambio - r.fecha_registro)) / 86400)::numeric, 1) AS dias_promedio "
                + "FROM reporte r JOIN categoria c ON c.categoria_id = r.categoria_id "
                + "JOIN historial_estado h ON h.reporte_id = r.reporte_id AND h.estado_nuevo = 'Atendido' "
                + "WHERE r.reporte_original_id IS NULL AND r.fecha_registro >= ? AND r.fecha_registro < ? "
                + "GROUP BY c.nombre ORDER BY dias_promedio DESC",
                (rs, n) -> new TiempoCategoria(rs.getString("categoria"), rs.getInt("casos_atendidos"), rs.getDouble("dias_promedio")),
                Timestamp.valueOf(desde), Timestamp.valueOf(hasta));
    }
}
