package pe.edu.utp.piuravigila.model;
import java.time.LocalDateTime;

/**
 * Caso original (reporteOriginalId == null, con código propio) o reporte vinculado
 * (reporteOriginalId != null, sin código propio; manda el estado del original).
 */
public record Reporte(int reporteId, String codigoSeguimiento, Integer reporteOriginalId,
                      int categoriaId, int zonaId, String descripcion, String fotoUrl,
                      Double lat, Double lng, EstadoReporte estado, String dispositivoId,
                      LocalDateTime fechaRegistro) {
    public boolean esCasoOriginal() { return reporteOriginalId == null; }
    public boolean estaAbierto() { return estado.estaAbierto(); }
}
