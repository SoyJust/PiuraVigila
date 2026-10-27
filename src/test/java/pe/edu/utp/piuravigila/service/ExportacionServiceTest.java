package pe.edu.utp.piuravigila.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import pe.edu.utp.piuravigila.dto.ConteoZona;
class ExportacionServiceTest {
    @Test void genera_excel_con_conteos_numericos_y_texto_sin_formulas() throws Exception {
        var dashboard = mock(DashboardService.class);
        var d = LocalDate.of(2026, 10, 1); var h = d.plusDays(7);
        when(dashboard.zonasCriticas(d,h)).thenReturn(List.of(new ConteoZona("=1+1",4)));
        byte[] bytes = new ExportacionService(dashboard).zonas(d,h);
        try(var libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var row = libro.getSheetAt(0).getRow(3);
            assertEquals(CellType.STRING, row.getCell(0).getCellType());
            assertEquals("=1+1", row.getCell(0).getStringCellValue());
            assertEquals(4.0, row.getCell(1).getNumericCellValue());
        }
    }
}
