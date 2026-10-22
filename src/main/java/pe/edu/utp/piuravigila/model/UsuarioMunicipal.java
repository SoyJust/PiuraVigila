package pe.edu.utp.piuravigila.model;
public record UsuarioMunicipal(int usuarioId, String nombre, String usuario, String contrasenaHash, RolUsuario rol) {
    public boolean esAdministrador() { return rol == RolUsuario.ADMINISTRADOR; }
}
