package pe.edu.utp.piuravigila.dao;
import java.util.*;
import pe.edu.utp.piuravigila.model.Zona;
public interface ZonaDAO {
    List<Zona> listar();
    Optional<Zona> buscarPorId(int zonaId);
}
