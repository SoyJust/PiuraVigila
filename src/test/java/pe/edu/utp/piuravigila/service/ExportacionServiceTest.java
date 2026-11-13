package pe.edu.utp.piuravigila.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.utp.piuravigila.dao.ReporteDAO;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.EstadoReporte;

/** v1.1 · el Excel de gestión (TC-X01 a TC-X04). */
class ExportacionServiceTest {
    private static final LocalDate D = LocalDate.of(2026, 10, 1), H = D.plusDays(7);
    private DashboardService dashboard;
    private ReporteDAO dao;
    private ExportacionService svc;

    @BeforeEach
    void montar() {
        dashboard = mock(DashboardService.class);
        dao = mock(ReporteDAO.class);
        when(dashboard.zonasCriticas(D, H)).thenReturn(List.of(new ConteoZona("=1+1", 4), new ConteoZona("Castilla", 2)));
        when(dashboard.resumenGestion(D, H)).thenReturn(new ResumenGestion(
                Map.of(EstadoReporte.RECIBIDO, 1, EstadoReporte.EN_PROCESO, 1, EstadoReporte.ATENDIDO, 2), 3, 75.0, 4.0, 10.0, false));
        when(dashboard.tiemposAtencion(D, H)).thenReturn(List.of(new TiempoCategoria("Bache en la vía", 2, 3.5)));
        LocalDateTime t = LocalDateTime.of(2026, 10, 2, 9, 0);
        when(dao.casosParaExportar(any(), any())).thenReturn(List.of(
                new FilaExportacion("PR-2026-0002", t, "Bache en la vía", "Piura", "Atendido", 1, t.plusDays(3), t.plusDays(3), "=HYPERLINK(\"http://x\")"),
                new FilaExportacion("PR-2026-0003", t.plusDays(1), "Luminaria dañada", "Castilla", "Recibido", 0, null, null, "Poste sin luz")));
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T15:00:00Z"), ZoneId.of("America/Lima"));
        svc = new ExportacionService(dashboard, dao, reloj);
    }

    private XSSFWorkbook abrir(byte[] b) throws Exception { return new XSSFWorkbook(new ByteArrayInputStream(b)); }

    @Test @DisplayName("TC-X01 · el operador recibe Resumen, Casos y Distritos; el administrador además Tiempos de atención")
    void hojas_segun_rol() throws Exception {
        try (var op = abrir(svc.libro(D, H, false)); var adm = abrir(svc.libro(D, H, true))) {
            assertEquals(List.of("Resumen", "Casos", "Distritos"), List.of(op.getSheetName(0), op.getSheetName(1), op.getSheetName(2)));
            assertEquals(3, op.getNumberOfSheets());
            assertEquals(4, adm.getNumberOfSheets());
            assertEquals("Tiempos de atención", adm.getSheetName(3));
        }
    }

    @Test @DisplayName("TC-X02 · los textos del vecino se guardan como texto, nunca como fórmula (inyección en Excel)")
    void texto_no_se_vuelve_formula() throws Exception {
        try (var l = abrir(svc.libro(D, H, false))) {
            var desc = l.getSheet("Casos").getRow(5).getCell(9);
            assertEquals(CellType.STRING, desc.getCellType());
            assertEquals("=HYPERLINK(\"http://x\")", desc.getStringCellValue());
            var zona = l.getSheet("Distritos").getRow(5).getCell(0);
            assertEquals(CellType.STRING, zona.getCellType());
            assertEquals("=1+1", zona.getStringCellValue());
        }
    }

    @Test @DisplayName("TC-X03 · Casos trae fechas reales, días hasta atender o días abierto, filtros y encabezado fijo")
    void hoja_casos() throws Exception {
        try (var l = abrir(svc.libro(D, H, false))) {
            var h = l.getSheet("Casos");
            assertEquals("Código", h.getRow(4).getCell(0).getStringCellValue());
            assertEquals(LocalDateTime.of(2026, 10, 2, 9, 0), h.getRow(5).getCell(1).getLocalDateTimeCellValue());
            assertEquals(3.0, h.getRow(5).getCell(7).getNumericCellValue(), 0.01);       // atendido en 3 días
            assertNull(h.getRow(5).getCell(8));                                           // no figura como abierto
            assertEquals(6.0, h.getRow(6).getCell(8).getNumericCellValue(), 0.05);       // abierto desde el 03/10 09:00 (6 días y 1 h)
            assertNotNull(h.getCTWorksheet().getAutoFilter());
            assertNotNull(h.getPaneInformation());
        }
    }

    @Test @DisplayName("TC-X04 · Distritos suma el total y trae un gráfico de barras; Resumen calcula el % atendido")
    void distritos_y_resumen() throws Exception {
        try (var l = abrir(svc.libro(D, H, false))) {
            var d = l.getSheet("Distritos");
            assertEquals("Total", d.getRow(7).getCell(0).getStringCellValue());
            assertEquals(6.0, d.getRow(7).getCell(1).getNumericCellValue());
            assertEquals(1, d.getDrawingPatriarch().getCharts().size());
            var r = l.getSheet("Resumen");
            assertEquals(4.0, r.getRow(5).getCell(1).getNumericCellValue());   // casos del periodo
            assertEquals(50.0, r.getRow(9).getCell(1).getNumericCellValue());  // 2 de 4 atendidos
            assertTrue(r.getRow(1).getCell(0).getStringCellValue().contains("01/10/2026"));
        }
    }
}
