package pe.edu.utp.piuravigila.service;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.utp.piuravigila.dao.ReporteDAO;
import pe.edu.utp.piuravigila.dto.ResultadoControl;

/** Anti-spam (RF-06): verificación simple, 60 s entre reportes y 3 por día por dispositivo. */
@Component
public class ControlAntiSpam {
    public static final String MSG_SUMA = "Verificación incorrecta";
    public static final String MSG_ESPERA = "Espera antes de enviar otro";
    public static final String MSG_LIMITE = "Límite diario alcanzado";

    private final ReporteDAO reporteDAO;
    private final Clock reloj;
    private final int intervaloSeg;
    private final int maxPorDia;

    public ControlAntiSpam(ReporteDAO reporteDAO, Clock reloj,
                           @Value("${piuravigila.antispam.intervalo-seg:60}") int intervaloSeg,
                           @Value("${piuravigila.antispam.max-por-dia:3}") int maxPorDia) {
        this.reporteDAO = reporteDAO;
        this.reloj = reloj;
        this.intervaloSeg = intervaloSeg;
        this.maxPorDia = maxPorDia;
    }

    public ResultadoControl validar(String dispositivoId, Integer respuestaSuma, Integer sumaEsperada) {
        ResultadoControl suma = validarSuma(respuestaSuma, sumaEsperada);
        return suma.permitido() ? validarLimites(dispositivoId) : suma;
    }

    public ResultadoControl validarSuma(Integer respuesta, Integer esperada) {
        if (respuesta == null || esperada == null || !respuesta.equals(esperada)) {
            return new ResultadoControl(false, MSG_SUMA, 0, true);
        }
        return ResultadoControl.ok();
    }

    /** Intervalo mínimo y tope diario; los reportes vinculados también cuentan. */
    public ResultadoControl validarLimites(String dispositivoId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        var ultimo = reporteDAO.ultimoEnvio(dispositivoId);
        if (ultimo.isPresent()) {
            long transcurridos = Duration.between(ultimo.get(), ahora).getSeconds();
            if (transcurridos < intervaloSeg) {
                return new ResultadoControl(false, MSG_ESPERA, intervaloSeg - transcurridos, false);
            }
        }
        if (reporteDAO.contarEnDia(dispositivoId, ahora.toLocalDate()) >= maxPorDia) {
            return ResultadoControl.rechazo(MSG_LIMITE);
        }
        return ResultadoControl.ok();
    }
}
