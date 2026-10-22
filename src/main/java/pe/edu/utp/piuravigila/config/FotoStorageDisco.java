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
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");
    private final Path directorio;

    public FotoStorageDisco(@Value("${piuravigila.foto.directorio:fotos}") String directorio) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String nombreOriginal, byte[] contenido) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal.toLowerCase(Locale.ROOT);
        int punto = nombre.lastIndexOf('.');
        String ext = punto >= 0 ? nombre.substring(punto + 1) : "";
        if (!EXTENSIONES.contains(ext)) throw new IllegalArgumentException("La foto debe ser JPG, PNG o WEBP.");
        try {
            Files.createDirectories(directorio);
            String archivo = UUID.randomUUID() + "." + ext;
            Files.write(directorio.resolve(archivo), contenido, StandardOpenOption.CREATE_NEW);
            return "fotos/" + archivo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto.", e);
        }
    }
}
