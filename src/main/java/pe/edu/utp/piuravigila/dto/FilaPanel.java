package pe.edu.utp.piuravigila.dto;
import pe.edu.utp.piuravigila.model.Reporte;
/** Fila del listado de P03: un caso y cuántos reportes se vincularon a él (columna «Vinc.»). */
public record FilaPanel(Reporte caso, int vinculados) {}
