package pe.edu.utp.piuravigila.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.utp.piuravigila.service.FotoStorage;

/** Guarda la foto en disco con nombre aleatorio (nunca el nombre enviado por el usuario). */
@Component
public class FotoStorageDisco implements FotoStorage {
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png");
    private final Path directorio;

    public FotoStorageDisco(@Value("${piuravigila.foto.directorio:fotos}") String directorio) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String nombreOriginal, byte[] contenido) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal.toLowerCase(Locale.ROOT);
        int punto = nombre.lastIndexOf('.');
        String ext = punto >= 0 ? nombre.substring(punto + 1) : "";
        if (!EXTENSIONES.contains(ext)) throw new IllegalArgumentException("La foto debe ser JPG o PNG.");
        validarImagen(ext, contenido);
        try {
            Files.createDirectories(directorio);
            String archivo = UUID.randomUUID() + "." + ext;
            Files.write(directorio.resolve(archivo), contenido, StandardOpenOption.CREATE_NEW);
            org.slf4j.LoggerFactory.getLogger(FotoStorageDisco.class).info(
                    "Foto guardada archivo={} sha256={}", archivo,
                    com.google.common.hash.Hashing.sha256().hashBytes(contenido).toString());
            return "fotos/" + archivo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto.", e);
        }
    }
    private static void validarImagen(String ext, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 2097152)
            throw new IllegalArgumentException("La foto debe tener entre 1 byte y 2 MB.");
        try (var input = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("El archivo no contiene una imagen válida.");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                String esperado = ext.equals("jpg") ? "jpeg" : ext;
                if (!esperado.equals(formato)) throw new IllegalArgumentException("La extensión no coincide con la imagen.");
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || (long) w * h > 20000000L)
                    throw new IllegalArgumentException("La imagen supera el límite de 20 megapíxeles.");
                if (reader.read(0) == null) throw new IllegalArgumentException("Imagen inválida.");
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("No se pudo leer la imagen.", e); }
    }
}
