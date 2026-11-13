package pe.edu.utp.piuravigila.dto;
import java.time.LocalDateTime;
/** v1.1 · una fila de la hoja «Casos» del Excel (sin foto, sin dispositivo ni datos personales). */
public record FilaExportacion(String codigo, LocalDateTime fechaRegistro, String categoria, String zona, String estado,
                              int vinculados, LocalDateTime ultimaActualizacion, LocalDateTime fechaAtendido, String descripcion) {}
