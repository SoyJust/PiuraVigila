package pe.edu.utp.piuravigila.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Semana 14 · cabeceras HTTP de seguridad en todas las respuestas (hallazgos SEC-01 a SEC-03 de ZAP pasivo).
 *
 * <p>CSP: los scripts solo pueden venir del propio sitio o de cdnjs (Leaflet, con SRI); no se permiten scripts en línea.
 * Los estilos tampoco admiten 'unsafe-inline': las plantillas usan clases CSS y Leaflet aplica sus estilos por CSSOM,
 * que la CSP no bloquea. Las imágenes del mapa vienen de los mosaicos de OpenStreetMap y de cdnjs.</p>
 */
@Component
public class CabecerasSeguridad extends OncePerRequestFilter {
    static final String CSP = "default-src 'self'; "
            + "script-src 'self' https://cdnjs.cloudflare.com; "
            + "style-src 'self' https://cdnjs.cloudflare.com https://fonts.googleapis.com; "
            + "img-src 'self' data: blob: https://tile.openstreetmap.org https://cdnjs.cloudflare.com; "
            + "connect-src 'self'; font-src 'self' https://fonts.gstatic.com; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest rq, HttpServletResponse rs, FilterChain cadena) throws ServletException, IOException {
        rs.setHeader("Content-Security-Policy", CSP);
        rs.setHeader("X-Frame-Options", "DENY");
        rs.setHeader("X-Content-Type-Options", "nosniff");
        rs.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        rs.setHeader("Permissions-Policy", "geolocation=(self), camera=(), microphone=()");
        cadena.doFilter(rq, rs);
    }
}
