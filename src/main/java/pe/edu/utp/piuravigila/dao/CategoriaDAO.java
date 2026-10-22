package pe.edu.utp.piuravigila.dao;
import java.util.*;
import pe.edu.utp.piuravigila.model.Categoria;
public interface CategoriaDAO {
    List<Categoria> listarActivas();
    Optional<Categoria> buscarPorId(int categoriaId);
    void insertar(Categoria c);
    boolean existeNombre(String nombre);
    /** % de reportes «Otra» sobre el total del mes calendario (RF-07). */
    double porcentajeOtraMes();
}
