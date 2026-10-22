package pe.edu.utp.piuravigila.service;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.piuravigila.dao.CategoriaDAO;
import pe.edu.utp.piuravigila.model.Categoria;

/** RF-07: habilitar una categoría nueva cuando «Otra» supera el umbral del mes. */
@Service
public class CategoriaService {
    private static final Logger LOG = LoggerFactory.getLogger(CategoriaService.class);
    private final CategoriaDAO categoriaDAO;
    private final double umbralOtra;

    public CategoriaService(CategoriaDAO categoriaDAO, @Value("${piuravigila.dashboard.umbral-otra:10}") double umbralOtra) {
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO);
        this.umbralOtra = umbralOtra;
    }

    @Transactional
    public void habilitar(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || limpio.length() > 60) throw new IllegalArgumentException("El nombre de la categoría es obligatorio (máx. 60).");
        if (categoriaDAO.existeNombre(limpio)) throw new IllegalStateException("Categoría ya existe");
        categoriaDAO.insertar(new Categoria(0, limpio, true));
        LOG.info("Categoría habilitada nombre={}", limpio);
    }

    public boolean umbralOtraSuperado() { return categoriaDAO.porcentajeOtraMes() > umbralOtra; }
    public double porcentajeOtraMes() { return categoriaDAO.porcentajeOtraMes(); }
}
