package pe.edu.utp.piuravigila.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.ReporteService;

/**
 * Semana 13 · flujo P01/P02 en el Controller (MockMvc, Service simulado). JaCoCo mostraba 26 % de ramas cubiertas en
 * ReporteController, el flujo público de mayor riesgo; estas pruebas cubren las decisiones del formulario y del duplicado.
 */
class ReporteControllerTest {
    private ReporteService servicio;
    private MockMvc mvc;
    private MockHttpSession sesion;
    private String token;
    private final Reporte casoAbierto = new Reporte(7, "PR-2026-0007", null, 1, 1, "Bache", "fotos/a.jpg", null, null,
            EstadoReporte.EN_PROCESO, "disp-0000-0001", LocalDateTime.of(2026, 11, 2, 9, 0));

    /** Vista vacía: se verifica el nombre de la vista y el modelo, no el HTML. */
    private static final AbstractView VISTA = new AbstractView() {
        @Override protected void renderMergedOutputModel(Map<String, Object> m, HttpServletRequest rq, HttpServletResponse rs) { }
    };

    @BeforeEach
    void preparar() {
        servicio = mock(ReporteService.class);
        when(servicio.categoriasActivas()).thenReturn(List.of(new Categoria(1, "Bache en la vía", true)));
        when(servicio.zonas()).thenReturn(List.of(new Zona(1, "Piura", null)));
        mvc = MockMvcBuilders.standaloneSetup(new ReporteController(servicio)).setViewResolvers((v, l) -> VISTA).build();
        sesion = new MockHttpSession();
        token = Csrf.token(sesion);
        sesion.setAttribute("sumaEsperada", 7);
    }

    private org.springframework.test.web.servlet.ResultActions enviar() throws Exception {
        return mvc.perform(multipart("/reportar").file("foto", new byte[] {1, 2, 3}).session(sesion).param("_csrf", token)
                .param("categoriaId", "1").param("zonaId", "1").param("descripcion", "Bache grande")
                .param("respuestaSuma", " 7 ").cookie(new Cookie("pv_dispositivo", "disp-0000-0001")));
    }

    @Test @DisplayName("TC-P01-01 · registro válido muestra el código y descarta la suma usada")
    void registro_valido_muestra_codigo() throws Exception {
        when(servicio.registrarReporte(any())).thenReturn(ResultadoRegistro.registrado("PR-2026-0001"));

        enviar().andExpect(status().isOk()).andExpect(view().name("registrado"))
                .andExpect(model().attribute("codigo", "PR-2026-0001")).andExpect(model().attribute("vinculado", false));

        ArgumentCaptor<DatosReporte> datos = ArgumentCaptor.forClass(DatosReporte.class);
        verify(servicio).registrarReporte(datos.capture());
        assertEquals(7, datos.getValue().respuestaSuma(), "la respuesta se recorta y se convierte a número");
        assertEquals(7, datos.getValue().sumaEsperada(), "la suma esperada sale de la sesión, no del formulario");
        assertEquals("disp-0000-0001", datos.getValue().dispositivoId());
        assertNull(sesion.getAttribute("sumaEsperada"), "una suma resuelta no se puede reutilizar");
    }

    @Test @DisplayName("TC-P01-02 · caso abierto: pregunta al ciudadano y guarda el reporte pendiente en la sesión")
    void caso_abierto_pide_confirmacion() throws Exception {
        when(servicio.registrarReporte(any())).thenReturn(ResultadoRegistro.duplicado(casoAbierto));

        enviar().andExpect(view().name("duplicado")).andExpect(model().attribute("caso", casoAbierto))
                .andExpect(model().attributeExists("csrf"));
        assertNotNull(sesion.getAttribute("pendiente"));
        assertEquals(7, sesion.getAttribute("casoId"));
    }

    @Test @DisplayName("TC-P01-03 · «Sí, es el mismo» vincula una sola vez; reenviar no crea otro vínculo")
    void confirmar_duplicado_vincula_una_sola_vez() throws Exception {
        when(servicio.registrarReporte(any())).thenReturn(ResultadoRegistro.duplicado(casoAbierto));
        when(servicio.responderDuplicado(any(), eq(true), eq(7))).thenReturn(ResultadoRegistro.vinculado("PR-2026-0007"));
        enviar();

        mvc.perform(post("/reportar/duplicado").session(sesion).param("_csrf", token).param("respuesta", "si"))
                .andExpect(view().name("registrado")).andExpect(model().attribute("vinculado", true))
                .andExpect(model().attribute("codigo", "PR-2026-0007"));
        // El resolver de vistas de la prueba también recibe "redirect:..."; por eso se verifica el nombre de la vista.
        mvc.perform(post("/reportar/duplicado").session(sesion).param("_csrf", token).param("respuesta", "si"))
                .andExpect(view().name("redirect:/reportar"));

        verify(servicio, times(1)).responderDuplicado(any(), eq(true), eq(7));
        assertNull(sesion.getAttribute("pendiente"));
    }

    @Test @DisplayName("TC-P01-04 · «No, es otro» pide al Service un caso nuevo")
    void negar_duplicado_crea_caso_nuevo() throws Exception {
        when(servicio.registrarReporte(any())).thenReturn(ResultadoRegistro.duplicado(casoAbierto));
        when(servicio.responderDuplicado(any(), eq(false), eq(7))).thenReturn(ResultadoRegistro.registrado("PR-2026-0008"));
        enviar();

        mvc.perform(post("/reportar/duplicado").session(sesion).param("_csrf", token).param("respuesta", "no"))
                .andExpect(view().name("registrado")).andExpect(model().attribute("codigo", "PR-2026-0008"));
    }

    @Test @DisplayName("TC-P01-05 · rechazo del Service vuelve al formulario con el motivo, los datos y una suma nueva")
    void rechazo_vuelve_al_formulario() throws Exception {
        when(servicio.registrarReporte(any())).thenReturn(ResultadoRegistro.rechazado("Espera antes de enviar otro reporte: faltan 40 s", true));

        enviar().andExpect(view().name("reportar"))
                .andExpect(model().attribute("error", "Espera antes de enviar otro reporte: faltan 40 s"))
                .andExpect(model().attribute("desc", "Bache grande")).andExpect(model().attribute("catSel", 1))
                .andExpect(model().attributeExists("pregunta"));
        assertNotNull(sesion.getAttribute("sumaEsperada"), "se genera otra suma de verificación");
    }

    @Test @DisplayName("TC-P01-06 · cookie de dispositivo con formato inválido se reemplaza por una nueva HttpOnly")
    void cookie_invalida_se_reemplaza() throws Exception {
        mvc.perform(get("/reportar").cookie(new Cookie("pv_dispositivo", "<script>")))
                .andExpect(view().name("reportar"))
                .andExpect(cookie().exists("pv_dispositivo")).andExpect(cookie().httpOnly("pv_dispositivo", true));
    }

    @Test @DisplayName("TC-P02-01 · código inexistente muestra un mensaje genérico; vacío no consulta")
    void consulta_codigo_inexistente_o_vacio() throws Exception {
        when(servicio.consultarEstado("PR-2026-9999")).thenThrow(new IllegalArgumentException("Reporte inexistente"));

        mvc.perform(get("/consultar").param("codigo", "PR-2026-9999"))
                .andExpect(view().name("consultar"))
                .andExpect(model().attribute("error", "Código no encontrado. Verifica que esté bien escrito"))
                .andExpect(model().attributeDoesNotExist("consulta"));
        mvc.perform(get("/consultar").param("codigo", "  ")).andExpect(model().attributeDoesNotExist("error"));
        verify(servicio, times(1)).consultarEstado(anyString());
    }
}
