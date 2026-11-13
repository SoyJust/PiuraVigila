package pe.edu.utp.piuravigila.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.*;

/** v1.1 · abuso por fuerza bruta y por IP, y acceso al detalle y a las fotos (TC-A01 a TC-A06). */
class AbusoYCasoControllerTest {
    private static final UsuarioMunicipal OP = new UsuarioMunicipal(2, "Operador", "operador", "h", RolUsuario.OPERADOR);
    private AuthService auth;
    private ReporteService reportes;
    private MockMvc panel, ciudadano, caso;
    private final ViewResolver vistas = (nombre, l) -> nombre.startsWith("redirect:")
            ? new RedirectView(nombre.substring("redirect:".length())) : (View) (m, rq, rs) -> { };

    @BeforeEach
    void montar() {
        auth = mock(AuthService.class);
        reportes = mock(ReporteService.class);
        when(reportes.categoriasActivas()).thenReturn(List.of(new Categoria(1, "Bache en la vía", true)));
        when(reportes.zonas()).thenReturn(List.of(new Zona(1, "Piura", null)));
        Clock reloj = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneId.of("America/Lima"));
        panel = MockMvcBuilders.standaloneSetup(new PanelController(auth, reportes, mock(CategoriaService.class), mock(DashboardService.class), reloj))
                .setViewResolvers(vistas).build();
        ciudadano = MockMvcBuilders.standaloneSetup(new ReporteController(reportes)).setViewResolvers(vistas).build();
        caso = MockMvcBuilders.standaloneSetup(new CasoController(reportes)).setViewResolvers(vistas).build();
    }

    private String token(MockHttpSession s) throws Exception {
        return (String) panel.perform(get("/login").session(s)).andReturn().getModelAndView().getModel().get("csrf");
    }

    @Test @DisplayName("TC-A01 · tras 5 contraseñas incorrectas el usuario queda bloqueado 15 min, aunque luego acierte")
    void fuerza_bruta_en_login() throws Exception {
        when(auth.autenticar(eq("operador"), anyString())).thenReturn(Optional.empty());
        when(auth.autenticar("operador", "correcta-123")).thenReturn(Optional.of(OP));
        MockHttpSession s = new MockHttpSession();
        for (int i = 0; i < 5; i++) {
            panel.perform(post("/login").session(s).param("_csrf", token(s)).param("usuario", "operador").param("clave", "mala" + i))
                    .andExpect(status().isOk()).andExpect(model().attribute("error", "Usuario o contraseña incorrectos"));
        }
        panel.perform(post("/login").session(s).param("_csrf", token(s)).param("usuario", "Operador ").param("clave", "correcta-123"))
                .andExpect(status().isTooManyRequests()).andExpect(request().sessionAttributeDoesNotExist("usuario"));
        verify(auth, never()).autenticar("operador", "correcta-123");
    }

    @Test @DisplayName("TC-A02 · un inicio de sesión correcto reinicia el contador de fallos del usuario")
    void acierto_reinicia_contador() throws Exception {
        when(auth.autenticar(eq("operador"), anyString())).thenReturn(Optional.empty());
        when(auth.autenticar("operador", "correcta-123")).thenReturn(Optional.of(OP));
        MockHttpSession s = new MockHttpSession();
        for (int i = 0; i < 4; i++) panel.perform(post("/login").session(s).param("_csrf", token(s)).param("usuario", "operador").param("clave", "x"));
        panel.perform(post("/login").session(s).param("_csrf", token(s)).param("usuario", "operador").param("clave", "correcta-123"))
                .andExpect(redirectedUrl("/panel"));
        MockHttpSession s2 = new MockHttpSession();
        panel.perform(post("/login").session(s2).param("_csrf", token(s2)).param("usuario", "operador").param("clave", "x"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("TC-A03 · desde una misma IP se aceptan 20 envíos en 10 minutos; el 21.º recibe 429 sin llegar al servicio")
    void limite_de_envios_por_ip() throws Exception {
        when(reportes.registrarReporte(any())).thenReturn(pe.edu.utp.piuravigila.dto.ResultadoRegistro.rechazado("Verificación incorrecta", true));
        MockMultipartFile foto = new MockMultipartFile("foto", "f.jpg", "image/jpeg", new byte[]{1, 2, 3});
        for (int i = 0; i < 20; i++) {
            MockHttpSession s = new MockHttpSession(); s.setAttribute("csrf", "t");
            ciudadano.perform(multipart("/reportar").file(foto).session(s).param("_csrf", "t").with(r -> { r.setRemoteAddr("203.0.113.7"); return r; }))
                    .andExpect(status().isOk());
        }
        MockHttpSession s = new MockHttpSession(); s.setAttribute("csrf", "t");
        ciudadano.perform(multipart("/reportar").file(foto).session(s).param("_csrf", "t").with(r -> { r.setRemoteAddr("203.0.113.7"); return r; }))
                .andExpect(status().isTooManyRequests());
        verify(reportes, times(20)).registrarReporte(any());
        s = new MockHttpSession(); s.setAttribute("csrf", "t");
        ciudadano.perform(multipart("/reportar").file(foto).session(s).param("_csrf", "t").with(r -> { r.setRemoteAddr("198.51.100.4"); return r; }))
                .andExpect(status().isOk()); // otra IP no se ve afectada
    }

    @Test @DisplayName("TC-A07 · probar códigos uno por uno: tras 60 consultas en 10 min desde una IP responde 429")
    void limite_de_consultas_por_ip() throws Exception {
        when(reportes.consultarEstado(anyString())).thenThrow(new IllegalArgumentException("Código no encontrado"));
        for (int i = 1; i <= 60; i++) {
            String codigo = String.format("PR-2026-%04d", i);
            ciudadano.perform(get("/consultar").param("codigo", codigo).with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }))
                    .andExpect(status().isOk());
        }
        ciudadano.perform(get("/consultar").param("codigo", "PR-2026-0061").with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }))
                .andExpect(status().isTooManyRequests());
        verify(reportes, times(60)).consultarEstado(anyString());
        ciudadano.perform(get("/consultar").with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }))
                .andExpect(status().isOk()); // abrir la página sin código no cuenta
    }

    @Test @DisplayName("TC-A04 · sin sesión no se ve el detalle ni las fotos (401)")
    void fotos_y_detalle_requieren_sesion() throws Exception {
        caso.perform(get("/panel/foto/1")).andExpect(status().isUnauthorized());
        caso.perform(get("/panel/caso").param("codigo", "PR-2026-0001")).andExpect(redirectedUrl("/login?requerida"));
        verifyNoInteractions(reportes);
    }

    @Test @DisplayName("TC-A05 · con sesión la foto se entrega como imagen; si ya no existe responde 404")
    void foto_con_sesion() throws Exception {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        when(reportes.foto(1)).thenReturn(Optional.of(jpg));
        when(reportes.foto(2)).thenReturn(Optional.empty());
        caso.perform(get("/panel/foto/1").sessionAttr("usuario", OP))
                .andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
        caso.perform(get("/panel/foto/2").sessionAttr("usuario", OP)).andExpect(status().isNotFound());
    }

    @Test @DisplayName("TC-A06 · cambiar estado desde el detalle vuelve al detalle; un «volver» manipulado vuelve al panel")
    void volver_solo_a_codigos_validos() throws Exception {
        panel.perform(post("/panel/estado").sessionAttr("usuario", OP).sessionAttr("csrf", "t").param("_csrf", "t")
                        .param("reporteId", "1").param("nuevo", "En proceso").param("comentario", "Cuadrilla en camino").param("volver", "PR-2026-0007"))
                .andExpect(redirectedUrl("/panel/caso?codigo=PR-2026-0007"));
        verify(reportes).cambiarEstado(eq(1), eq(EstadoReporte.EN_PROCESO), eq(OP), eq("Cuadrilla en camino"));
        panel.perform(post("/panel/estado").sessionAttr("usuario", OP).sessionAttr("csrf", "t").param("_csrf", "t")
                        .param("reporteId", "1").param("nuevo", "Atendido").param("volver", "https://sitio-malo.example"))
                .andExpect(redirectedUrl("/panel"));
    }
}
