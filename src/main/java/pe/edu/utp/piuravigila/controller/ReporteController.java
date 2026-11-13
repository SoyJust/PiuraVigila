package pe.edu.utp.piuravigila.controller;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.ReporteService;

/** Pantallas del ciudadano: P01 (registrar) y P02 (consultar). El Controller coordina; las reglas viven en el Service. */
@Controller
public class ReporteController {
    private static final String COOKIE = "pv_dispositivo";
    private static final Pattern FORMATO_DISPOSITIVO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    /**
     * v1.1 · como máximo 20 envíos cada 10 minutos desde una misma IP. Es holgado a propósito: muchas personas pueden
     * compartir una IP (wifi de un colegio, datos móviles con CGNAT); el límite fino es el de 60 s y 3/día por dispositivo.
     */
    static final int MAX_ENVIOS_IP = 20;
    static final java.time.Duration VENTANA_IP = java.time.Duration.ofMinutes(10);
    /** v1.1 · como máximo 60 consultas de código cada 10 minutos por IP: frena a quien prueba códigos uno por uno. */
    static final int MAX_CONSULTAS_IP = 60;
    private final ReporteService service;
    private final pe.edu.utp.piuravigila.service.LimitadorIntentos limitador;

    public ReporteController(ReporteService service) {
        this(service, new pe.edu.utp.piuravigila.service.LimitadorIntentos(java.time.Clock.systemDefaultZone()));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ReporteController(ReporteService service, pe.edu.utp.piuravigila.service.LimitadorIntentos limitador) {
        this.service = service; this.limitador = limitador;
    }

    /** v1.0 · página de inicio: qué es PiuraVigila, cómo funciona y cifras públicas agregadas (sin datos personales). */
    @GetMapping("/")
    public String inicio(Model m) {
        int recibidos = service.totalCasos(null, null, EstadoReporte.RECIBIDO);
        int enProceso = service.totalCasos(null, null, EstadoReporte.EN_PROCESO);
        int atendidos = service.totalCasos(null, null, EstadoReporte.ATENDIDO);
        int descartados = service.totalCasos(null, null, EstadoReporte.DESCARTADO);
        int total = recibidos + enProceso + atendidos + descartados;
        int validos = total - descartados; // v1.2 · el % resuelto no cuenta los reportes descartados
        m.addAttribute("total", total);
        m.addAttribute("atendidos", atendidos);
        m.addAttribute("enAtencion", recibidos + enProceso);
        m.addAttribute("pctAtendidos", validos == 0 ? 0 : Math.round(atendidos * 100.0 / validos));
        m.addAttribute("zonas", service.zonas().size());
        m.addAttribute("categorias", service.categoriasActivas());
        return "inicio";
    }

    @GetMapping("/reportar")
    public String formulario(Model m, HttpSession s, HttpServletRequest rq, HttpServletResponse rs) {
        dispositivo(rq, rs);
        return vistaFormulario(m, s, null, 0, 0, "");
    }

    @PostMapping("/reportar")
    public String enviar(@RequestParam(defaultValue = "0") int categoriaId, @RequestParam(defaultValue = "0") int zonaId,
                         @RequestParam(defaultValue = "") String descripcion, @RequestParam(required = false) MultipartFile foto,
                         @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
                         @RequestParam(defaultValue = "") String respuestaSuma,
                         Model m, HttpSession s, HttpServletRequest rq, HttpServletResponse rs) {
        if (!Csrf.valido(s, rq.getParameter("_csrf"))) {
            rs.setStatus(403);
            return vistaFormulario(m, s, "Solicitud no válida. Vuelve a enviar el formulario.", categoriaId, zonaId, descripcion);
        }
        if (limitador.registrarYExcede("envio-ip:" + rq.getRemoteAddr(), MAX_ENVIOS_IP, VENTANA_IP)) {
            rs.setStatus(429);
            return vistaFormulario(m, s, "Demasiados envíos desde esta conexión. Intenta de nuevo en unos minutos.", categoriaId, zonaId, descripcion);
        }
        byte[] bytes = null;
        try { if (foto != null) bytes = foto.getBytes(); } catch (IOException e) { bytes = null; }
        DatosReporte d = new DatosReporte(categoriaId, zonaId, descripcion, foto == null ? null : foto.getOriginalFilename(),
                bytes, lat, lng, dispositivo(rq, rs), entero(respuestaSuma), (Integer) s.getAttribute("sumaEsperada"));
        ResultadoRegistro r;
        try { r = service.registrarReporte(d); }
        catch (IllegalArgumentException e) { r = ResultadoRegistro.rechazado(e.getMessage(), true); }

        if (r.tipo() == ResultadoRegistro.Tipo.DUPLICADO_PENDIENTE) {
            s.setAttribute("pendiente", d);
            s.setAttribute("casoId", r.casoExistente().reporteId());
            m.addAttribute("csrf", Csrf.token(s));
            m.addAttribute("caso", r.casoExistente());
            m.addAttribute("categorias", nombresCategoria());
            return "duplicado";
        }
        return resultado(r, m, s, categoriaId, zonaId, descripcion);
    }

    @PostMapping("/reportar/duplicado")
    public String responder(@RequestParam String respuesta, @RequestParam(name = "_csrf", required = false) String csrf, Model m, HttpSession s) {
        if (!Csrf.valido(s, csrf)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Solicitud no válida");
        DatosReporte pendiente = (DatosReporte) s.getAttribute("pendiente");
        Integer casoId = (Integer) s.getAttribute("casoId");
        if (pendiente == null || casoId == null) return "redirect:/reportar";
        s.removeAttribute("pendiente"); s.removeAttribute("casoId");
        ResultadoRegistro r;
        try { r = service.responderDuplicado(pendiente, "si".equals(respuesta), casoId); }
        catch (IllegalArgumentException e) { r = ResultadoRegistro.rechazado(e.getMessage(), true); }
        return resultado(r, m, s, pendiente.categoriaId(), pendiente.zonaId(), pendiente.descripcion());
    }

    @GetMapping("/consultar")
    public String consultar(@RequestParam(required = false) String codigo, Model m, HttpServletRequest rq, HttpServletResponse rs) {
        m.addAttribute("codigo", codigo == null ? "" : codigo);
        if (codigo != null && !codigo.isBlank()
                && limitador.registrarYExcede("consulta-ip:" + rq.getRemoteAddr(), MAX_CONSULTAS_IP, VENTANA_IP)) {
            rs.setStatus(429);
            m.addAttribute("error", "Demasiadas consultas seguidas desde esta conexión. Espera unos minutos.");
            return "consultar";
        }
        if (codigo != null && !codigo.isBlank()) {
            try {
                ConsultaEstado c = service.consultarEstado(codigo);
                m.addAttribute("consulta", c);
                m.addAttribute("categorias", nombresCategoria());
                m.addAttribute("zona", service.zonas().stream().filter(z -> z.zonaId() == c.caso().zonaId())
                        .map(Zona::nombre).findFirst().orElse("—"));
            } catch (IllegalArgumentException e) {
                m.addAttribute("error", "Código no encontrado. Verifica que esté bien escrito");
            }
        }
        return "consultar";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fotoExcedida(Model m, HttpSession s) {
        return vistaFormulario(m, s, "Foto demasiado pesada", 0, 0, "");
    }

    // ---- auxiliares ----
    private String resultado(ResultadoRegistro r, Model m, HttpSession s, int cat, int zona, String desc) {
        if (r.tipo() == ResultadoRegistro.Tipo.REGISTRADO || r.tipo() == ResultadoRegistro.Tipo.VINCULADO) {
            s.removeAttribute("sumaEsperada");
            m.addAttribute("codigo", r.codigo());
            m.addAttribute("vinculado", r.tipo() == ResultadoRegistro.Tipo.VINCULADO);
            return "registrado";
        }
        return vistaFormulario(m, s, r.mensaje(), cat, zona, desc);
    }

    private String vistaFormulario(Model m, HttpSession s, String error, int cat, int zona, String desc) {
        int a = ThreadLocalRandom.current().nextInt(1, 10), b = ThreadLocalRandom.current().nextInt(1, 10);
        s.setAttribute("sumaEsperada", a + b);
        m.addAttribute("csrf", Csrf.token(s));
        m.addAttribute("pregunta", a + " + " + b);
        m.addAttribute("categorias", service.categoriasActivas());
        m.addAttribute("zonas", service.zonas());
        m.addAttribute("error", error);
        m.addAttribute("catSel", cat); m.addAttribute("zonaSel", zona); m.addAttribute("desc", desc);
        return "reportar";
    }

    private Map<Integer, String> nombresCategoria() {
        return service.categoriasActivas().stream().collect(Collectors.toMap(Categoria::categoriaId, Categoria::nombre));
    }

    private static Integer entero(String t) {
        try { return Integer.valueOf(t.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** Dispositivo = cookie anónima (sin datos personales). Limitación: borrar la cookie reinicia los límites. */
    private static String dispositivo(HttpServletRequest rq, HttpServletResponse rs) {
        if (rq.getCookies() != null) {
            for (Cookie c : rq.getCookies()) {
                if (COOKIE.equals(c.getName()) && FORMATO_DISPOSITIVO.matcher(c.getValue()).matches()) return c.getValue();
            }
        }
        String id = UUID.randomUUID().toString();
        // SEC-05: HttpOnly y SameSite=Lax (no viaja en peticiones iniciadas por otros sitios).
        rs.addHeader("Set-Cookie", org.springframework.http.ResponseCookie.from(COOKIE, id)
                .httpOnly(true).path("/").maxAge(java.time.Duration.ofDays(365)).sameSite("Lax").build().toString());
        return id;
    }
}
