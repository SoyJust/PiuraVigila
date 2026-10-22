package pe.edu.utp.piuravigila.dto;
import java.util.Map;
import pe.edu.utp.piuravigila.model.EstadoReporte;
public record ResumenGestion(Map<EstadoReporte, Integer> casosPorEstado, int duplicadosEvitados,
                             double pctActualizados, double pctOtraMes, double umbralOtra, boolean superaUmbral) {}
