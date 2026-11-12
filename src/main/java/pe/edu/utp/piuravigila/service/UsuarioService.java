package pe.edu.utp.piuravigila.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.model.RolUsuario;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;

/**
 * Gestión del personal municipal (v1.0): el administrador crea cuentas y las activa o desactiva; cada usuario cambia
 * su propia contraseña. Las reglas viven aquí, no en el Controller. Las contraseñas nunca se registran en logs.
 */
@Service
public class UsuarioService {
    private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);
    private static final Pattern FORMATO_USUARIO = Pattern.compile("^[a-z0-9._]{3,40}$");
    static final int MIN_CLAVE = 8;
    private final UsuarioMunicipalDAO dao;
    private final HashContrasena hash;

    public UsuarioService(UsuarioMunicipalDAO dao, HashContrasena hash) {
        this.dao = Objects.requireNonNull(dao);
        this.hash = Objects.requireNonNull(hash);
    }

    public List<UsuarioMunicipal> listar() { return dao.listar(); }

    /** Crea una cuenta activa. Devuelve su id. */
    public int crear(UsuarioMunicipal quien, String nombre, String usuario, String clave, RolUsuario rol) {
        exigirAdministrador(quien);
        String n = nombre == null ? "" : nombre.trim();
        String u = usuario == null ? "" : usuario.trim().toLowerCase();
        if (n.length() < 3 || n.length() > 100) throw new IllegalArgumentException("El nombre debe tener entre 3 y 100 caracteres");
        if (!FORMATO_USUARIO.matcher(u).matches())
            throw new IllegalArgumentException("El usuario debe tener de 3 a 40 caracteres: minúsculas, números, punto o guion bajo");
        if (rol == null) throw new IllegalArgumentException("Elige un rol");
        validarClave(clave);
        if (dao.buscarPorUsuario(u).isPresent()) throw new IllegalArgumentException("Ya existe un usuario con ese nombre de acceso");
        int id = dao.crear(n, u, hash.codificar(clave), rol);
        LOG.info("Usuario creado usuarioId={} rol={} por usuarioId={}", id, rol.valorBD(), quien.usuarioId());
        return id;
    }

    /** Activa o desactiva una cuenta. Nadie puede desactivarse a sí mismo (evita quedarse sin administrador). */
    public void cambiarActivo(UsuarioMunicipal quien, int usuarioId, boolean activo) {
        exigirAdministrador(quien);
        if (!activo && quien.usuarioId() == usuarioId) throw new IllegalStateException("No puedes desactivar tu propia cuenta");
        UsuarioMunicipal objetivo = dao.buscarPorId(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        dao.cambiarActivo(objetivo.usuarioId(), activo);
        LOG.info("Usuario {} usuarioId={} por usuarioId={}", activo ? "activado" : "desactivado", usuarioId, quien.usuarioId());
    }

    /** Cambia la contraseña del propio usuario: exige la actual y que la nueva se escriba dos veces igual. */
    public void cambiarClave(UsuarioMunicipal quien, String actual, String nueva, String confirmacion) {
        Objects.requireNonNull(quien);
        UsuarioMunicipal u = dao.buscarPorId(quien.usuarioId()).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (actual == null || actual.isEmpty() || !hash.coincide(actual, u.contrasenaHash()))
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        validarClave(nueva);
        if (!nueva.equals(confirmacion)) throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden");
        if (nueva.equals(actual)) throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual");
        dao.actualizarHash(u.usuario(), hash.codificar(nueva));
        LOG.info("Contraseña cambiada usuarioId={}", u.usuarioId());
    }

    /** El administrador asigna una contraseña nueva a otra cuenta (por ejemplo, si el usuario la olvidó). */
    public void restablecerClave(UsuarioMunicipal quien, int usuarioId, String nueva) {
        exigirAdministrador(quien);
        if (quien.usuarioId() == usuarioId) throw new IllegalStateException("Para tu propia cuenta usa «Mi contraseña»");
        UsuarioMunicipal objetivo = dao.buscarPorId(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        validarClave(nueva);
        dao.actualizarHash(objetivo.usuario(), hash.codificar(nueva));
        LOG.info("Contraseña restablecida usuarioId={} por usuarioId={}", usuarioId, quien.usuarioId());
    }

    static void validarClave(String clave) {
        if (clave == null || clave.length() < MIN_CLAVE || !clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*"))
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_CLAVE + " caracteres, con letras y números");
        if (clave.length() > 72) throw new IllegalArgumentException("La contraseña no puede superar los 72 caracteres");
    }

    private static void exigirAdministrador(UsuarioMunicipal quien) {
        if (quien == null || !quien.esAdministrador()) throw new IllegalStateException("Solo el administrador puede gestionar usuarios");
    }
}
