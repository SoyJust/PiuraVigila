package pe.edu.utp.piuravigila.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Errores técnicos: mensaje genérico al usuario; el detalle va al log, no a la pantalla. */
@ControllerAdvice
public class ManejoErrores {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ManejoErrores.class);

    @ExceptionHandler({org.springframework.dao.DataAccessException.class, java.io.UncheckedIOException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String error(RuntimeException e, Model m) {
        LOG.error("Error no controlado", e);
        m.addAttribute("mensaje", "Ocurrió un error. Intenta de nuevo en unos minutos.");
        return "error";
    }
}
