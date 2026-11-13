package pe.edu.utp.piuravigila.controller;

import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.piuravigila.model.EstadoReporte;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
import pe.edu.utp.piuravigila.service.ReporteService;

/**
 * v1.1 · P03b detalle del caso y fotos. Solo para personal con sesión: la foto puede mostrar rostros o placas, por eso
 * nunca se publica en la consulta del vecino ni se sirve como archivo estático.
 */
@Controller
public class CasoController {
    private static final String SESION = "usuario";
    private final ReporteService reportes;

    public CasoController(ReporteService reportes) { this.reportes = reportes; }

    @GetMapping("/panel/caso")
    public String detalle(@RequestParam(defaultValue = "") String codigo, HttpSession s, Model m, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        try {
            m.addAttribute("detalle", reportes.detalleCaso(codigo));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "No existe un caso con el código «" + codigo + "».");
            return "redirect:/panel";
        }
        m.addAttribute("usuario", u);
        m.addAttribute("csrf", Csrf.token(s));
        m.addAttribute("estados", EstadoReporte.values());
        m.addAttribute("maxComentario", ReporteService.MAX_COMENTARIO);
        return "caso";
    }

    /** Foto de un reporte (caso o vinculado). 401 sin sesión, 404 si no existe o ya se eliminó por antigüedad. */
    @GetMapping("/panel/foto/{reporteId}")
    public ResponseEntity<byte[]> foto(@PathVariable int reporteId, HttpSession s) {
        if (!(s.getAttribute(SESION) instanceof UsuarioMunicipal)) return ResponseEntity.status(401).build();
        return reportes.foto(reporteId)
                .map(b -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                        .contentType(b.length > 3 && (b[0] & 0xFF) == 0x89 ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                        .body(b))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
