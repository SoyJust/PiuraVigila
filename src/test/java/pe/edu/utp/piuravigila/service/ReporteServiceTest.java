package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.Fakes.*;

/** Reglas críticas de ReporteService con DAO simulados (sin Spring ni PostgreSQL). */
class ReporteServiceTest {
    static final String AHORA = "2026-10-21T10:00:00";

    static final class Fx {
        final FakeReporteDAO reportes = new FakeReporteDAO();
        final FakeHistorialDAO historial = new FakeHistorialDAO();
        final FakeCategoriaDAO categorias = new FakeCategoriaDAO();
        final FakeFotos fotos = new FakeFotos();
        final Clock reloj = Fakes.reloj(AHORA);
        final ReporteService service = new ReporteService(reportes, historial, categorias, new FakeZonaDAO(),
                new ValidadorReporte(2 * 1024 * 1024), new ControlAntiSpam(reportes, reloj, 60, 3), fotos, reloj);
    }

    static DatosReporte datos(int cat, int zona, byte[] foto, int respuesta, int esperada) {
        return new DatosReporte(cat, zona, "Bache grande frente al mercado", "foto.jpg", foto, null, null, "disp-0001-abcd", respuesta, esperada);
    }
    static DatosReporte valido(int cat, int zona) { return datos(cat, zona, new byte[1000], 7, 7); }

    @Test void registra_caso_nuevo_con_codigo_y_historial_recibido() {
        Fx f = new Fx();
        ResultadoRegistro r = f.service.registrarReporte(valido(1, 1));
        assertEquals(ResultadoRegistro.Tipo.REGISTRADO, r.tipo());
        assertTrue(r.codigo().matches("^PR-[0-9]{4}-[0-9]{4}$"));
        assertEquals(1, f.reportes.insertadosCaso);
        assertEquals(EstadoReporte.RECIBIDO, f.historial.filas.get(0).estadoNuevo());
        assertNull(f.historial.filas.get(0).usuarioId()); // «Recibido» automático del sistema
    }

    @Test void rechaza_si_faltan_campos_y_no_guarda() {
        Fx f = new Fx();
        ResultadoRegistro r = f.service.registrarReporte(valido(0, 1));
        assertEquals(ResultadoRegistro.Tipo.RECHAZADO, r.tipo());
        assertEquals("Faltan campos obligatorios", r.mensaje());
        assertEquals(0, f.reportes.insertadosCaso);
        assertEquals(0, f.fotos.guardadas);
    }

    @Test void rechaza_foto_de_mas_de_2_mb() {
        Fx f = new Fx();
        ResultadoRegistro r = f.service.registrarReporte(datos(1, 1, new byte[2 * 1024 * 1024 + 1], 7, 7));
        assertEquals("Foto demasiado pesada", r.mensaje());
        assertEquals(0, f.reportes.insertadosCaso);
    }

    @Test void detecta_duplicado_abierto_y_no_inserta_hasta_que_decida_el_ciudadano() {
        Fx f = new Fx();
        f.reportes.caso(1, 1, EstadoReporte.EN_PROCESO);
        ResultadoRegistro r = f.service.registrarReporte(valido(1, 1));
        assertEquals(ResultadoRegistro.Tipo.DUPLICADO_PENDIENTE, r.tipo());
        assertEquals(1, f.reportes.insertadosCaso); // solo el caso previo
        assertEquals(0, f.reportes.insertadosVinculado);
    }

    @Test void misma_categoria_con_otra_zona_no_es_duplicado() {
        Fx f = new Fx();
        f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        assertEquals(ResultadoRegistro.Tipo.REGISTRADO, f.service.registrarReporte(valido(1, 2)).tipo());
    }

    @Test void caso_atendido_no_cuenta_como_duplicado() {
        Fx f = new Fx();
        f.reportes.caso(1, 1, EstadoReporte.ATENDIDO);
        assertEquals(ResultadoRegistro.Tipo.REGISTRADO, f.service.registrarReporte(valido(1, 1)).tipo());
    }

    @Test void responder_si_vincula_sin_codigo_propio_y_devuelve_el_codigo_del_caso() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        ResultadoRegistro r = f.service.responderDuplicado(valido(1, 1), true, caso.reporteId());
        assertEquals(ResultadoRegistro.Tipo.VINCULADO, r.tipo());
        assertEquals(caso.codigoSeguimiento(), r.codigo());
        assertEquals(1, f.reportes.insertadosVinculado);
        Reporte vinc = f.reportes.filas.get(f.reportes.filas.size() - 1);
        assertNull(vinc.codigoSeguimiento());
        assertEquals(caso.reporteId(), vinc.reporteOriginalId());
        assertEquals(1, f.reportes.contarVinculados(caso.reporteId()));
    }

    @Test void responder_no_crea_un_caso_nuevo() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        ResultadoRegistro r = f.service.responderDuplicado(valido(1, 1), false, caso.reporteId());
        assertEquals(ResultadoRegistro.Tipo.REGISTRADO, r.tipo());
        assertEquals(2, f.reportes.insertadosCaso);
        assertEquals(0, f.reportes.insertadosVinculado);
    }

    @Test void no_vincula_a_un_caso_que_ya_fue_atendido() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        f.reportes.actualizarEstado(caso.reporteId(), EstadoReporte.ATENDIDO);
        ResultadoRegistro r = f.service.responderDuplicado(valido(1, 1), true, caso.reporteId());
        assertEquals(ResultadoRegistro.Tipo.RECHAZADO, r.tipo());
        assertEquals(0, f.reportes.insertadosVinculado);
    }

    @Test void no_vincula_a_un_caso_de_otra_categoria() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(2, 1, EstadoReporte.RECIBIDO);
        assertThrows(IllegalArgumentException.class, () -> f.service.responderDuplicado(valido(1, 1), true, caso.reporteId()));
    }

    @Test void el_vinculado_cuenta_para_el_limite_diario() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        f.reportes.enElDia = 3;
        ResultadoRegistro r = f.service.responderDuplicado(valido(1, 1), true, caso.reporteId());
        assertEquals("Límite diario alcanzado", r.mensaje());
        assertEquals(0, f.reportes.insertadosVinculado);
    }

    @Test void estado_avanza_y_queda_con_responsable() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        UsuarioMunicipal op = new UsuarioMunicipal(5, "Operador", "op1", "hash", RolUsuario.OPERADOR);
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, op);
        assertEquals(EstadoReporte.EN_PROCESO, f.reportes.buscarPorId(caso.reporteId()).get().estado());
        HistorialEstado h = f.historial.filas.get(0);
        assertEquals(5, h.usuarioId());
        assertEquals(LocalDateTime.parse(AHORA), h.fechaCambio());
    }

    @Test void estado_no_retrocede_ni_se_salta() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        UsuarioMunicipal op = new UsuarioMunicipal(5, "Operador", "op1", "hash", RolUsuario.OPERADOR);
        assertThrows(IllegalStateException.class, () -> f.service.cambiarEstado(caso.reporteId(), EstadoReporte.ATENDIDO, op));
        f.service.cambiarEstado(caso.reporteId(), EstadoReporte.EN_PROCESO, op);
        assertThrows(IllegalStateException.class, () -> f.service.cambiarEstado(caso.reporteId(), EstadoReporte.RECIBIDO, op));
        assertEquals(1, f.historial.filas.size());
    }

    @Test void un_reporte_vinculado_no_cambia_de_estado() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        f.service.responderDuplicado(valido(1, 1), true, caso.reporteId());
        int idVinculado = f.reportes.filas.get(f.reportes.filas.size() - 1).reporteId();
        UsuarioMunicipal op = new UsuarioMunicipal(5, "Operador", "op1", "hash", RolUsuario.OPERADOR);
        assertThrows(IllegalStateException.class, () -> f.service.cambiarEstado(idVinculado, EstadoReporte.EN_PROCESO, op));
    }

    @Test void consulta_devuelve_ultima_actualizacion_del_historial() {
        Fx f = new Fx();
        f.service.registrarReporte(valido(1, 1));
        ConsultaEstado c = f.service.consultarEstado(" pr-2026-0001 ");
        assertEquals(EstadoReporte.RECIBIDO, c.caso().estado());
        assertEquals(LocalDateTime.parse(AHORA), c.ultimaActualizacion());
    }

    @Test void consulta_muestra_los_tres_pasos_y_solo_los_alcanzados_llevan_fecha() {
        Fx f = new Fx();
        f.service.registrarReporte(valido(1, 1));
        UsuarioMunicipal op = new UsuarioMunicipal(5, "Operador", "op1", "hash", RolUsuario.OPERADOR);
        f.service.cambiarEstado(f.reportes.filas.get(0).reporteId(), EstadoReporte.EN_PROCESO, op);
        ConsultaEstado c = f.service.consultarEstado("PR-2026-0001");
        assertEquals(3, c.pasos().size());
        assertEquals(EstadoReporte.RECIBIDO, c.pasos().get(0).estado());
        assertTrue(c.pasos().get(0).alcanzado());
        assertTrue(c.pasos().get(1).alcanzado());
        assertTrue(c.pasos().get(1).actual());
        assertFalse(c.pasos().get(2).alcanzado());
        assertNull(c.pasos().get(2).fecha());
    }

    @Test void consulta_de_un_caso_sin_historial_usa_la_fecha_de_registro_como_recibido() {
        Fx f = new Fx();
        Reporte caso = f.reportes.caso(1, 1, EstadoReporte.RECIBIDO);
        ConsultaEstado c = f.service.consultarEstado(caso.codigoSeguimiento());
        assertEquals(caso.fechaRegistro(), c.pasos().get(0).fecha());
        assertTrue(c.pasos().get(0).actual());
        assertFalse(c.pasos().get(1).alcanzado());
    }

    @Test void consulta_con_codigo_inexistente_o_mal_formado_falla() {
        Fx f = new Fx();
        assertThrows(IllegalArgumentException.class, () -> f.service.consultarEstado("PR-2026-9999"));
        assertThrows(IllegalArgumentException.class, () -> f.service.consultarEstado("ABC"));
        assertThrows(IllegalArgumentException.class, () -> f.service.consultarEstado(null));
    }
}
