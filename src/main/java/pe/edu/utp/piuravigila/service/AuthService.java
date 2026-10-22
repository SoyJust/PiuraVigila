package pe.edu.utp.piuravigila.service;

import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;

/** RNF-01: autenticación básica del personal municipal. La contraseña nunca se registra en logs. */
@Service
public class AuthService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private final UsuarioMunicipalDAO usuarioDAO;
    private final HashContrasena hash;

    public AuthService(UsuarioMunicipalDAO usuarioDAO, HashContrasena hash) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO);
        this.hash = Objects.requireNonNull(hash);
    }

    public Optional<UsuarioMunicipal> autenticar(String usuario, String clave) {
        if (usuario == null || usuario.isBlank() || clave == null || clave.isEmpty()) return Optional.empty();
        Optional<UsuarioMunicipal> u = usuarioDAO.buscarPorUsuario(usuario.trim())
                .filter(x -> hash.coincide(clave, x.contrasenaHash()));
        if (u.isEmpty()) LOG.warn("Inicio de sesión fallido usuario={}", usuario.trim());
        else LOG.info("Inicio de sesión usuarioId={}", u.get().usuarioId());
        return u;
    }
}
