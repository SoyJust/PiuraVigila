package pe.edu.utp.piuravigila.model;
import java.time.LocalDateTime;
/**
 * usuarioId es null solo para el «Recibido» automático del sistema (ck_historial_responsable).
 * v1.1 · comentario: nota opcional del personal (máx. 200), visible para el vecino en la consulta.
 */
public record HistorialEstado(int historialId, int reporteId, Integer usuarioId,
                              EstadoReporte estadoNuevo, LocalDateTime fechaCambio, String comentario) {
    public HistorialEstado(int historialId, int reporteId, Integer usuarioId, EstadoReporte estadoNuevo, LocalDateTime fechaCambio) {
        this(historialId, reporteId, usuarioId, estadoNuevo, fechaCambio, null);
    }
}
