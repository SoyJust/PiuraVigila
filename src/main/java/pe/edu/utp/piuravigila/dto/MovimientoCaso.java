package pe.edu.utp.piuravigila.dto;
import java.time.LocalDateTime;
import pe.edu.utp.piuravigila.model.EstadoReporte;
/** v1.1 · una fila del historial en el detalle del caso (solo personal): quién lo cambió y su comentario. responsable null = sistema. */
public record MovimientoCaso(EstadoReporte estado, LocalDateTime fecha, String responsable, String comentario) {}
