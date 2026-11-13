package pe.edu.utp.piuravigila.dao;
import java.time.*;
import java.util.*;
import pe.edu.utp.piuravigila.dto.ConteoZona;
import pe.edu.utp.piuravigila.dto.FilaExportacion;
import pe.edu.utp.piuravigila.dto.PuntoReporte;
import pe.edu.utp.piuravigila.model.*;

public interface ReporteDAO {
    /**
     * v1.1 · bloqueo de la transacción actual sobre una clave (dispositivo o categoría+zona): dos peticiones simultáneas
     * con la misma clave se atienden una después de otra, así la segunda ve lo que guardó la primera.
     */
    default void bloquear(String clave) { }
    /** Inserta un caso original y devuelve su código PR-AAAA-#### (lo genera la BD con la secuencia, RNF-03). */
    String insertarCaso(Reporte r);
    /** Inserta un reporte vinculado al caso: sin código propio (RN-01). */
    void insertarVinculado(Reporte r, int casoId);
    Optional<Reporte> buscarPorCodigo(String codigo);
    Optional<Reporte> buscarPorId(int reporteId);
    /** Caso original abierto (Recibido/En proceso) con la misma categoría y zona. */
    Optional<Reporte> buscarCasoAbierto(int categoriaId, int zonaId);
    Optional<LocalDateTime> ultimoEnvio(String dispositivoId);
    /** Reportes del dispositivo en el día, incluidos los vinculados (RF-06). */
    int contarEnDia(String dispositivoId, LocalDate dia);
    List<Reporte> listarCasos(Integer zonaId, Integer categoriaId, EstadoReporte estado, int pagina, int tamano);
    int contarCasos(Integer zonaId, Integer categoriaId, EstadoReporte estado);
    int contarVinculados(int casoId);
    void actualizarEstado(int reporteId, EstadoReporte nuevo);
    // Dashboard (R01 a R03)
    List<ConteoZona> reportesPorZona(LocalDateTime desde, LocalDateTime hasta);
    Map<EstadoReporte, Integer> casosPorEstado(LocalDateTime desde, LocalDateTime hasta);
    int contarVinculadosPeriodo(LocalDateTime desde, LocalDateTime hasta);
    double porcentajeActualizados(LocalDateTime desde, LocalDateTime hasta);
    /** Reportes del periodo con ubicación marcada (incluye vinculados), para el mapa de P04. */
    default List<PuntoReporte> puntosPeriodo(LocalDateTime desde, LocalDateTime hasta) { return List.of(); }
    /** v1.1 · reportes vinculados a un caso (detalle del caso). */
    default List<Reporte> listarVinculados(int casoId) { return List.of(); }
    /** v1.1 · casos originales del periodo con nombres y fechas, para la hoja «Casos» del Excel. */
    default List<FilaExportacion> casosParaExportar(LocalDateTime desde, LocalDateTime hasta) { return List.of(); }
    /** v1.1 · fotos (caso y vinculados) de casos atendidos antes de la fecha límite: política de retención. */
    default List<String> fotosDeCasosAtendidosAntesDe(LocalDateTime limite) { return List.of(); }
}
