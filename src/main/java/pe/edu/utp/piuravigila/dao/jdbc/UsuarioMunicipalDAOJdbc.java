package pe.edu.utp.piuravigila.dao.jdbc;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.model.*;

@Repository
public class UsuarioMunicipalDAOJdbc implements UsuarioMunicipalDAO {
    private final JdbcTemplate jdbc;
    public UsuarioMunicipalDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<UsuarioMunicipal> buscarPorUsuario(String usuario) {
        return jdbc.query("SELECT usuario_id, nombre, usuario, contrasena_hash, rol FROM usuario_municipal WHERE usuario = ?",
                (rs, n) -> new UsuarioMunicipal(rs.getInt("usuario_id"), rs.getString("nombre"), rs.getString("usuario"),
                        rs.getString("contrasena_hash"), RolUsuario.desdeBD(rs.getString("rol"))), usuario).stream().findFirst();
    }
    @Override public void actualizarHash(String usuario, String hash) {
        jdbc.update("UPDATE usuario_municipal SET contrasena_hash = ? WHERE usuario = ?", hash, usuario);
    }
}
