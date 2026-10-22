package pe.edu.utp.piuravigila.service;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.utp.piuravigila.dao.*;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.EstadoReporte;

/** Reportes R01 a R03. Solo conteos, promedios y porcentajes: nunca fotos. */
@Service
public class DashboardService {
    private final ReporteDAO reporteDAO;
    private final HistorialEstadoDAO historialDAO;
    private final CategoriaDAO categoriaDAO;
    private final double umbralOtra;

    public DashboardService(ReporteDAO reporteDAO, HistorialEstadoDAO historialDAO, CategoriaDAO categoriaDAO,
                            @Value("${piuravigila.dashboard.umbral-otra:10}") double umbralOtra) {
        this.reporteDAO = Objects.requireNonNull(reporteDAO);
        this.historialDAO = Objects.requireNonNull(historialDAO);
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO);
        this.umbralOtra = umbralOtra;
    }

    public List<ConteoZona> zonasCriticas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return reporteDAO.reportesPorZona(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    public List<TiempoCategoria> tiemposAtencion(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return historialDAO.tiempoPromedio(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    public ResumenGestion resumenGestion(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime d = desde.atStartOfDay(), h = hasta.plusDays(1).atStartOfDay();
        Map<EstadoReporte, Integer> porEstado = new EnumMap<>(EstadoReporte.class);
        for (EstadoReporte e : EstadoReporte.values()) porEstado.put(e, 0);
        porEstado.putAll(reporteDAO.casosPorEstado(d, h));
        double pctOtra = categoriaDAO.porcentajeOtraMes();
        return new ResumenGestion(porEstado, reporteDAO.contarVinculadosPeriodo(d, h),
                reporteDAO.porcentajeActualizados(d, h), pctOtra, umbralOtra, pctOtra > umbralOtra);
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) throw new IllegalArgumentException("Periodo inválido.");
    }
}
