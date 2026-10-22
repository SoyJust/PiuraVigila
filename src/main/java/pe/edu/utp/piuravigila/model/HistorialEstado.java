package pe.edu.utp.piuravigila.model;
import java.time.LocalDateTime;
/** usuarioId es null solo para el «Recibido» automático del sistema (ck_historial_responsable). */
public record HistorialEstado(int historialId, int reporteId, Integer usuarioId,
                              EstadoReporte estadoNuevo, LocalDateTime fechaCambio) {}
