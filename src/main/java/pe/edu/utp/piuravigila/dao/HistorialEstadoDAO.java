package pe.edu.utp.piuravigila.dao;
import java.time.LocalDateTime;
import java.util.*;
import pe.edu.utp.piuravigila.dto.TiempoCategoria;
import pe.edu.utp.piuravigila.model.HistorialEstado;
public interface HistorialEstadoDAO {
    void registrar(HistorialEstado h);
    List<HistorialEstado> listarPorReporte(int reporteId);
    Optional<LocalDateTime> ultimaActualizacion(int reporteId);
    List<TiempoCategoria> tiempoPromedio(LocalDateTime desde, LocalDateTime hasta);
}
