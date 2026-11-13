package pe.edu.utp.piuravigila.config;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.utp.piuravigila.service.FotoStorage;

/**
 * Guarda la foto en disco con nombre aleatorio (nunca el nombre enviado por el usuario).
 *
 * <p>v1.1 · para no gastar disco ni exponer datos personales, la foto <strong>no se guarda tal cual</strong>: se vuelve a
 * codificar como JPEG de 1600 px como máximo en su lado mayor (una foto de celular de 2 MB queda en unos 200 a 400 KB),
 * se endereza según la orientación EXIF y se descartan los metadatos (EXIF puede traer la ubicación GPS de la casa
 * del vecino y el modelo del teléfono). También rechaza el mismo archivo exacto enviado otra vez en las últimas 24 h.</p>
 */
@Component
public class FotoStorageDisco implements FotoStorage {
    private static final Logger LOG = LoggerFactory.getLogger(FotoStorageDisco.class);
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png");
    public static final int LADO_MAXIMO = 1600;
    private static final float CALIDAD_JPEG = 0.82f;
    private static final Duration VENTANA_REPETIDA = Duration.ofHours(24);
    private final Path directorio;
    private final Map<String, Instant> huellasRecientes = new ConcurrentHashMap<>();

    public FotoStorageDisco(@Value("${piuravigila.foto.directorio:fotos}") String directorio) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String nombreOriginal, byte[] contenido) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal.toLowerCase(Locale.ROOT);
        int punto = nombre.lastIndexOf('.');
        String ext = punto >= 0 ? nombre.substring(punto + 1) : "";
        if (!EXTENSIONES.contains(ext)) throw new IllegalArgumentException("La foto debe ser JPG o PNG.");
        BufferedImage imagen = validarImagen(ext, contenido);
        String huella = com.google.common.hash.Hashing.sha256().hashBytes(contenido).toString();
        recordarHuella(huella);
        byte[] reducida = reducir(imagen, ext.startsWith("jp") ? orientacionExif(contenido) : 1);
        try {
            Files.createDirectories(directorio);
            String archivo = UUID.randomUUID() + ".jpg";
            Files.write(directorio.resolve(archivo), reducida, StandardOpenOption.CREATE_NEW);
            LOG.info("Foto guardada archivo={} original={} KB guardada={} KB sha256={}", archivo,
                    contenido.length / 1024, reducida.length / 1024, huella);
            return "fotos/" + archivo;
        } catch (IOException e) {
            huellasRecientes.remove(huella);
            throw new UncheckedIOException("No se pudo guardar la foto.", e);
        }
    }

    /** Lee la foto guardada (para el panel). Solo acepta archivos dentro del directorio configurado. */
    @Override
    public Optional<byte[]> leer(String ruta) {
        return archivo(ruta).filter(Files::isRegularFile).map(p -> {
            try { return Files.readAllBytes(p); } catch (IOException e) { return null; }
        });
    }

    @Override
    public boolean eliminar(String ruta) {
        return archivo(ruta).map(p -> {
            try { return Files.deleteIfExists(p); } catch (IOException e) { return false; }
        }).orElse(false);
    }

    /** «fotos/abc.jpg» → archivo dentro del directorio; cualquier otra forma (rutas, «..») se ignora. */
    private Optional<Path> archivo(String ruta) {
        if (ruta == null || !ruta.startsWith("fotos/")) return Optional.empty();
        String nombre = ruta.substring("fotos/".length());
        if (!nombre.matches("[A-Za-z0-9._-]{1,80}") || nombre.contains("..")) return Optional.empty();
        Path p = directorio.resolve(nombre).normalize();
        return p.startsWith(directorio) ? Optional.of(p) : Optional.empty();
    }

    /** El mismo archivo exacto (misma huella SHA-256) no se acepta dos veces en 24 h, venga de quien venga. */
    private void recordarHuella(String huella) {
        Instant ahora = Instant.now();
        if (huellasRecientes.size() > 20_000) huellasRecientes.values().removeIf(t -> t.isBefore(ahora.minus(VENTANA_REPETIDA)));
        Instant previa = huellasRecientes.putIfAbsent(huella, ahora);
        if (previa != null) {
            if (previa.isAfter(ahora.minus(VENTANA_REPETIDA)))
                throw new IllegalArgumentException("Esta misma foto ya se envió hace poco. Toma una foto nueva del problema.");
            huellasRecientes.put(huella, ahora);
        }
    }

    private static BufferedImage validarImagen(String ext, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 2097152)
            throw new IllegalArgumentException("La foto debe tener entre 1 byte y 2 MB.");
        try (var input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("El archivo no contiene una imagen válida.");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                String esperado = ext.equals("jpg") ? "jpeg" : ext;
                if (!esperado.equals(formato)) throw new IllegalArgumentException("La extensión no coincide con la imagen.");
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || (long) w * h > 20000000L) // evita «bombas» de descompresión
                    throw new IllegalArgumentException("La imagen supera el límite de 20 megapíxeles.");
                BufferedImage img = reader.read(0);
                if (img == null) throw new IllegalArgumentException("Imagen inválida.");
                return img;
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("No se pudo leer la imagen.", e); }
    }

    /** Endereza, reduce a {@link #LADO_MAXIMO} px y codifica en JPEG sin metadatos (fondo blanco si el PNG era transparente). */
    static byte[] reducir(BufferedImage original, int orientacion) {
        boolean gira = orientacion >= 5 && orientacion <= 8;
        int w = gira ? original.getHeight() : original.getWidth(), h = gira ? original.getWidth() : original.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * escala)), nh = Math.max(1, (int) Math.round(h * escala));
        BufferedImage destino = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            g.scale(escala, escala);
            g.drawImage(original, transformacion(orientacion, original.getWidth(), original.getHeight()), null);
        } finally { g.dispose(); }
        try (var out = new ByteArrayOutputStream()) {
            var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            try (var ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam p = writer.getDefaultWriteParam();
                p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                p.setCompressionQuality(CALIDAD_JPEG);
                writer.write(null, new IIOImage(destino, null, null), p); // sin metadatos
            } finally { writer.dispose(); }
            return out.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException("No se pudo procesar la foto.", e); }
    }

    /** Transformación que deja la imagen derecha según la etiqueta EXIF Orientation (1 a 8), en el espacio ya girado. */
    private static AffineTransform transformacion(int o, int w, int h) {
        AffineTransform t = new AffineTransform();
        switch (o) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            case 8 -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            default -> { }
        }
        return t;
    }

    /** Lee la etiqueta EXIF Orientation (0x0112) de un JPEG sin librerías externas. 1 = normal o sin dato. */
    public static int orientacionExif(byte[] b) {
        if (b == null || b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) return 1;
        int i = 2;
        while (i + 4 <= b.length) {
            if ((b[i] & 0xFF) != 0xFF) return 1;
            int marcador = b[i + 1] & 0xFF;
            if (marcador == 0xDA || marcador == 0xD9) return 1;
            int largo = ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
            if (largo < 2 || i + 2 + largo > b.length) return 1;
            if (marcador == 0xE1 && largo >= 16 && b[i + 4] == 'E' && b[i + 5] == 'x' && b[i + 6] == 'i' && b[i + 7] == 'f') {
                int tiff = i + 10;
                boolean le = b[tiff] == 'I';
                long ifd = tiff + u32(b, tiff + 4, le);
                if (ifd + 2 > i + 2 + largo) return 1;
                int n = u16(b, (int) ifd, le);
                for (int k = 0; k < n; k++) {
                    int e = (int) ifd + 2 + k * 12;
                    if (e + 12 > b.length) return 1;
                    if (u16(b, e, le) == 0x0112) { int v = u16(b, e + 8, le); return v >= 1 && v <= 8 ? v : 1; }
                }
                return 1;
            }
            i += 2 + largo;
        }
        return 1;
    }

    private static int u16(byte[] b, int i, boolean le) {
        return le ? (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8) : ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
    }

    private static long u32(byte[] b, int i, boolean le) {
        return le ? (u16(b, i, true) & 0xFFFFL) | ((long) u16(b, i + 2, true) << 16)
                  : ((long) u16(b, i, false) << 16) | (u16(b, i + 2, false) & 0xFFFFL);
    }
}
