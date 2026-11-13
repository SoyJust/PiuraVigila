package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.ReporteServiceTest.Fx;

/** v1.2 · estado «Descartado» (TC-D01 a TC-D07). */
class DescartadoTest {
    private static final UsuarioMunicipal ADM = new UsuarioMunicipal(1, "Administrador", "admin", "hash", RolUsuario.ADMINISTRADOR);
    private static final String MOTIVO = "La foto no muestra ningún problema en la vía pública";

    @Test @DisplayName("TC-D01 · transiciones: Recibido y En proceso pueden descartarse; Atendido y Descartado son finales")
    void transiciones() {
        assertTrue(EstadoReporte.RECIBIDO.puedePasarA(EstadoReporte.DESCARTADO));
        assertTrue(EstadoReporte.EN_PROCESO.puedePasarA(EstadoReporte.DESCARTADO));
        assertFalse(EstadoReporte.ATENDIDO.puedePasarA(EstadoReporte.DESCARTADO));
        for (EstadoReporte e : EstadoReporte.values()) assertFalse(EstadoReporte.DESCARTADO.puedePasarA(e));
        assertFalse(EstadoReporte.RECIBIDO.puedePasarA(EstadoReporte.ATENDIDO), "sigue sin poder saltarse «En proceso»");
        assertFalse(EstadoReporte.DESCARTADO.estaAbierto());
        assertTrue(EstadoReporte.DESCARTADO.exigeMotivo());
        assertFalse(EstadoReporte.ATENDIDO.exigeMotivo());
    }

    @Test @DisplayName("TC-D02 · descartar exige motivo de al menos 10 caracteres; sin él no cambia nada")
    void motivo_obligatorio() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        for (String malo : new String[]{null, "", "   ", "no sirve"}) // «no sirve» = 8 caracteres
            assertThrows(IllegalArgumentException.class, () -> f.service.cambiarEstado(caso.reporteId(), EstadoReporte.DESCARTADO, ADM, malo));
        assertEquals(EstadoReporte.RECIBIDO, f.reportes.buscarPorId(caso.reporteId()).orElseThrow().estado());
        assertTrue(f.historial.filas.isEmpty());
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.DESCARTADO, ADM, "Es de prueba");   // 12 caracteres
        assertEquals(EstadoReporte.DESCARTADO, f.reportes.buscarPorId(caso.reporteId()).orElseThrow().estado());
        assertEquals("Es de prueba", f.historial.filas.get(0).comentario());
    }

    @Test @DisplayName("TC-D03 · un caso atendido no se puede descartar")
    void atendido_no_se_descarta() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.ATENDIDO);
        assertThrows(IllegalStateException.class, () -> f.service.cambiarEstado(caso.reporteId(), EstadoReporte.DESCARTADO, ADM, MOTIVO));
    }

    @Test @DisplayName("TC-D04 · la consulta de un caso descartado muestra Recibido → Descartado con el motivo")
    void consulta_de_un_descartado() {
        Fx f = new Fx();
        f.service.registrarReporte(ReporteServiceTest.valido(1, 1));
        f.service.cambiarEstado(f.reportes.filas.get(0).reporteId(), EstadoReporte.DESCARTADO, ADM, MOTIVO);
        ConsultaEstado c = f.service.consultarEstado("PR-2026-0001");
        assertEquals(List.of(EstadoReporte.RECIBIDO, EstadoReporte.DESCARTADO), c.pasos().stream().map(PasoEstado::estado).toList());
        assertTrue(c.pasos().get(1).actual());
        assertEquals(MOTIVO, c.pasos().get(1).comentario());
    }

    @Test @DisplayName("TC-D05 · si pasó por «En proceso», la consulta muestra los tres pasos reales")
    void consulta_descartado_desde_en_proceso() {
        Fx f = new Fx();
        f.service.registrarReporte(ReporteServiceTest.valido(1, 1));
        int id = f.reportes.filas.get(0).reporteId();
        f.service.cambiarEstado(id, EstadoReporte.EN_PROCESO, ADM);
        f.service.cambiarEstado(id, EstadoReporte.DESCARTADO, ADM, MOTIVO);
        assertEquals(List.of(EstadoReporte.RECIBIDO, EstadoReporte.EN_PROCESO, EstadoReporte.DESCARTADO),
                f.service.consultarEstado("PR-2026-0001").pasos().stream().map(PasoEstado::estado).toList());
    }

    @Test @DisplayName("TC-D06 · un caso descartado está cerrado: no admite vinculaciones y un reporte nuevo crea otro caso")
    void descartado_esta_cerrado() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.DESCARTADO, ADM, MOTIVO);
        ResultadoRegistro r = f.service.registrarReporte(ReporteServiceTest.valido(1, 1));
        assertEquals(ResultadoRegistro.Tipo.REGISTRADO, r.tipo(), "no se ofrece unirse a un caso descartado");
        f.reportes.ultimo = java.util.Optional.empty();
        ResultadoRegistro v = f.service.responderDuplicado(ReporteServiceTest.valido(1, 1), true, caso.reporteId());
        assertEquals(ResultadoRegistro.Tipo.RECHAZADO, v.tipo());
    }

    @Test @DisplayName("TC-D07 · R03 incluye «Descartado» (en cero si no hay)")
    void resumen_incluye_descartado() {
        var dash = new DashboardService(new Fakes.FakeReporteDAO(), new Fakes.FakeHistorialDAO(), new Fakes.FakeCategoriaDAO(), 10);
        var r = dash.resumenGestion(java.time.LocalDate.of(2026, 10, 1), java.time.LocalDate.of(2026, 10, 31));
        assertEquals(0, r.casosPorEstado().get(EstadoReporte.DESCARTADO));
    }
}
