package pe.edu.utp.piuravigila.dao;
import java.util.List;
import java.util.Optional;
import pe.edu.utp.piuravigila.model.RolUsuario;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
public interface UsuarioMunicipalDAO {
    Optional<UsuarioMunicipal> buscarPorUsuario(String usuario);
    Optional<UsuarioMunicipal> buscarPorId(int usuarioId);
    List<UsuarioMunicipal> listar();
    int crear(String nombre, String usuario, String hash, RolUsuario rol);
    void cambiarActivo(int usuarioId, boolean activo);
    void actualizarHash(String usuario, String hash);
}
