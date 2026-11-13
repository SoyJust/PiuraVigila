package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.config.FotoStorageDisco;
import pe.edu.utp.piuravigila.config.RetencionFotos;
import pe.edu.utp.piuravigila.dao.ReporteDAO;

/** v1.1 · fotos más livianas y sin metadatos (TC-F01 a TC-F06) y límites de intentos (TC-L01 a TC-L02). */
class FotosYLimitesTest {

    private static byte[] jpeg(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        for (int y = 0; y < h; y += 7) for (int x = 0; x < w; x += 7) { g.setColor(new Color((x * 31 + y * 17) & 0xFFFFFF)); g.fillRect(x, y, 7, 7); }
        g.dispose();
        var out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    /** Inserta un segmento APP1 Exif con Orientation y un texto «GPS-CASA» (simula metadatos privados). */
    private static byte[] conExif(byte[] jpg, int orientacion) {
        byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientacion, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] extra = "GPS-CASA".getBytes(StandardCharsets.US_ASCII);
        int largo = 2 + 6 + tiff.length + extra.length;
        var out = new ByteArrayOutputStream();
        out.write(jpg, 0, 2);
        out.write(0xFF); out.write(0xE1); out.write(largo >> 8); out.write(largo & 0xFF);
        out.writeBytes(new byte[]{'E', 'x', 'i', 'f', 0, 0});
        out.writeBytes(tiff); out.writeBytes(extra);
        out.write(jpg, 2, jpg.length - 2);
        return out.toByteArray();
    }

    private static BufferedImage leer(Path dir, String ruta) throws Exception {
        return ImageIO.read(dir.resolve(ruta.substring("fotos/".length())).toFile());
    }

    @Test @DisplayName("TC-F01 · una foto grande se guarda reducida a 1600 px como máximo y pesa menos")
    void reduce_fotos_grandes() throws Exception {
        Path dir = Files.createTempDirectory("pv-f");
        byte[] original = jpeg(3000, 2000);
        String ruta = new FotoStorageDisco(dir.toString()).guardar("grande.jpg", original);
        BufferedImage g = leer(dir, ruta);
        assertEquals(1600, g.getWidth());
        assertEquals(1067, g.getHeight());
        assertTrue(Files.size(dir.resolve(ruta.substring(6))) < original.length);
    }

    @Test @DisplayName("TC-F02 · se descartan los metadatos EXIF (por ejemplo, la ubicación GPS)")
    void descarta_metadatos() throws Exception {
        Path dir = Files.createTempDirectory("pv-f");
        byte[] foto = conExif(jpeg(400, 300), 1);
        assertTrue(new String(foto, StandardCharsets.ISO_8859_1).contains("GPS-CASA"));
        String ruta = new FotoStorageDisco(dir.toString()).guardar("f.jpg", foto);
        String guardada = new String(Files.readAllBytes(dir.resolve(ruta.substring(6))), StandardCharsets.ISO_8859_1);
        assertFalse(guardada.contains("GPS-CASA"));
        assertFalse(guardada.contains("Exif"));
    }

    @Test @DisplayName("TC-F03 · una foto girada del celular (Orientation 6) se guarda derecha")
    void endereza_segun_exif() throws Exception {
        Path dir = Files.createTempDirectory("pv-f");
        byte[] foto = conExif(jpeg(400, 200), 6);
        assertEquals(6, FotoStorageDisco.orientacionExif(foto));
        BufferedImage g = leer(dir, new FotoStorageDisco(dir.toString()).guardar("f.jpg", foto));
        assertEquals(200, g.getWidth());
        assertEquals(400, g.getHeight());
    }

    @Test @DisplayName("TC-F04 · el mismo archivo exacto enviado otra vez se rechaza (spam de la misma foto)")
    void rechaza_la_misma_foto() throws Exception {
        var storage = new FotoStorageDisco(Files.createTempDirectory("pv-f").toString());
        byte[] foto = jpeg(300, 200);
        storage.guardar("a.jpg", foto);
        var e = assertThrows(IllegalArgumentException.class, () -> storage.guardar("b.jpg", foto));
        assertTrue(e.getMessage().contains("misma foto"));
        assertDoesNotThrow(() -> storage.guardar("c.jpg", jpeg(301, 200))); // otra foto sí pasa
    }

    @Test @DisplayName("TC-F05 · leer y eliminar solo aceptan archivos del directorio de fotos (sin recorrer rutas)")
    void leer_y_eliminar_seguros() throws Exception {
        Path dir = Files.createTempDirectory("pv-f");
        Files.writeString(dir.getParent().resolve("secreto-pv.txt"), "no");
        var storage = new FotoStorageDisco(dir.toString());
        String ruta = storage.guardar("a.jpg", jpeg(50, 50));
        assertTrue(storage.leer(ruta).isPresent());
        assertTrue(storage.leer("fotos/../secreto-pv.txt").isEmpty());
        assertTrue(storage.leer("C:/Windows/win.ini").isEmpty());
        assertFalse(storage.eliminar("fotos/../secreto-pv.txt"));
        assertTrue(storage.eliminar(ruta));
        assertTrue(storage.leer(ruta).isEmpty());
    }

    @Test @DisplayName("TC-F06 · la retención borra solo las fotos de casos atendidos hace más de N días; con 0 no borra")
    void retencion_de_fotos() throws Exception {
        Path dir = Files.createTempDirectory("pv-f");
        var storage = new FotoStorageDisco(dir.toString());
        String vieja = storage.guardar("v.jpg", jpeg(60, 40)), nueva = storage.guardar("n.jpg", jpeg(61, 40));
        AtomicReference<LocalDateTime> limite = new AtomicReference<>();
        ReporteDAO dao = org.mockito.Mockito.mock(ReporteDAO.class);
        org.mockito.Mockito.when(dao.fotosDeCasosAtendidosAntesDe(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> { limite.set(i.getArgument(0)); return List.of(vieja); });
        Clock reloj = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneId.of("America/Lima"));
        assertEquals(0, new RetencionFotos(dao, storage, reloj, 0).limpiar());
        assertEquals(1, new RetencionFotos(dao, storage, reloj, 365).limpiar());
        assertEquals(LocalDateTime.of(2025, 11, 13, 7, 0), limite.get());
        assertTrue(storage.leer(vieja).isEmpty());
        assertTrue(storage.leer(nueva).isPresent());
    }

    @Test @DisplayName("TC-F07 · «bomba de descompresión»: un PNG de 25 megapíxeles que pesa poco se rechaza")
    void rechaza_bomba_de_descompresion() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(5000, 5000, BufferedImage.TYPE_BYTE_BINARY), "png", out); // todo negro: comprime muchísimo
        assertTrue(out.size() < 2 * 1024 * 1024, "el archivo pesa poco: " + out.size() + " bytes");
        var storage = new FotoStorageDisco(Files.createTempDirectory("pv-f").toString());
        var e = assertThrows(IllegalArgumentException.class, () -> storage.guardar("bomba.png", out.toByteArray()));
        assertTrue(e.getMessage().contains("20 megapíxeles"));
    }

    @Test @DisplayName("TC-L01 · el limitador bloquea al llegar al máximo y libera cuando pasa la ventana")
    void limitador_por_ventana() {
        var ahora = new AtomicReference<>(Instant.parse("2026-11-13T12:00:00Z"));
        Clock reloj = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId z) { return this; }
            public Instant instant() { return ahora.get(); }
        };
        var lim = new LimitadorIntentos(reloj);
        Duration v = Duration.ofMinutes(10);
        for (int i = 0; i < 3; i++) assertFalse(lim.registrarYExcede("ip:1", 3, v));
        assertTrue(lim.registrarYExcede("ip:1", 3, v));
        assertFalse(lim.registrarYExcede("ip:2", 3, v), "otra clave no se ve afectada");
        ahora.set(ahora.get().plus(Duration.ofMinutes(11)));
        assertFalse(lim.registrarYExcede("ip:1", 3, v));
    }

    @Test @DisplayName("TC-L02 · bloqueado() no cuenta intentos y segundosParaLiberar informa la espera")
    void limitador_consulta() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
        var lim = new LimitadorIntentos(reloj);
        for (int i = 0; i < 5; i++) { assertFalse(lim.bloqueado("login-u:admin", 5, Duration.ofMinutes(15))); lim.registrar("login-u:admin"); }
        assertTrue(lim.bloqueado("login-u:admin", 5, Duration.ofMinutes(15)));
        assertEquals(900, lim.segundosParaLiberar("login-u:admin", 5, Duration.ofMinutes(15)));
        lim.limpiar("login-u:admin");
        assertFalse(lim.bloqueado("login-u:admin", 5, Duration.ofMinutes(15)));
    }
}
