package pe.edu.utp.piuravigila.service;

import java.io.*;
import java.time.LocalDate;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Exporta exclusivamente conteos agregados, sin fotos ni credenciales. */
@Service
public class ExportacionService {
    private final DashboardService dashboard;
    public ExportacionService(DashboardService dashboard) { this.dashboard = dashboard; }
    public byte[] zonas(LocalDate desde, LocalDate hasta) {
        var filas = dashboard.zonasCriticas(desde, hasta);
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Reportes por zona");
            var periodo = hoja.createRow(0);
            periodo.createCell(0).setCellValue("Desde " + desde + " hasta " + hasta);
            var cabecera = hoja.createRow(2);
            cabecera.createCell(0).setCellValue("Zona");
            cabecera.createCell(1).setCellValue("Reportes");
            var estilo = libro.createCellStyle(); var fuente = libro.createFont(); fuente.setBold(true); estilo.setFont(fuente);
            for (var celda : cabecera) celda.setCellStyle(estilo);
            int i = 3;
            for (var fila : filas) {
                var row = hoja.createRow(i++);
                // Tipo texto explícito: nombres que empiezan con '=' no son fórmulas.
                row.createCell(0).setCellValue(fila.zona());
                row.createCell(1).setCellValue(fila.reportes());
            }
            hoja.setColumnWidth(0, 36 * 256); hoja.setColumnWidth(1, 16 * 256);
            hoja.createFreezePane(0, 3); libro.write(salida); return salida.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException("No se pudo exportar el resumen.", e); }
    }
}
