package pe.edu.utp.piuravigila.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Semana 14 · SEC-01 a SEC-03: cabeceras HTTP de seguridad. (Esqueleto: todavía no agrega cabeceras.) */
@Component
public class CabecerasSeguridad extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest rq, HttpServletResponse rs, FilterChain cadena) throws ServletException, IOException {
        cadena.doFilter(rq, rs);
    }
}
