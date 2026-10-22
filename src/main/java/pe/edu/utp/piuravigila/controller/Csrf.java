package pe.edu.utp.piuravigila.controller;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/** Token CSRF mínimo para las acciones del panel (sin Spring Security completo). */
final class Csrf {
    private static final SecureRandom RND = new SecureRandom();
    private Csrf() {}
    static String token(HttpSession s) {
        Object t = s.getAttribute("csrf");
        if (t == null) {
            byte[] b = new byte[24]; RND.nextBytes(b);
            t = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            s.setAttribute("csrf", t);
        }
        return (String) t;
    }
    static boolean valido(HttpSession s, String recibido) {
        return recibido != null && recibido.equals(s.getAttribute("csrf"));
    }
}
