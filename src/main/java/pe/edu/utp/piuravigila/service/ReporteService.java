package pe.edu.utp.piuravigila.service;

import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.piuravigila.dao.*;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;

/**
 * Caso de uso central (RF-01, RF-02, RF-03, RF-05, RN-01).
 * Reglas de negocio aquí; nada de SQL (DAO) ni de HTML (vistas). Clase no final: @Transactional necesita proxy.
 */
@Service
public class ReporteService {
    private static final Logger LOG = LoggerFactory.getLogger(ReporteService.class);
    private static final Pattern FORMATO_CODIGO = Pattern.compile("^PR-[0-9]{4}-[0-9]{4}$");
    public static final int TAMANO_PAGINA = 20;

    private final ReporteDAO reporteDAO;
    private final HistorialEstadoDAO historialDAO;
    private final CategoriaDAO categoriaDAO;
    private final ZonaDAO zonaDAO;
    private final ValidadorReporte validador;
    private final ControlAntiSpam antiSpam;
    private final FotoStorage fotos;
    private final Clock reloj;

    public ReporteService(ReporteDAO reporteDAO, HistorialEstadoDAO historialDAO, CategoriaDAO categoriaDAO,
                          ZonaDAO zonaDAO, ValidadorReporte validador, ControlAntiSpam antiSpam,
                          FotoStorage fotos, Clock reloj) {
        this.reporteDAO = Objects.requireNonNull(reporteDAO);
        this.historialDAO = Objects.requireNonNull(historialDAO);
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO);
        this.zonaDAO = Objects.requireNonNull(zonaDAO);
        this.validador = Objects.requireNonNull(validador);
        this.antiSpam = Objects.requireNonNull(antiSpam);
        this.fotos = Objects.requireNonNull(fotos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** P01 · RF-01, RF-05, RF-06. Orden: campos/foto → verificación/60 s/3 por día → categoría y zona → duplicado. */
    @Transactional
    public ResultadoRegistro registrarReporte(DatosReporte d) {
        ResultadoControl v = validador.validar(d);
        if (!v.permitido()) return ResultadoRegistro.rechazado(v.mensaje(), false);

        ResultadoControl c = antiSpam.validar(d.dispositivoId(), d.respuestaSuma(), d.sumaEsperada());
        if (!c.permitido()) {
            String msg = c.segundosFaltan() > 0 ? c.mensaje() + ": faltan " + c.segundosFaltan() + " s" : c.mensaje();
            return ResultadoRegistro.rechazado(msg, c.regenerarSuma());
        }
        if (categoriaDAO.buscarPorId(d.categoriaId()).filter(Categoria::estaActiva).isEmpty()
                || zonaDAO.buscarPorId(d.zonaId()).isEmpty()) {
            return ResultadoRegistro.rechazado(ValidadorReporte.MSG_FALTAN, false);
        }
        Optional<Reporte> caso = reporteDAO.buscarCasoAbierto(d.categoriaId(), d.zonaId());
        if (caso.isPresent()) return ResultadoRegistro.duplicado(caso.get()); // decide el ciudadano; no se comparan fotos
        return crearCaso(d);
    }

    /** RN-01: «Sí, es el mismo» vincula al caso (sin código propio); «No, es otro» crea un caso nuevo. */
    @Transactional
    public ResultadoRegistro responderDuplicado(DatosReporte pendiente, boolean esMismo, int casoId) {
        ResultadoControl lim = antiSpam.validarLimites(pendiente.dispositivoId());
        if (!lim.permitido()) {
            String msg = lim.segundosFaltan() > 0 ? lim.mensaje() + ": faltan " + lim.segundosFaltan() + " s" : lim.mensaje();
            return ResultadoRegistro.rechazado(msg, false);
        }
        if (!esMismo) return crearCaso(pendiente);

        Reporte caso = reporteDAO.buscarPorId(casoId).orElseThrow(() -> new IllegalArgumentException("Caso inexistente."));
        if (caso.categoriaId() != pendiente.categoriaId() || caso.zonaId() != pendiente.zonaId()) {
            throw new IllegalArgumentException("El caso no coincide con la categoría y zona del reporte.");
        }
        if (!caso.esCasoOriginal() || !caso.estaAbierto()) {
            return ResultadoRegistro.rechazado("El caso ya no admite vinculaciones; registra un reporte nuevo.", false);
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String ruta = fotos.guardar(pendiente.fotoNombre(), pendiente.fotoBytes());
        reporteDAO.insertarVinculado(nuevoReporte(pendiente, ruta, ahora, caso.reporteId()), caso.reporteId());
        LOG.info("Reporte vinculado al caso codigo={}", caso.codigoSeguimiento());
        return ResultadoRegistro.vinculado(caso.codigoSeguimiento());
    }

    /** P02 · RF-02. Quien se vinculó consulta con el código del caso. */
    public ConsultaEstado consultarEstado(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        if (!FORMATO_CODIGO.matcher(limpio).matches()) throw new IllegalArgumentException("Código no encontrado");
        Reporte caso = reporteDAO.buscarPorCodigo(limpio).orElseThrow(() -> new IllegalArgumentException("Código no encontrado"));
        List<HistorialEstado> historial = historialDAO.listarPorReporte(caso.reporteId());
        LocalDateTime ultima = historial.stream().map(HistorialEstado::fechaCambio).max(Comparator.naturalOrder()).orElse(caso.fechaRegistro());
        return new ConsultaEstado(caso, ultima, pasos(caso, historial));
    }

    /** Los tres estados en orden. Solo se expone el estado y la fecha: el responsable del cambio no se muestra al ciudadano. */
    private static List<PasoEstado> pasos(Reporte caso, List<HistorialEstado> historial) {
        List<PasoEstado> pasos = new ArrayList<>();
        for (EstadoReporte e : EstadoReporte.values()) {
            LocalDateTime fecha = historial.stream().filter(h -> h.estadoNuevo() == e)
                    .map(HistorialEstado::fechaCambio).min(Comparator.naturalOrder()).orElse(null);
            if (fecha == null && e == EstadoReporte.RECIBIDO) fecha = caso.fechaRegistro(); // casos sin historial
            pasos.add(new PasoEstado(e, fecha, e == caso.estado()));
        }
        return pasos;
    }

    /** P03 · RF-03. El estado solo avanza y queda registrado con fecha y responsable. */
    @Transactional
    public void cambiarEstado(int reporteId, EstadoReporte nuevo, UsuarioMunicipal usuario) {
        Objects.requireNonNull(usuario, "Sesión requerida");
        Reporte r = reporteDAO.buscarPorId(reporteId).orElseThrow(() -> new IllegalArgumentException("Reporte inexistente."));
        if (!r.esCasoOriginal()) throw new IllegalStateException("Solo los casos cambian de estado; el vinculado usa el del caso.");
        if (!r.estado().puedePasarA(nuevo)) throw new IllegalStateException("El estado solo avanza (Recibido → En proceso → Atendido).");
        reporteDAO.actualizarEstado(reporteId, nuevo);
        historialDAO.registrar(new HistorialEstado(0, reporteId, usuario.usuarioId(), nuevo, LocalDateTime.now(reloj)));
        LOG.info("Estado actualizado reporte={} nuevo={} usuarioId={}", reporteId, nuevo.etiqueta(), usuario.usuarioId());
    }

    public List<FilaPanel> listarPanel(Integer zonaId, Integer categoriaId, EstadoReporte estado, int pagina) {
        List<FilaPanel> filas = new ArrayList<>();
        for (Reporte r : reporteDAO.listarCasos(zonaId, categoriaId, estado, Math.max(pagina, 0), TAMANO_PAGINA)) {
            filas.add(new FilaPanel(r, reporteDAO.contarVinculados(r.reporteId())));
        }
        return filas;
    }

    public int totalCasos(Integer zonaId, Integer categoriaId, EstadoReporte estado) {
        return reporteDAO.contarCasos(zonaId, categoriaId, estado);
    }

    public List<Categoria> categoriasActivas() { return categoriaDAO.listarActivas(); }
    public List<Zona> zonas() { return zonaDAO.listar(); }

    // ---- internos ----
    private ResultadoRegistro crearCaso(DatosReporte d) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String ruta = fotos.guardar(d.fotoNombre(), d.fotoBytes());
        String codigo = reporteDAO.insertarCaso(nuevoReporte(d, ruta, ahora, null));
        Reporte guardado = reporteDAO.buscarPorCodigo(codigo).orElseThrow(() -> new IllegalStateException("Caso no encontrado tras insertar."));
        historialDAO.registrar(new HistorialEstado(0, guardado.reporteId(), null, EstadoReporte.RECIBIDO, ahora));
        LOG.info("Caso registrado codigo={} categoria={} zona={}", codigo, d.categoriaId(), d.zonaId());
        return ResultadoRegistro.registrado(codigo);
    }

    private static Reporte nuevoReporte(DatosReporte d, String fotoUrl, LocalDateTime ahora, Integer originalId) {
        return new Reporte(0, null, originalId, d.categoriaId(), d.zonaId(), d.descripcion().trim(), fotoUrl,
                d.lat(), d.lng(), EstadoReporte.RECIBIDO, d.dispositivoId(), ahora);
    }
}
