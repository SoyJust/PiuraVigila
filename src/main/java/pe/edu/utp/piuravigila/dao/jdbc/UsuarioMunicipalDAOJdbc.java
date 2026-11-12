package pe.edu.utp.piuravigila.dao.jdbc;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.model.*;

@Repository
public class UsuarioMunicipalDAOJdbc implements UsuarioMunicipalDAO {
    private static final String COLUMNAS = "SELECT usuario_id, nombre, usuario, contrasena_hash, rol, activo FROM usuario_municipal";
    private static final RowMapper<UsuarioMunicipal> MAPEO = (rs, n) -> new UsuarioMunicipal(rs.getInt("usuario_id"),
            rs.getString("nombre"), rs.getString("usuario"), rs.getString("contrasena_hash"),
            RolUsuario.desdeBD(rs.getString("rol")), rs.getBoolean("activo"));
    private final JdbcTemplate jdbc;
    public UsuarioMunicipalDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<UsuarioMunicipal> buscarPorUsuario(String usuario) {
        return jdbc.query(COLUMNAS + " WHERE usuario = ?", MAPEO, usuario).stream().findFirst();
    }
    @Override public Optional<UsuarioMunicipal> buscarPorId(int usuarioId) {
        return jdbc.query(COLUMNAS + " WHERE usuario_id = ?", MAPEO, usuarioId).stream().findFirst();
    }
    @Override public List<UsuarioMunicipal> listar() {
        return jdbc.query(COLUMNAS + " ORDER BY activo DESC, rol, nombre", MAPEO);
    }
    @Override public int crear(String nombre, String usuario, String hash, RolUsuario rol) {
        return jdbc.queryForObject("INSERT INTO usuario_municipal (nombre, usuario, contrasena_hash, rol) VALUES (?, ?, ?, ?) RETURNING usuario_id",
                Integer.class, nombre, usuario, hash, rol.valorBD());
    }
    @Override public void cambiarActivo(int usuarioId, boolean activo) {
        jdbc.update("UPDATE usuario_municipal SET activo = ? WHERE usuario_id = ?", activo, usuarioId);
    }
    @Override public void actualizarHash(String usuario, String hash) {
        jdbc.update("UPDATE usuario_municipal SET contrasena_hash = ? WHERE usuario = ?", hash, usuario);
    }
}
