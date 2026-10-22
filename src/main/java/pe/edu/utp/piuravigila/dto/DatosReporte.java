package pe.edu.utp.piuravigila.dto;

/** Datos del formulario P01. dispositivoId sale de una cookie anónima; sumaEsperada, de la sesión del servidor. */
public record DatosReporte(int categoriaId, int zonaId, String descripcion,
                           String fotoNombre, byte[] fotoBytes, Double lat, Double lng,
                           String dispositivoId, Integer respuestaSuma, Integer sumaEsperada) {}
