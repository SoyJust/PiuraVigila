package pe.edu.utp.piuravigila.dto;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.utp.piuravigila.model.Reporte;
/** pasos: Recibido → En proceso → Atendido, en orden; los que aún no se alcanzan no llevan fecha. */
public record ConsultaEstado(Reporte caso, LocalDateTime ultimaActualizacion, List<PasoEstado> pasos) {
    public ConsultaEstado(Reporte caso, LocalDateTime ultimaActualizacion) { this(caso, ultimaActualizacion, List.of()); }
}
