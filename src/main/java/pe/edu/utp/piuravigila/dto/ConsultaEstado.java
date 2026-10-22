package pe.edu.utp.piuravigila.dto;
import java.time.LocalDateTime;
import pe.edu.utp.piuravigila.model.Reporte;
public record ConsultaEstado(Reporte caso, LocalDateTime ultimaActualizacion) {}
