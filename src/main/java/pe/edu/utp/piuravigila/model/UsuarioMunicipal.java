package pe.edu.utp.piuravigila.model;

/** Personal municipal. {@code activo = false} impide iniciar sesión sin borrar el historial que firmó (gestión de usuarios). */
public record UsuarioMunicipal(int usuarioId, String nombre, String usuario, String contrasenaHash, RolUsuario rol, boolean activo) {
    public UsuarioMunicipal(int usuarioId, String nombre, String usuario, String contrasenaHash, RolUsuario rol) {
        this(usuarioId, nombre, usuario, contrasenaHash, rol, true);
    }
    public boolean esAdministrador() { return rol == RolUsuario.ADMINISTRADOR; }
}
