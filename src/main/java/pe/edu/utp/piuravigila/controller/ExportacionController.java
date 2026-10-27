package pe.edu.utp.piuravigila.controller;

import java.time.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
import pe.edu.utp.piuravigila.service.ExportacionService;

@Controller
public class ExportacionController {
    private final ExportacionService exportacion;
    private final Clock reloj;
    public ExportacionController(ExportacionService exportacion, Clock reloj) { this.exportacion = exportacion; this.reloj = reloj; }
    @GetMapping("/panel/exportar")
    public ResponseEntity<byte[]> descargar(HttpSession sesion,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (!(sesion.getAttribute("usuario") instanceof UsuarioMunicipal)) return ResponseEntity.status(401).build();
        LocalDate fin = hasta == null ? LocalDate.now(reloj) : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(30) : desde;
        if (inicio.isAfter(fin) || inicio.isBefore(fin.minusYears(5))) return ResponseEntity.badRequest().build();
        byte[] contenido = exportacion.zonas(inicio, fin);
        org.slf4j.LoggerFactory.getLogger(ExportacionController.class).info("Exportación de zonas desde={} hasta={}", inicio, fin);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PiuraVigila_Zonas.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(contenido);
    }
}
