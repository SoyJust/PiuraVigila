package pe.edu.utp.piuravigila.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.piuravigila.model.RolUsuario;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
import pe.edu.utp.piuravigila.service.UsuarioService;

/** v1.0 · P05 gestión de usuarios (solo administrador) y P06 cambio de la propia contraseña (ambos roles). */
@Controller
public class UsuarioController {
    private static final String SESION = "usuario";
    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping("/panel/usuarios")
    public String lista(HttpSession s, Model m, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!u.esAdministrador()) { ra.addFlashAttribute("error", "Solo el administrador puede gestionar usuarios"); return "redirect:/panel"; }
        m.addAttribute("usuario", u);
        m.addAttribute("csrf", Csrf.token(s));
        m.addAttribute("lista", usuarios.listar());
        m.addAttribute("roles", RolUsuario.values());
        return "usuarios";
    }

    @PostMapping("/panel/usuarios")
    public String crear(@RequestParam(defaultValue = "") String nombre, @RequestParam(defaultValue = "") String usuario,
                        @RequestParam(defaultValue = "") String clave, @RequestParam(defaultValue = "") String rol,
                        @RequestParam(name = "_csrf", required = false) String csrf, HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return "redirect:/panel/usuarios"; }
        try {
            RolUsuario r = rol.isBlank() ? null : RolUsuario.desdeBD(rol);
            usuarios.crear(u, nombre, usuario, clave, r);
            ra.addFlashAttribute("ok", "Usuario «" + usuario.trim().toLowerCase() + "» creado. Entrégale su contraseña inicial por un medio seguro.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("fNombre", nombre); ra.addFlashAttribute("fUsuario", usuario); ra.addFlashAttribute("fRol", rol);
        }
        return u.esAdministrador() ? "redirect:/panel/usuarios" : "redirect:/panel";
    }

    @PostMapping("/panel/usuarios/estado")
    public String cambiarActivo(@RequestParam int usuarioId, @RequestParam boolean activo,
                                @RequestParam(name = "_csrf", required = false) String csrf, HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return "redirect:/panel/usuarios"; }
        try {
            usuarios.cambiarActivo(u, usuarioId, activo);
            ra.addFlashAttribute("ok", activo ? "Usuario activado" : "Usuario desactivado: ya no puede iniciar sesión");
        } catch (IllegalArgumentException | IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return u.esAdministrador() ? "redirect:/panel/usuarios" : "redirect:/panel";
    }

    @PostMapping("/panel/usuarios/clave")
    public String restablecer(@RequestParam(defaultValue = "0") int usuarioId, @RequestParam(defaultValue = "") String nueva,
                              @RequestParam(name = "_csrf", required = false) String csrf, HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return "redirect:/panel/usuarios"; }
        try {
            usuarios.restablecerClave(u, usuarioId, nueva);
            ra.addFlashAttribute("ok", "Contraseña restablecida. Entrégala al usuario por un medio seguro.");
        } catch (IllegalArgumentException | IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return u.esAdministrador() ? "redirect:/panel/usuarios" : "redirect:/panel";
    }

    @GetMapping("/panel/clave")
    public String formClave(HttpSession s, Model m) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        m.addAttribute("usuario", u);
        m.addAttribute("csrf", Csrf.token(s));
        return "clave";
    }

    @PostMapping("/panel/clave")
    public String cambiarClave(@RequestParam(defaultValue = "") String actual, @RequestParam(defaultValue = "") String nueva,
                               @RequestParam(defaultValue = "") String confirmacion,
                               @RequestParam(name = "_csrf", required = false) String csrf, HttpSession s, RedirectAttributes ra) {
        UsuarioMunicipal u = (UsuarioMunicipal) s.getAttribute(SESION);
        if (u == null) return "redirect:/login?requerida";
        if (!Csrf.valido(s, csrf)) { ra.addFlashAttribute("error", "Solicitud no válida"); return "redirect:/panel/clave"; }
        try {
            usuarios.cambiarClave(u, actual, nueva, confirmacion);
            ra.addFlashAttribute("ok", "Contraseña actualizada");
            return "redirect:/panel";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/panel/clave";
        }
    }
}
