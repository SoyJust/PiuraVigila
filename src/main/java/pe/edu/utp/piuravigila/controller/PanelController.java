package pe.edu.utp.piuravigila.controller;

import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.piuravigila.dto.ConteoZona;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.*;

/** P03 · panel municipal (RF-03, RF-07, RNF-01), resumen R01 a R03 (sin fotos) y P04 · mapa de zonas críticas (RF-04). Sesión del servidor. */
@Controller
public class PanelController {
    private static final String SESION = "usuario";
    private final AuthService auth;
    private final ReporteService reportes;
    private final CategoriaService categorias;
    private final DashboardService dashboard;
    private final Clock reloj;
    private final LimitadorIntentos limitador;
    /** v1.1 · fuerza bruta en el login: 5 fallos por usuario o 20 por IP en 15 minutos bloquean nuevos intentos. */
    static final int MAX_FALLOS_USUARIO = 5, MAX_FALLOS_IP = 20;
    static final Duration VENTANA_LOGIN = Duration.ofMinutes(15);

    public PanelController(AuthService auth, ReporteService reportes, CategoriaService categorias,
                           DashboardService dashboard, Clock reloj) {
        this(auth, reportes, categorias, dashboard, reloj, new LimitadorIntentos(reloj));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PanelController(AuthService auth, ReporteService reportes, CategoriaService categorias,
                           DashboardService dashboard, Clock reloj, LimitadorIntentos limitador) {
        this.auth = auth; this.reportes = reportes; this.categorias = categorias; this.dashboard = dashboard; this.reloj = reloj;
        this.limitador = limitador;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String requerida, HttpSession s, Model m) {
        if (requerida != null) m.addAttribute("error", "Sesión requerida");
        m.addAttribute("csrf", Csrf.token(s)); // SEC-06: el formulario de login también lleva token CSRF
        return "login";
    }

    @PostMapping("/login")
    public String iniciar(@RequestParam String usuario, @RequestParam String clave,
                          @RequestParam(name = "_csrf", required = false) String csrf,
                          HttpServletRequest rq, HttpServletResponse rs, Model m) {
        HttpSession s = rq.getSession(); // crea la sesión si aún no existe: changeSessionId() la exige
        if (!Csrf.valido(s, csrf)) { // SEC-06: sin token válido no se intenta autenticar (login CSRF)
            rs.setStatus(HttpServletResponse.SC_FORBIDDEN);
            m.addAttribute("error", "Solicitud no válida. Vuelve a cargar la página de inicio de sesión.");
            m.addAttribute("csrf", Csrf.token(s));
            return "login";
        }
        String claveUsuario = "login-u:" + usuario.trim().toLowerCase(Locale.ROOT), claveIp = "login-ip:" + rq.getRemoteAddr();
        if (limitador.bloqueado(claveUsuario, MAX_FALLOS_USUARIO, VENTANA_LOGIN) || limitador.bloqueado(claveIp, MAX_FALLOS_IP, VENTANA_LOGIN)) {
            long seg = Math.max(limitador.segundosParaLiberar(claveUsuario, MAX_FALLOS_USUARIO, VENTANA_LOGIN),
                    limitador.segundosParaLiberar(claveIp, MAX_FALLOS_IP, VENTANA_LOGIN));
            rs.setStatus(429);
            m.addAttribute("error", "Demasiados intentos fallidos. Espera " + Math.max(1, (seg + 59) / 60) + " min antes de volver a intentarlo.");
            m.addAttribute("csrf", Csrf.token(s));
            return "login";
        }
        Optional<UsuarioMunicipal> u = auth.autenticar(usuario, clave);
        if (u.isEmpty()) {
            limitador.registrar(claveUsuario); limitador.registrar(claveIp);
            m.addAttribute("error", "Usuario o contraseña incorrectos");
            m.addAttribute("csrf", Csrf.token(s));
            return "login";
        }
        limitador.limpiar(claveUsuario);
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
        // v1.0 · tarjetas de cifras arriba del panel (todos los casos, sin filtros)
        m.addAttribute("kRecibido", reportes.totalCasos(null, null, EstadoReporte.RECIBIDO));
        m.addAttribute("kProceso", reportes.totalCasos(null, null, EstadoReporte.EN_PROCESO));
        m.addAttribute("kAtendido", reportes.totalCasos(null, null, EstadoReporte.ATENDIDO));
        m.addAttribute("filtrado", zonaId != null || categoriaId != null || filtroEstado != null);
        if (resumen) {
            LocalDate h = hasta != null ? hasta : LocalDate.now(reloj);
            LocalDate d = desde != null ? desde : h.minusDays(30);
            try {
                m.addAttribute("desde", d); m.addAttribute("hasta", h);
                List<ConteoZona> r01 = dashboard.zonasCriticas(d, h);
                m.addAttribute("r01", r01);
                m.addAttribute("r01Max", r01.stream().mapToInt(ConteoZona::reportes).max().orElse(0));
                m.addAttribute("r03", dashboard.resumenGestion(d, h));
                if (u.esAdministrador()) m.addAttribute("r02", dashboard.tiemposAtencion(d, h)); // R02: administrador
            } catch (IllegalArgumentException e) { m.addAttribute("errorResumen", e.getMessage()); }
        }
        return "panel";
    }

    /** P04 · mapa esquemático de zonas críticas (RF-04): cuadrícula por zona con el conteo del periodo (incluye vinculados). Operador y administrador. */
    @GetMapping("/panel/mapa")
    public String mapa(@RequestParam(defaultValue = "7d") String periodo, HttpSession s, Model m) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        LocalDate hasta = LocalDate.now(reloj);
        LocalDate desde = switch (periodo) {
            case "mes" -> hasta.minusMonths(1).plusDays(1);
            case "3m" -> hasta.minusMonths(3).plusDays(1);
            case "anio" -> hasta.minusYears(1).plusDays(1);
            default -> { periodo = "7d"; yield hasta.minusDays(6); }
        };
        Map<String, Integer> conteos = new HashMap<>();
        for (ConteoZona c : dashboard.zonasCriticas(desde, hasta)) conteos.put(c.zona(), c.reportes());
        List<ConteoZona> celdas = new ArrayList<>(); // las zonas sin reportes también aparecen (con 0)
        for (Zona z : reportes.zonas()) celdas.add(new ConteoZona(z.nombre(), conteos.getOrDefault(z.nombre(), 0)));
        m.addAttribute("usuario", u);
        m.addAttribute("csrf", Csrf.token(s));
        m.addAttribute("periodo", periodo);
        m.addAttribute("desde", desde); m.addAttribute("hasta", hasta);
        m.addAttribute("celdas", celdas);
        m.addAttribute("puntos", dashboard.puntosReportes(desde, hasta));
        m.addAttribute("maximo", celdas.stream().mapToInt(ConteoZona::reportes).max().orElse(0));
        return "mapa";
    }

    @PostMapping("/panel/estado")
    public String cambiarEstado(@RequestParam int reporteId, @RequestParam String nuevo,
                                @RequestParam(required = false) String comentario,
                                @RequestParam(required = false) String volver,
                                @RequestParam(name = "_csrf", required = false) String csrf,
                                HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        // v1.1 · desde el detalle del caso se vuelve al detalle (solo códigos con formato válido: nada de redirecciones abiertas)
        String destino = volver != null && volver.matches("PR-\\d{4}-\\d{4,}") ? "redirect:/panel/caso?codigo=" + volver : "redirect:/panel";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return destino; }
        try {
            reportes.cambiarEstado(reporteId, EstadoReporte.desdeEtiqueta(nuevo), u, comentario);
            ra.addFlashAttribute("ok", "Estado actualizado");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return destino;
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
