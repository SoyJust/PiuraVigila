package pe.edu.utp.piuravigila.dao.jdbc;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.edu.utp.piuravigila.dao.ZonaDAO;
import pe.edu.utp.piuravigila.model.Zona;

@Repository
public class ZonaDAOJdbc implements ZonaDAO {
    private static final RowMapper<Zona> MAPEO = (rs, n) -> new Zona(rs.getInt("zona_id"), rs.getString("nombre"), rs.getString("sector"));
    private final JdbcTemplate jdbc;
    public ZonaDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<Zona> listar() { return jdbc.query("SELECT zona_id, nombre, sector FROM zona ORDER BY zona_id", MAPEO); }
    @Override public Optional<Zona> buscarPorId(int id) {
        return jdbc.query("SELECT zona_id, nombre, sector FROM zona WHERE zona_id = ?", MAPEO, id).stream().findFirst();
    }
}
