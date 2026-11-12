package pe.edu.utp.piuravigila.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import pe.edu.utp.piuravigila.config.SesionVigente;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.model.RolUsuario;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
import pe.edu.utp.piuravigila.service.UsuarioService;

/** v1.0 · pantallas de usuarios y contraseña (TC-U11 a TC-U15) con MockMvc standalone. */
class UsuarioControllerTest {
    private static final UsuarioMunicipal ADMIN = new UsuarioMunicipal(1, "Administrador", "admin", "h", RolUsuario.ADMINISTRADOR);
    private static final UsuarioMunicipal OPERADOR = new UsuarioMunicipal(2, "Operador", "operador", "h", RolUsuario.OPERADOR);
    private UsuarioService usuarios;
    private UsuarioMunicipalDAO dao;
    private MockMvc mvc;

    @BeforeEach
    void montar() {
        usuarios = mock(UsuarioService.class);
        dao = mock(UsuarioMunicipalDAO.class);
        when(dao.buscarPorId(1)).thenReturn(Optional.of(ADMIN));
        when(dao.buscarPorId(2)).thenReturn(Optional.of(OPERADOR));
        ViewResolver vistas = (nombre, l) -> nombre.startsWith("redirect:")
                ? new RedirectView(nombre.substring("redirect:".length())) : (View) (m, rq, rs) -> { };
        mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(usuarios)).setViewResolvers(vistas)
                .addFilters(new SesionVigente(dao)).build();
    }

    @Test @DisplayName("TC-U11 · el operador no ve la gestión de usuarios")
    void operador_no_entra_a_usuarios() throws Exception {
        mvc.perform(get("/panel/usuarios").sessionAttr("usuario", OPERADOR))
                .andExpect(redirectedUrl("/panel")).andExpect(flash().attributeExists("error"));
        verify(usuarios, never()).listar();
    }

    @Test @DisplayName("TC-U12 · el administrador ve la lista de usuarios")
    void admin_ve_usuarios() throws Exception {
        when(usuarios.listar()).thenReturn(List.of(ADMIN, OPERADOR));
        mvc.perform(get("/panel/usuarios").sessionAttr("usuario", ADMIN))
                .andExpect(status().isOk()).andExpect(view().name("usuarios")).andExpect(model().attributeExists("lista", "csrf"));
    }

    @Test @DisplayName("TC-U13 · crear usuario sin token CSRF se rechaza y no llega al servicio")
    void crear_sin_csrf() throws Exception {
        mvc.perform(post("/panel/usuarios").sessionAttr("usuario", ADMIN).sessionAttr("csrf", "tok")
                        .param("nombre", "Rosa Chávez").param("usuario", "rchavez").param("clave", "Inicio2026").param("rol", "operador"))
                .andExpect(redirectedUrl("/panel/usuarios")).andExpect(flash().attribute("error", "Solicitud no válida"));
        verifyNoInteractions(usuarios);
    }

    @Test @DisplayName("TC-U14 · crear usuario con token válido llama al servicio con el rol elegido")
    void crear_con_csrf() throws Exception {
        mvc.perform(post("/panel/usuarios").sessionAttr("usuario", ADMIN).sessionAttr("csrf", "tok").param("_csrf", "tok")
                        .param("nombre", "Rosa Chávez").param("usuario", "rchavez").param("clave", "Inicio2026").param("rol", "operador"))
                .andExpect(redirectedUrl("/panel/usuarios")).andExpect(flash().attributeExists("ok"));
        verify(usuarios).crear(eq(ADMIN), eq("Rosa Chávez"), eq("rchavez"), eq("Inicio2026"), eq(RolUsuario.OPERADOR));
    }

    @Test @DisplayName("TC-U15 · una cuenta desactivada pierde la sesión abierta en su siguiente petición")
    void sesion_de_cuenta_desactivada_se_cierra() throws Exception {
        UsuarioMunicipal desactivado = new UsuarioMunicipal(2, "Operador", "operador", "h", RolUsuario.OPERADOR, false);
        when(dao.buscarPorId(2)).thenReturn(Optional.of(desactivado));
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuario", OPERADOR);
        mvc.perform(get("/panel/clave").session(s)).andExpect(redirectedUrl("/login?requerida"));
        org.junit.jupiter.api.Assertions.assertTrue(s.isInvalid());
    }
}
