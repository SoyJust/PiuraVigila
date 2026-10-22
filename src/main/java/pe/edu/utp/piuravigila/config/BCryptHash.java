package pe.edu.utp.piuravigila.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.utp.piuravigila.service.HashContrasena;

/** Librería con propósito observable (RNF-01): contraseñas del personal solo como hash BCrypt. */
@Component
public class BCryptHash implements HashContrasena {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Override public String codificar(String claveEnClaro) { return encoder.encode(claveEnClaro); }
    @Override public boolean coincide(String claveEnClaro, String hash) {
        return hash != null && hash.startsWith("$2") && encoder.matches(claveEnClaro, hash);
    }
}
