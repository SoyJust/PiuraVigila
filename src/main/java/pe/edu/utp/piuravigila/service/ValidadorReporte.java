package pe.edu.utp.piuravigila.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.utp.piuravigila.dto.DatosReporte;
import pe.edu.utp.piuravigila.dto.ResultadoControl;

/** Valida el formulario P01 en el servidor (RF-01): campos obligatorios y foto de máx. 2 MB. */
@Component
public class ValidadorReporte {
    public static final String MSG_FALTAN = "Faltan campos obligatorios";
    public static final String MSG_FOTO = "Foto demasiado pesada";
    public static final int MAX_DESCRIPCION = 300; // reporte.descripcion VARCHAR(300)
    public static final String MSG_FUERA = "La ubicación marcada está fuera de la provincia de Piura. Toca el mapa en el lugar del problema.";
    /** v1.1 · rectángulo holgado que contiene los 10 distritos de la provincia de Piura (de Las Lomas a La Unión). */
    static final double LAT_MIN = -5.70, LAT_MAX = -4.30, LNG_MIN = -81.00, LNG_MAX = -79.80;

    private final long maxFotoBytes;

    public ValidadorReporte(@Value("${piuravigila.foto.max-bytes:2097152}") long maxFotoBytes) {
        this.maxFotoBytes = maxFotoBytes;
    }

    public ResultadoControl validar(DatosReporte d) {
        if (d == null || d.categoriaId() <= 0 || d.zonaId() <= 0
                || org.apache.commons.lang3.StringUtils.isBlank(d.descripcion())
                || d.descripcion().length() > MAX_DESCRIPCION
                || d.fotoBytes() == null || d.fotoBytes().length == 0
                || d.dispositivoId() == null || d.dispositivoId().isBlank()) {
            return ResultadoControl.rechazo(MSG_FALTAN);
        }
        if ((d.lat() == null) != (d.lng() == null)
                || (d.lat() != null && (!Double.isFinite(d.lat()) || !Double.isFinite(d.lng()) || Math.abs(d.lat()) > 90 || Math.abs(d.lng()) > 180))) {
            return ResultadoControl.rechazo(MSG_FALTAN);
        }
        if (d.lat() != null && (d.lat() < LAT_MIN || d.lat() > LAT_MAX || d.lng() < LNG_MIN || d.lng() > LNG_MAX))
            return ResultadoControl.rechazo(MSG_FUERA);
        if (d.fotoBytes().length > maxFotoBytes) return ResultadoControl.rechazo(MSG_FOTO);
        return ResultadoControl.ok();
    }
}
