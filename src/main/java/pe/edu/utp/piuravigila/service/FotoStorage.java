package pe.edu.utp.piuravigila.service;

/** Guarda la foto (solo evidencia, una por reporte) y devuelve la ruta que se guarda en reporte.foto_url. */
public interface FotoStorage {
    String guardar(String nombreOriginal, byte[] contenido);
}
