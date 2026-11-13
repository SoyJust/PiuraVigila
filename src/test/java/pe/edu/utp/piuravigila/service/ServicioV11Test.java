package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.ReporteServiceTest.Fx;

/** v1.1 · bloqueos contra envíos simultáneos, comentario del personal y detalle del caso (TC-C01 a TC-C05). */
class ServicioV11Test {
    private static final UsuarioMunicipal OP = new UsuarioMunicipal(5, "Operador", "op1", "hash", RolUsuario.OPERADOR);

    @Test @DisplayName("TC-C01 · registrar bloquea primero el dispositivo y luego la categoría+zona, en ese orden (sin interbloqueos)")
    void bloquea_dispositivo_y_caso() {
        Fx f = new Fx();
        f.service.registrarReporte(ReporteServiceTest.valido(1, 2));
        assertEquals(java.util.List.of("dispositivo:disp-0001-abcd", "caso:1:2"), f.reportes.bloqueos);
    }

    @Test @DisplayName("TC-C02 · un envío rechazado por campos vacíos no llega a bloquear nada")
    void rechazo_temprano_no_bloquea() {
        Fx f = new Fx();
        f.service.registrarReporte(ReporteServiceTest.valido(0, 2));
        assertTrue(f.reportes.bloqueos.isEmpty());
    }

    @Test @DisplayName("TC-C03 · el comentario se guarda limpio y lo ve el vecino en el paso correspondiente")
    void comentario_visible_en_la_consulta() {
        Fx f = new Fx();
        f.service.registrarReporte(ReporteServiceTest.valido(1, 1));
        int id = f.reportes.filas.get(0).reporteId();
        f.service.cambiarEstado(id, EstadoReporte.EN_PROCESO, OP, "  Cuadrilla   programada\n para el lunes  ");
        assertEquals("Cuadrilla programada para el lunes", f.historial.filas.get(1).comentario());
        ConsultaEstado c = f.service.consultarEstado("PR-2026-0001");
        assertNull(c.pasos().get(0).comentario());
        assertEquals("Cuadrilla programada para el lunes", c.pasos().get(1).comentario());
    }

    @Test @DisplayName("TC-C04 · comentario vacío se guarda como nulo; más de 200 caracteres se rechaza sin cambiar el estado")
    void comentario_limites() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        assertThrows(IllegalArgumentException.class, () -> f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, OP, "x".repeat(201)));
        assertEquals(EstadoReporte.RECIBIDO, f.reportes.buscarPorId(caso.reporteId()).orElseThrow().estado());
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, OP, "   ");
        assertNull(f.historial.filas.get(0).comentario());
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.ATENDIDO, OP, "y".repeat(200));
        assertEquals(200, f.historial.filas.get(1).comentario().length());
    }

    @Test @DisplayName("TC-C06 · cambiar estado bloquea el caso: dos operadores a la vez no duplican el historial")
    void cambiar_estado_bloquea_el_caso() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, OP);
        assertEquals(java.util.List.of("reporte:" + caso.reporteId()), f.reportes.bloqueos);
        // el segundo operador llega después y ve el estado ya cambiado: «En proceso → En proceso» no es válido
        assertThrows(IllegalStateException.class, () -> f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, OP));
        assertEquals(1, f.historial.filas.size());
    }

    @Test @DisplayName("TC-C07 · la ubicación debe estar dentro de la provincia de Piura (valores límite)")
    void ubicacion_dentro_de_piura() {
        ValidadorReporte v = new ValidadorReporte(2 * 1024 * 1024);
        java.util.function.BiFunction<Double, Double, ResultadoControl> con = (la, ln) -> v.validar(new DatosReporte(1, 1,
                "Bache grande", "f.jpg", new byte[10], la, ln, "disp-0001-abcd", 7, 7));
        assertTrue(con.apply(-5.1945, -80.6328).permitido());                 // Piura, centro
        assertTrue(con.apply(-4.6555, -80.2430).permitido());                 // Las Lomas
        assertTrue(con.apply(-5.70, -81.00).permitido());                     // borde
        assertEquals(ValidadorReporte.MSG_FUERA, con.apply(-12.0464, -77.0428).mensaje()); // Lima
        assertEquals(ValidadorReporte.MSG_FUERA, con.apply(-5.71, -80.63).mensaje());
        assertTrue(con.apply(null, null).permitido());                        // la ubicación es opcional
    }

    @Test @DisplayName("TC-C05 · el detalle del caso trae nombres, historial con responsable y los reportes vinculados")
    void detalle_del_caso() {
        Fx f = new Fx();
        f.service.registrarReporte(ReporteServiceTest.valido(1, 2));
        Reporte caso = f.reportes.filas.get(0);
        f.reportes.ultimo = java.util.Optional.empty();
        f.service.responderDuplicado(ReporteServiceTest.valido(1, 2), true, caso.reporteId());
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, OP, "Revisado en campo");
        DetalleCaso d = f.service.detalleCaso(" pr-2026-0001 ");
        assertEquals("Bache en la vía", d.categoria());
        assertEquals("Castilla", d.zona());
        assertEquals(1, d.vinculados().size());
        assertEquals(2, d.movimientos().size());
        assertNull(d.movimientos().get(0).responsable());             // «Recibido» del sistema
        assertEquals("Usuario 5", d.movimientos().get(1).responsable());
        assertEquals("Revisado en campo", d.movimientos().get(1).comentario());
        assertThrows(IllegalArgumentException.class, () -> f.service.detalleCaso("PR-2026-9999"));
        assertThrows(IllegalArgumentException.class, () -> f.service.detalleCaso("'; DROP TABLE reporte;--"));
    }
}
