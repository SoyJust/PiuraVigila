package pe.edu.utp.piuravigila.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import pe.edu.utp.piuravigila.config.CabecerasSeguridad;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.*;

/**
 * Semana 14 · pruebas de seguridad (TC-SEC). Cada prueba expresa el comportamiento seguro esperado; las que fallan en
 * la línea inicial se registran como hallazgo (SEC-xx) antes de corregir.
 */
class SeguridadWebTest {
    private static final UsuarioMunicipal ADMIN = new UsuarioMunicipal(1, "Administrador", "admin", "h", RolUsuario.ADMINISTRADOR);
    private AuthService auth;
    private ReporteService reportes;
    private MockMvc panel, ciudadano;

    @BeforeEach
    void montar() {
        auth = mock(AuthService.class);
        reportes = mock(ReporteService.class);
        when(reportes.categoriasActivas()).thenReturn(List.of(new Categoria(1, "Bache en la vía", true)));
        when(reportes.zonas()).thenReturn(List.of(new Zona(1, "Piura", null)));
        ViewResolver vistas = (nombre, l) -> nombre.startsWith("redirect:")
                ? new RedirectView(nombre.substring("redirect:".length())) : (View) (m, rq, rs) -> { };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneId.of("America/Lima"));
        panel = MockMvcBuilders.standaloneSetup(new PanelController(auth, reportes, mock(CategoriaService.class), mock(DashboardService.class), reloj))
                .setViewResolvers(vistas).addFilters(new CabecerasSeguridad()).build();
        ciudadano = MockMvcBuilders.standaloneSetup(new ReporteController(reportes))
                .setViewResolvers(vistas).addFilters(new CabecerasSeguridad()).build();
    }

    @Test @DisplayName("TC-SEC-04 · POST /login sin token CSRF se rechaza y no intenta autenticar")
    void login_sin_token_csrf_se_rechaza() throws Exception {
        panel.perform(post("/login").param("usuario", "admin").param("clave", "x"))
                .andExpect(status().isForbidden()).andExpect(request().sessionAttributeDoesNotExist("usuario"));
        verify(auth, never()).autenticar(anyString(), anyString());
    }

    @Test @DisplayName("TC-SEC-10 · login con token válido cambia el identificador de sesión (anti fijación)")
    void login_con_token_cambia_el_id_de_sesion() throws Exception {
        when(auth.autenticar("admin", "clave-de-prueba")).thenReturn(Optional.of(ADMIN));
        MockHttpSession s = new MockHttpSession();
        String token = (String) panel.perform(get("/login").session(s)).andReturn().getModelAndView().getModel().get("csrf");
        assertNotNull(token, "el formulario de login debe recibir un token CSRF");
        String antes = s.getId();

        panel.perform(post("/login").session(s).param("_csrf", token).param("usuario", "admin").param("clave", "clave-de-prueba"))
                .andExpect(redirectedUrl("/panel")).andExpect(request().sessionAttribute("usuario", ADMIN));
        assertNotEquals(antes, s.getId());
    }

    @Test @DisplayName("TC-SEC-05 · las respuestas llevan CSP estricta para scripts, anti-clickjacking, nosniff y Referrer-Policy")
    void cabeceras_de_seguridad() throws Exception {
        ciudadano.perform(get("/consultar"))
                .andExpect(header().string("Content-Security-Policy", allOf(
                        containsString("default-src 'self'"), containsString("script-src 'self' https://cdnjs.cloudflare.com;"),
                        containsString("frame-ancestors 'none'"), containsString("object-src 'none'"),
                        containsString("base-uri 'self'"), containsString("form-action 'self'"))))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("geolocation=(self)")));
        String csp = ciudadano.perform(get("/consultar")).andReturn().getResponse().getHeader("Content-Security-Policy");
        String scriptSrc = csp == null ? "" : csp.replaceAll(".*script-src([^;]*);.*", "$1");
        assertFalse(scriptSrc.contains("unsafe-inline"), "los scripts en línea no deben permitirse");
    }

    @Test @DisplayName("TC-SEC-06 · la cookie de dispositivo es HttpOnly y SameSite=Lax")
    void cookie_de_dispositivo_samesite() throws Exception {
        ciudadano.perform(get("/reportar"))
                .andExpect(cookie().httpOnly("pv_dispositivo", true))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test @DisplayName("TC-SEC-08 · las plantillas no tienen scripts en línea y Leaflet se carga con SRI")
    void plantillas_sin_scripts_en_linea_y_con_sri() throws Exception {
        Pattern scriptEnLinea = Pattern.compile("<script(?![^>]*\\bsrc=)[^>]*>");
        Pattern externo = Pattern.compile("<(script|link)[^>]*(src|href)=\"https://cdnjs[^\"]+\"[^>]*>");
        try (var archivos = Files.list(Path.of("src/main/resources/templates"))) {
            for (Path p : archivos.toList()) {
                String html = Files.readString(p, StandardCharsets.UTF_8);
                assertFalse(scriptEnLinea.matcher(html).find(), "script en línea en " + p.getFileName());
                Matcher m = externo.matcher(html);
                while (m.find()) {
                    assertTrue(m.group().contains("integrity=\"sha"), "falta SRI en " + p.getFileName() + ": " + m.group());
                    assertTrue(m.group().contains("crossorigin=\"anonymous\""), "falta crossorigin en " + p.getFileName());
                }
            }
        }
    }
}
