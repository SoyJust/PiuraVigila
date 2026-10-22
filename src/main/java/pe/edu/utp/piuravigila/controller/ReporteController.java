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
    private final ReporteService service;

    public ReporteController(ReporteService service) { this.service = service; }

    @GetMapping("/")
    public String inicio() { return "redirect:/reportar"; }

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
            m.addAttribute("caso", r.casoExistente());
            m.addAttribute("categorias", nombresCategoria());
            return "duplicado";
        }
        return resultado(r, m, s, categoriaId, zonaId, descripcion);
    }

    @PostMapping("/reportar/duplicado")
    public String responder(@RequestParam String respuesta, Model m, HttpSession s) {
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
    public String consultar(@RequestParam(required = false) String codigo, Model m) {
        m.addAttribute("codigo", codigo == null ? "" : codigo);
        if (codigo != null && !codigo.isBlank()) {
            try {
                m.addAttribute("consulta", service.consultarEstado(codigo));
                m.addAttribute("categorias", nombresCategoria());
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
        Cookie c = new Cookie(COOKIE, id);
        c.setHttpOnly(true); c.setPath("/"); c.setMaxAge(60 * 60 * 24 * 365);
        rs.addCookie(c);
        return id;
    }
}
