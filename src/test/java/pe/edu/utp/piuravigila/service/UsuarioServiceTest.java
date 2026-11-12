package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.model.RolUsuario;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
import pe.edu.utp.piuravigila.service.Fakes.FakeUsuarioDAO;

/** v1.0 · gestión de usuarios (TC-U01 a TC-U10): reglas del UsuarioService con DAO y hash simulados. */
class UsuarioServiceTest {
    private static final HashContrasena HASH = new HashContrasena() {
        public String codificar(String c) { return "h:" + c; }
        public boolean coincide(String c, String h) { return h.equals("h:" + c); }
    };
    private FakeUsuarioDAO dao;
    private UsuarioService svc;
    private UsuarioMunicipal admin, operador;

    @BeforeEach
    void montar() {
        dao = new FakeUsuarioDAO();
        admin = new UsuarioMunicipal(1, "Administrador", "admin", "h:Clave1234", RolUsuario.ADMINISTRADOR);
        operador = new UsuarioMunicipal(2, "Operador", "operador", "h:Clave5678", RolUsuario.OPERADOR);
        dao.m.put("admin", admin); dao.m.put("operador", operador);
        svc = new UsuarioService(dao, HASH);
    }

    @Test @DisplayName("TC-U01 · el administrador crea un usuario activo con la contraseña cifrada y el usuario en minúsculas")
    void crea_usuario() {
        int id = svc.crear(admin, "  Rosa Chávez ", " RChavez ", "Inicio2026", RolUsuario.OPERADOR);
        UsuarioMunicipal u = dao.buscarPorId(id).orElseThrow();
        assertEquals("rchavez", u.usuario());
        assertEquals("Rosa Chávez", u.nombre());
        assertEquals("h:Inicio2026", u.contrasenaHash());
        assertTrue(u.activo());
    }

    @Test @DisplayName("TC-U02 · el operador no puede crear usuarios")
    void operador_no_crea() {
        assertThrows(IllegalStateException.class, () -> svc.crear(operador, "Rosa Chávez", "rchavez", "Inicio2026", RolUsuario.OPERADOR));
        assertTrue(dao.buscarPorUsuario("rchavez").isEmpty());
    }

    @Test @DisplayName("TC-U03 · no se repite el usuario ni se aceptan formatos inválidos")
    void validaciones_de_creacion() {
        assertThrows(IllegalArgumentException.class, () -> svc.crear(admin, "Otro admin", "admin", "Inicio2026", RolUsuario.OPERADOR));
        assertThrows(IllegalArgumentException.class, () -> svc.crear(admin, "Rosa", "r ch", "Inicio2026", RolUsuario.OPERADOR));
        assertThrows(IllegalArgumentException.class, () -> svc.crear(admin, "Ro", "rchavez", "Inicio2026", RolUsuario.OPERADOR));
        assertThrows(IllegalArgumentException.class, () -> svc.crear(admin, "Rosa Chávez", "rchavez", "Inicio2026", null));
    }

    @Test @DisplayName("TC-U04 · la contraseña exige 8 caracteres con letras y números (valores límite)")
    void politica_de_contrasena() {
        assertThrows(IllegalArgumentException.class, () -> UsuarioService.validarClave("abc1234"));   // 7 caracteres
        assertDoesNotThrow(() -> UsuarioService.validarClave("abcd1234"));                            // 8 caracteres
        assertThrows(IllegalArgumentException.class, () -> UsuarioService.validarClave("abcdefgh"));   // sin números
        assertThrows(IllegalArgumentException.class, () -> UsuarioService.validarClave("12345678"));   // sin letras
        assertThrows(IllegalArgumentException.class, () -> UsuarioService.validarClave("a1".repeat(37))); // 74 > 72 (límite de BCrypt)
    }

    @Test @DisplayName("TC-U05 · desactivar impide iniciar sesión; volver a activar lo permite")
    void desactivar_y_activar() {
        AuthService auth = new AuthService(dao, HASH);
        svc.cambiarActivo(admin, 2, false);
        assertTrue(auth.autenticar("operador", "Clave5678").isEmpty());
        svc.cambiarActivo(admin, 2, true);
        assertTrue(auth.autenticar("operador", "Clave5678").isPresent());
    }

    @Test @DisplayName("TC-U06 · nadie puede desactivar su propia cuenta")
    void no_se_desactiva_a_si_mismo() {
        assertThrows(IllegalStateException.class, () -> svc.cambiarActivo(admin, 1, false));
        assertTrue(dao.buscarPorId(1).orElseThrow().activo());
    }

    @Test @DisplayName("TC-U07 · cambiar la propia contraseña exige la actual y la confirmación")
    void cambia_propia_contrasena() {
        assertThrows(IllegalArgumentException.class, () -> svc.cambiarClave(operador, "incorrecta1", "Nueva2026", "Nueva2026"));
        assertThrows(IllegalArgumentException.class, () -> svc.cambiarClave(operador, "Clave5678", "Nueva2026", "Otra2026"));
        assertThrows(IllegalArgumentException.class, () -> svc.cambiarClave(operador, "Clave5678", "Clave5678", "Clave5678"));
        svc.cambiarClave(operador, "Clave5678", "Nueva2026", "Nueva2026");
        assertEquals("h:Nueva2026", dao.buscarPorUsuario("operador").orElseThrow().contrasenaHash());
    }

    @Test @DisplayName("TC-U08 · el administrador restablece la contraseña de otro usuario, no la suya")
    void restablece_contrasena() {
        svc.restablecerClave(admin, 2, "Temporal99");
        assertEquals("h:Temporal99", dao.buscarPorUsuario("operador").orElseThrow().contrasenaHash());
        assertThrows(IllegalStateException.class, () -> svc.restablecerClave(admin, 1, "Temporal99"));
        assertThrows(IllegalStateException.class, () -> svc.restablecerClave(operador, 1, "Temporal99"));
    }

    @Test @DisplayName("TC-U09 · el operador no puede activar ni desactivar cuentas")
    void operador_no_cambia_estado() {
        assertThrows(IllegalStateException.class, () -> svc.cambiarActivo(operador, 1, false));
    }

    @Test @DisplayName("TC-U10 · usuario inexistente se rechaza con un mensaje claro")
    void usuario_inexistente() {
        assertThrows(IllegalArgumentException.class, () -> svc.cambiarActivo(admin, 999, false));
        assertThrows(IllegalArgumentException.class, () -> svc.restablecerClave(admin, 999, "Temporal99"));
    }
}
