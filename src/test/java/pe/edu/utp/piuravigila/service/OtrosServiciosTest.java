package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.dto.DatosReporte;
import pe.edu.utp.piuravigila.model.*;
import pe.edu.utp.piuravigila.service.Fakes.*;

class OtrosServiciosTest {
    // ---- ValidadorReporte (RF-01) ----
    private static DatosReporte conFoto(int bytes, String desc) {
        return new DatosReporte(1, 1, desc, "f.jpg", new byte[bytes], null, null, "disp-0001-abcd", 1, 1);
    }
    @Test void foto_de_exactamente_2_mb_es_valida() {
        assertTrue(new ValidadorReporte(2 * 1024 * 1024).validar(conFoto(2 * 1024 * 1024, "ok")).permitido());
    }
    @Test void foto_de_2_mb_mas_un_byte_se_rechaza() {
        assertFalse(new ValidadorReporte(2 * 1024 * 1024).validar(conFoto(2 * 1024 * 1024 + 1, "ok")).permitido());
    }
    @Test void descripcion_de_301_caracteres_se_rechaza() {
        assertFalse(new ValidadorReporte(2 * 1024 * 1024).validar(conFoto(10, "x".repeat(301))).permitido());
    }

    // ---- CategoriaService (RF-07) ----
    @Test void habilita_categoria_nueva() {
        FakeCategoriaDAO dao = new FakeCategoriaDAO();
        new CategoriaService(dao, 10).habilitar("  Poste   caído ");
        assertTrue(dao.existeNombre("Poste caído"));
    }
    @Test void no_crea_categoria_que_ya_existe() {
        assertThrows(IllegalStateException.class, () -> new CategoriaService(new FakeCategoriaDAO(), 10).habilitar("otra"));
    }
    @Test void rechaza_nombre_vacio() {
        assertThrows(IllegalArgumentException.class, () -> new CategoriaService(new FakeCategoriaDAO(), 10).habilitar("   "));
    }
    @Test void umbral_otra_solo_se_supera_por_encima_de_10() {
        FakeCategoriaDAO dao = new FakeCategoriaDAO();
        CategoriaService s = new CategoriaService(dao, 10);
        dao.pctOtra = 10.0; assertFalse(s.umbralOtraSuperado());
        dao.pctOtra = 10.1; assertTrue(s.umbralOtraSuperado());
    }

    // ---- AuthService (RNF-01) ----
    private static final HashContrasena HASH_FALSO = new HashContrasena() {
        public String codificar(String c) { return "h:" + c; }
        public boolean coincide(String c, String h) { return h.equals("h:" + c); }
    };
    @Test void autentica_con_credenciales_correctas_e_incorrectas() {
        FakeUsuarioDAO dao = new FakeUsuarioDAO();
        dao.m.put("op1", new UsuarioMunicipal(5, "Operador", "op1", "h:secreta", RolUsuario.OPERADOR));
        AuthService a = new AuthService(dao, HASH_FALSO);
        assertTrue(a.autenticar("op1", "secreta").isPresent());
        assertTrue(a.autenticar("op1", "otra").isEmpty());
        assertTrue(a.autenticar("nadie", "secreta").isEmpty());
        assertTrue(a.autenticar("", "").isEmpty());
    }

    // ---- DashboardService (R01 a R03) ----
    @Test void periodo_invertido_se_rechaza() {
        DashboardService d = new DashboardService(new FakeReporteDAO(), new FakeHistorialDAO(), new FakeCategoriaDAO(), 10);
        assertThrows(IllegalArgumentException.class, () -> d.zonasCriticas(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1)));
    }
    @Test void resumen_completa_estados_en_cero_y_marca_umbral() {
        FakeReporteDAO rep = new FakeReporteDAO();
        rep.porEstado = Map.of(EstadoReporte.RECIBIDO, 2); rep.vinculadosPeriodo = 3; rep.pctAct = 0;
        FakeCategoriaDAO cat = new FakeCategoriaDAO(); cat.pctOtra = 12.5;
        ResumenGestion r = new DashboardService(rep, new FakeHistorialDAO(), cat, 10).resumenGestion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21));
        assertEquals(0, r.casosPorEstado().get(EstadoReporte.ATENDIDO));
        assertEquals(2, r.casosPorEstado().get(EstadoReporte.RECIBIDO));
        assertEquals(3, r.duplicadosEvitados());
        assertTrue(r.superaUmbral());
    }
}
