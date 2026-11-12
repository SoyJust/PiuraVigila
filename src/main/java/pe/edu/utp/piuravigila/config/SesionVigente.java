package pe.edu.utp.piuravigila.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;

/**
 * v1.0 · en cada petición al panel se vuelve a leer al usuario de la sesión: si el administrador lo desactivó, la sesión
 * se cierra de inmediato (no espera a que caduque); si cambió su nombre o rol, la sesión toma el dato vigente.
 */
@Component
public class SesionVigente extends OncePerRequestFilter {
    private final UsuarioMunicipalDAO usuarios;

    public SesionVigente(UsuarioMunicipalDAO usuarios) { this.usuarios = usuarios; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest rq) {
        return !rq.getRequestURI().startsWith(rq.getContextPath() + "/panel");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest rq, HttpServletResponse rs, FilterChain cadena) throws ServletException, IOException {
        HttpSession s = rq.getSession(false);
        if (s != null && s.getAttribute("usuario") instanceof UsuarioMunicipal u) {
            UsuarioMunicipal vigente = usuarios.buscarPorId(u.usuarioId()).filter(UsuarioMunicipal::activo).orElse(null);
            if (vigente == null) {
                s.invalidate();
                rs.sendRedirect(rq.getContextPath() + "/login?requerida");
                return;
            }
            if (!vigente.equals(u)) s.setAttribute("usuario", vigente);
        }
        cadena.doFilter(rq, rs);
    }
}
