package pe.edu.utp.piuravigila.service;

import java.util.Optional;

/** Guarda la foto (evidencia, una por reporte) y devuelve la ruta que se guarda en reporte.foto_url. */
public interface FotoStorage {
    String guardar(String nombreOriginal, byte[] contenido);
    /** Contenido de una foto guardada (para verla en el panel); vacío si ya no existe. */
    default Optional<byte[]> leer(String ruta) { return Optional.empty(); }
    /** Borra el archivo (política de retención); true si existía. */
    default boolean eliminar(String ruta) { return false; }
}
