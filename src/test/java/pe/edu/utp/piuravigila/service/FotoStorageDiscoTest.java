package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.config.FotoStorageDisco;

/** La foto es solo evidencia: extensión permitida y nombre aleatorio dentro del directorio configurado. */
class FotoStorageDiscoTest {
    private static Path temporal() throws Exception { return Files.createTempDirectory("pv-fotos"); }

    @Test void rechaza_extension_no_permitida() throws Exception {
        FotoStorageDisco s = new FotoStorageDisco(temporal().toString());
        assertThrows(IllegalArgumentException.class, () -> s.guardar("virus.exe", new byte[10]));
        assertThrows(IllegalArgumentException.class, () -> s.guardar(null, new byte[10]));
    }

    @Test void guarda_con_nombre_aleatorio_y_extension_en_minuscula() throws Exception {
        Path dir = temporal();
        String ruta = new FotoStorageDisco(dir.toString()).guardar("MiFoto.JPG", new byte[10]);
        assertTrue(ruta.startsWith("fotos/") && ruta.endsWith(".jpg"));
        assertFalse(ruta.contains("MiFoto"));
        assertTrue(Files.exists(dir.resolve(ruta.substring("fotos/".length()))));
    }

    @Test void un_nombre_con_ruta_relativa_no_escapa_del_directorio() throws Exception {
        Path dir = temporal();
        String ruta = new FotoStorageDisco(dir.toString()).guardar("../../escape.png", new byte[10]);
        assertTrue(Files.exists(dir.resolve(ruta.substring("fotos/".length()))));
        assertFalse(Files.exists(dir.getParent().resolve("escape.png")));
    }
}
