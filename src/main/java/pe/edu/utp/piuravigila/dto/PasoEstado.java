package pe.edu.utp.piuravigila.dto;
import java.time.LocalDateTime;
import pe.edu.utp.piuravigila.model.EstadoReporte;
/** Un paso de la línea de tiempo del caso (P02). La fecha es null mientras el estado no se haya alcanzado. */
public record PasoEstado(EstadoReporte estado, LocalDateTime fecha, boolean actual) {
    public boolean alcanzado() { return fecha != null; }
}
