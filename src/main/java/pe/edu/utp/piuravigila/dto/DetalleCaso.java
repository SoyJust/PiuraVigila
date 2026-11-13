package pe.edu.utp.piuravigila.dto;
import java.util.List;
import pe.edu.utp.piuravigila.model.Reporte;
/** v1.1 · P03b detalle del caso para el personal: datos, foto, historial y reportes vinculados (con sus fotos). */
public record DetalleCaso(Reporte caso, String categoria, String zona, List<MovimientoCaso> movimientos, List<Reporte> vinculados) {}
