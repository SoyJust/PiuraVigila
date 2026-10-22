package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.dto.ResultadoControl;
import pe.edu.utp.piuravigila.service.Fakes.FakeReporteDAO;

class ControlAntiSpamTest {
    static final String AHORA = "2026-10-21T10:00:00";
    private final FakeReporteDAO dao = new FakeReporteDAO();
    private final ControlAntiSpam control = new ControlAntiSpam(dao, Fakes.reloj(AHORA), 60, 3);

    @Test void suma_incorrecta_se_rechaza_y_pide_otra_suma() {
        ResultadoControl r = control.validar("d", 5, 7);
        assertFalse(r.permitido());
        assertEquals("Verificación incorrecta", r.mensaje());
        assertTrue(r.regenerarSuma());
    }

    @Test void suma_vacia_se_rechaza() { assertFalse(control.validar("d", null, 7).permitido()); }

    @Test void antes_de_60_s_indica_cuanto_falta() {
        dao.ultimo = Optional.of(LocalDateTime.parse(AHORA).minusSeconds(20));
        ResultadoControl r = control.validar("d", 7, 7);
        assertFalse(r.permitido());
        assertEquals("Espera antes de enviar otro", r.mensaje());
        assertEquals(40, r.segundosFaltan());
    }

    @Test void a_los_60_s_exactos_ya_se_permite() {
        dao.ultimo = Optional.of(LocalDateTime.parse(AHORA).minusSeconds(60));
        assertTrue(control.validar("d", 7, 7).permitido());
    }

    @Test void con_3_reportes_en_el_dia_se_bloquea() {
        dao.enElDia = 3;
        ResultadoControl r = control.validar("d", 7, 7);
        assertFalse(r.permitido());
        assertEquals("Límite diario alcanzado", r.mensaje());
    }

    @Test void con_2_reportes_en_el_dia_se_permite() {
        dao.enElDia = 2;
        assertTrue(control.validar("d", 7, 7).permitido());
    }
}
