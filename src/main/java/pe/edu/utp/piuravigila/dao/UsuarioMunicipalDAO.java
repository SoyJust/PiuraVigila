package pe.edu.utp.piuravigila.dao;
import java.util.Optional;
import pe.edu.utp.piuravigila.model.UsuarioMunicipal;
public interface UsuarioMunicipalDAO {
    Optional<UsuarioMunicipal> buscarPorUsuario(String usuario);
    void actualizarHash(String usuario, String hash);
}
