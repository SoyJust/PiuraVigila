package pe.edu.utp.piuravigila.dao.jdbc;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.edu.utp.piuravigila.dao.CategoriaDAO;
import pe.edu.utp.piuravigila.model.Categoria;

@Repository
public class CategoriaDAOJdbc implements CategoriaDAO {
    private static final RowMapper<Categoria> MAPEO = (rs, n) ->
            new Categoria(rs.getInt("categoria_id"), rs.getString("nombre"), rs.getBoolean("activo"));
    private final JdbcTemplate jdbc;
    public CategoriaDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<Categoria> listarActivas() {
        return jdbc.query("SELECT categoria_id, nombre, activo FROM categoria WHERE activo ORDER BY categoria_id", MAPEO);
    }
    @Override public Optional<Categoria> buscarPorId(int id) {
        return jdbc.query("SELECT categoria_id, nombre, activo FROM categoria WHERE categoria_id = ?", MAPEO, id).stream().findFirst();
    }
    @Override public void insertar(Categoria c) {
        jdbc.update("INSERT INTO categoria(nombre, activo) VALUES (?, ?)", c.nombre(), c.activo());
    }
    @Override public boolean existeNombre(String nombre) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM categoria WHERE LOWER(nombre) = LOWER(?))", Boolean.class, nombre));
    }
    @Override public double porcentajeOtraMes() {
        Double d = jdbc.queryForObject("SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE c.nombre = 'Otra') / NULLIF(COUNT(*), 0), 1) "
                + "FROM reporte r JOIN categoria c ON c.categoria_id = r.categoria_id "
                + "WHERE date_trunc('month', r.fecha_registro) = date_trunc('month', CURRENT_DATE)", Double.class);
        return d == null ? 0 : d;
    }
}
