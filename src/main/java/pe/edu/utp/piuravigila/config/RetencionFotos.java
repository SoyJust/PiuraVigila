package pe.edu.utp.piuravigila.config;

import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import pe.edu.utp.piuravigila.dao.ReporteDAO;
import pe.edu.utp.piuravigila.service.FotoStorage;

/**
 * v1.1 · política de retención: la foto es evidencia para atender el caso; cuando el caso lleva más de N días atendido,
 * su foto (y las de sus vinculados) se borra del disco. El registro del caso, su historial y la ruta quedan; el panel
 * muestra «Foto eliminada por antigüedad». Con {@code piuravigila.foto.retencion-dias=0} no se borra nada.
 */
@Configuration
@EnableScheduling
public class RetencionFotos {
    private static final Logger LOG = LoggerFactory.getLogger(RetencionFotos.class);
    private final ReporteDAO reportes;
    private final FotoStorage fotos;
    private final Clock reloj;
    private final int dias;

    public RetencionFotos(ReporteDAO reportes, FotoStorage fotos, Clock reloj,
                          @Value("${piuravigila.foto.retencion-dias:365}") int dias) {
        this.reportes = reportes; this.fotos = fotos; this.reloj = reloj; this.dias = dias;
    }

    /** Todos los días a las 3:15 a. m. (hora del servidor). Devuelve cuántos archivos borró. */
    @Scheduled(cron = "${piuravigila.foto.retencion-cron:0 15 3 * * *}")
    public int limpiar() {
        if (dias <= 0) return 0;
        int borradas = 0;
        for (String ruta : reportes.fotosDeCasosAtendidosAntesDe(LocalDateTime.now(reloj).minusDays(dias))) {
            if (fotos.eliminar(ruta)) borradas++;
        }
        if (borradas > 0) LOG.info("Retención de fotos: {} archivos borrados (casos atendidos hace más de {} días)", borradas, dias);
        return borradas;
    }
}
