package pe.edu.utp.piuravigila.controller;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import pe.edu.utp.piuravigila.service.*;
import pe.edu.utp.piuravigila.model.*;
import java.time.*;
class MejorasSeguridadTest {
    @Test void reporte_sin_csrf_no_invoca_registro() throws Exception {
        var servicio = mock(ReporteService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ReporteController(servicio)).setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/templates/reportar.html")).build();
        mvc.perform(post("/reportar")).andExpect(status().isForbidden());
        verify(servicio, never()).registrarReporte(any());
    }
    @Test void duplicado_sin_csrf_no_invoca_servicio() throws Exception {
        var servicio = mock(ReporteService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ReporteController(servicio)).setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/templates/reportar.html")).build();
        mvc.perform(post("/reportar/duplicado").param("respuesta","si")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }
    @Test void exportacion_exige_sesion() throws Exception {
        var servicio = mock(ExportacionService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ExportacionController(servicio, Clock.systemUTC())).build();
        mvc.perform(get("/panel/exportar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }
    @Test void operador_puede_exportar_conteos() throws Exception {
        var servicio = mock(ExportacionService.class);
        when(servicio.zonas(any(),any())).thenReturn(new byte[]{1,2});
        var s = new MockHttpSession(); s.setAttribute("usuario",new UsuarioMunicipal(1,"Operador","operador","hash",RolUsuario.OPERADOR));
        var mvc = MockMvcBuilders.standaloneSetup(new ExportacionController(servicio, Clock.systemUTC())).build();
        mvc.perform(get("/panel/exportar").session(s)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void exportacion_rechaza_periodo_invertido() throws Exception {
        var servicio = mock(ExportacionService.class);
        var s = new MockHttpSession(); s.setAttribute("usuario",new UsuarioMunicipal(1,"Admin","admin","hash",RolUsuario.ADMINISTRADOR));
        var mvc = MockMvcBuilders.standaloneSetup(new ExportacionController(servicio, Clock.systemUTC())).build();
        mvc.perform(get("/panel/exportar").session(s).param("desde","2026-10-02").param("hasta","2026-10-01")).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
    @Test void reporte_con_csrf_llega_a_validacion_del_servicio() throws Exception {
        var servicio = mock(ReporteService.class);
        when(servicio.registrarReporte(any())).thenReturn(pe.edu.utp.piuravigila.dto.ResultadoRegistro.rechazado("Datos incompletos", true));
        var sesion = new MockHttpSession(); String token = Csrf.token(sesion);
        var mvc = MockMvcBuilders.standaloneSetup(new ReporteController(servicio)).setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/templates/reportar.html")).build();
        mvc.perform(post("/reportar").session(sesion).param("_csrf",token)).andExpect(status().isOk());
        verify(servicio).registrarReporte(any());
    }
    @Test void csrf_de_otra_sesion_se_rechaza() throws Exception {
        var servicio = mock(ReporteService.class);
        var s = new MockHttpSession(); Csrf.token(s);
        String otro = Csrf.token(new MockHttpSession());
        var mvc = MockMvcBuilders.standaloneSetup(new ReporteController(servicio)).setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/templates/reportar.html")).build();
        mvc.perform(post("/reportar").session(s).param("_csrf",otro)).andExpect(status().isForbidden());
        verify(servicio,never()).registrarReporte(any());
    }
}
