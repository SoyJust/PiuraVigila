package pe.edu.utp.piuravigila.controller;

import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.*;

/** P03 · panel municipal (RF-03, RF-07, RNF-01) y resumen R01 a R03 (sin fotos). Sesión del servidor. */
@Controller
public class PanelController {
    private static final String SESION = "usuario";
    private final AuthService auth;
    private final ReporteService reportes;
    private final CategoriaService categorias;
    private final DashboardService dashboard;
    private final Clock reloj;

    public PanelController(AuthService auth, ReporteService reportes, CategoriaService categorias,
                           DashboardService dashboard, Clock reloj) {
        this.auth = auth; this.reportes = reportes; this.categorias = categorias; this.dashboard = dashboard; this.reloj = reloj;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String requerida, Model m) {
        if (requerida != null) m.addAttribute("error", "Sesión requerida");
        return "login";
    }

    @PostMapping("/login")
    public String iniciar(@RequestParam String usuario, @RequestParam String clave, HttpServletRequest rq, Model m) {
        Optional<UsuarioMunicipal> u = auth.autenticar(usuario, clave);
        if (u.isEmpty()) { m.addAttribute("error", "Usuario o contraseña incorrectos"); return "login"; }
        rq.changeSessionId(); // evita fijación de sesión
        rq.getSession().setAttribute(SESION, u.get());
        return "redirect:/panel";
    }

    @PostMapping("/logout")
    public String salir(@RequestParam(name = "_csrf", required = false) String csrf, HttpSession s) {
        if (Csrf.valido(s, csrf)) s.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/panel")
    public String panel(@RequestParam(required = false) Integer zonaId, @RequestParam(required = false) Integer categoriaId,
                        @RequestParam(required = false) String estado, @RequestParam(defaultValue = "0") int pagina,
                        @RequestParam(defaultValue = "false") boolean resumen,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        HttpSession s, Model m) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        EstadoReporte filtroEstado = null;
        if (estado != null && !estado.isBlank()) {
            try { filtroEstado = EstadoReporte.desdeEtiqueta(estado); } catch (IllegalArgumentException e) { filtroEstado = null; }
        }
        int total = reportes.totalCasos(zonaId, categoriaId, filtroEstado);
        m.addAttribute("usuario", u);
        m.addAttribute("csrf", Csrf.token(s));
        m.addAttribute("filas", reportes.listarPanel(zonaId, categoriaId, filtroEstado, pagina));
        m.addAttribute("total", total);
        m.addAttribute("pagina", Math.max(pagina, 0));
        m.addAttribute("paginas", Math.max(1, (total + ReporteService.TAMANO_PAGINA - 1) / ReporteService.TAMANO_PAGINA));
        m.addAttribute("categorias", reportes.categoriasActivas());
        m.addAttribute("zonas", reportes.zonas());
        m.addAttribute("estados", EstadoReporte.values());
        m.addAttribute("fzona", zonaId); m.addAttribute("fcat", categoriaId); m.addAttribute("festado", estado);
        m.addAttribute("avisoOtra", categorias.umbralOtraSuperado());
        m.addAttribute("pctOtra", categorias.porcentajeOtraMes());
        m.addAttribute("verResumen", resumen);
        if (resumen) {
            LocalDate h = hasta != null ? hasta : LocalDate.now(reloj);
            LocalDate d = desde != null ? desde : h.minusDays(30);
            try {
                m.addAttribute("desde", d); m.addAttribute("hasta", h);
                m.addAttribute("r01", dashboard.zonasCriticas(d, h));
                m.addAttribute("r03", dashboard.resumenGestion(d, h));
                if (u.esAdministrador()) m.addAttribute("r02", dashboard.tiemposAtencion(d, h)); // R02: administrador
            } catch (IllegalArgumentException e) { m.addAttribute("errorResumen", e.getMessage()); }
        }
        return "panel";
    }

    @PostMapping("/panel/estado")
    public String cambiarEstado(@RequestParam int reporteId, @RequestParam String nuevo,
                                @RequestParam(name = "_csrf", required = false) String csrf,
                                HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return "redirect:/panel"; }
        try {
            reportes.cambiarEstado(reporteId, EstadoReporte.desdeEtiqueta(nuevo), u);
            ra.addFlashAttribute("ok", "Estado actualizado");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/panel";
    }

    @PostMapping("/panel/categoria")
    public String habilitarCategoria(@RequestParam String nombre, @RequestParam(name = "_csrf", required = false) String csrf,
                                     HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return "redirect:/panel"; }
        if (!u.esAdministrador()) { ra.addFlashAttribute("error", "Solo el administrador puede habilitar categorías"); return "redirect:/panel"; }
        try { categorias.habilitar(nombre); ra.addFlashAttribute("ok", "Categoría habilitada"); }
        catch (IllegalArgumentException | IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/panel";
    }
}
