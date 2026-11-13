package pe.edu.utp.piuravigila.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.utp.piuravigila.dao.ReporteDAO;
import pe.edu.utp.piuravigila.dto.*;
import pe.edu.utp.piuravigila.model.EstadoReporte;

/**
 * v1.1 · reporte de gestión en Excel con varias hojas: Resumen (R03), Casos (lista con filtros), Distritos (R01 con
 * gráfico) y, solo para el administrador, Tiempos de atención (R02). Sin fotos, sin identificador de dispositivo y sin
 * nombres del personal. Todo texto se escribe como texto: una descripción que empiece con «=» no se vuelve fórmula.
 */
@Service
public class ExportacionService {
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DMYHM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DashboardService dashboard;
    private final ReporteDAO reportes;
    private final Clock reloj;

    @Autowired
    public ExportacionService(DashboardService dashboard, ReporteDAO reportes, Clock reloj) {
        this.dashboard = dashboard; this.reportes = reportes; this.reloj = reloj;
    }

    /** Compatibilidad: solo la hoja de distritos (la usaba la v1.0). */
    public ExportacionService(DashboardService dashboard) { this(dashboard, null, Clock.systemDefaultZone()); }

    public byte[] zonas(LocalDate desde, LocalDate hasta) { return libro(desde, hasta, false); }

    public byte[] libro(LocalDate desde, LocalDate hasta, boolean incluirTiempos) {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(libro);
            if (reportes != null) {
                resumen(libro, e, desde, hasta);
                casos(libro, e, desde, hasta);
            }
            distritos(libro, e, desde, hasta);
            if (incluirTiempos) tiempos(libro, e, desde, hasta);
            libro.setActiveSheet(0);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) { throw new UncheckedIOException("No se pudo exportar el reporte.", ex); }
    }

    // ---- Hoja 1 · Resumen (R03) ----
    private void resumen(XSSFWorkbook libro, Estilos e, LocalDate desde, LocalDate hasta) {
        XSSFSheet h = libro.createSheet("Resumen");
        encabezado(h, e, "PiuraVigila · Reporte de gestión", desde, hasta);
        ResumenGestion r = dashboard.resumenGestion(desde, hasta);
        int rec = r.casosPorEstado().getOrDefault(EstadoReporte.RECIBIDO, 0);
        int pro = r.casosPorEstado().getOrDefault(EstadoReporte.EN_PROCESO, 0);
        int ate = r.casosPorEstado().getOrDefault(EstadoReporte.ATENDIDO, 0);
        int total = rec + pro + ate;
        fila(h, 4, e.cabecera, "Indicador", "Valor");
        Object[][] datos = {
                {"Casos registrados en el periodo", total},
                {"Recibidos (por atender)", rec},
                {"En proceso", pro},
                {"Atendidos", ate},
                {"Casos atendidos (%)", total == 0 ? 0.0 : ate * 100.0 / total},
                {"Reportes de vecinos unidos a un caso (duplicados evitados)", r.duplicadosEvitados()},
                {"Casos con estado actualizado (%)", r.pctActualizados()},
                {"Reportes en «Otra» del mes (%) · umbral " + (int) r.umbralOtra() + " %", r.pctOtraMes()},
        };
        int i = 5;
        for (Object[] d : datos) {
            Row row = h.createRow(i++);
            texto(row, 0, (String) d[0], e.normal);
            Cell c = row.createCell(1);
            if (d[1] instanceof Double v) { c.setCellValue(v); c.setCellStyle(e.decimal); } else { c.setCellValue((Integer) d[1]); c.setCellStyle(e.entero); }
        }
        texto(h.createRow(i + 1), 0, "Fuente: PiuraVigila. Datos agregados, sin fotos ni datos personales de los vecinos.", e.nota);
        h.setColumnWidth(0, 62 * 256); h.setColumnWidth(1, 14 * 256);
    }

    // ---- Hoja 2 · Casos ----
    private void casos(XSSFWorkbook libro, Estilos e, LocalDate desde, LocalDate hasta) {
        XSSFSheet h = libro.createSheet("Casos");
        encabezado(h, e, "Casos registrados en el periodo", desde, hasta);
        String[] cols = {"Código", "Fecha de registro", "Tipo de problema", "Distrito", "Estado", "Vecinos vinculados",
                "Última actualización", "Días hasta atender", "Días abierto", "Descripción"};
        fila(h, 4, e.cabecera, cols);
        List<FilaExportacion> filas = reportes.casosParaExportar(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int i = 5;
        for (FilaExportacion f : filas) {
            Row row = h.createRow(i++);
            texto(row, 0, f.codigo(), e.normal);
            fecha(row, 1, f.fechaRegistro(), e.fecha);
            texto(row, 2, f.categoria(), e.normal);
            texto(row, 3, f.zona(), e.normal);
            texto(row, 4, f.estado(), e.normal);
            numero(row, 5, f.vinculados(), e.entero);
            fecha(row, 6, f.ultimaActualizacion() == null ? f.fechaRegistro() : f.ultimaActualizacion(), e.fecha);
            if (f.fechaAtendido() != null) numero(row, 7, dias(f.fechaRegistro(), f.fechaAtendido()), e.decimal);
            else numero(row, 8, dias(f.fechaRegistro(), ahora), e.decimal);
            texto(row, 9, f.descripcion(), e.ajustado);
        }
        if (filas.isEmpty()) texto(h.createRow(5), 0, "No hay casos registrados en este periodo.", e.nota);
        else h.setAutoFilter(new CellRangeAddress(4, i - 1, 0, cols.length - 1));
        h.createFreezePane(1, 5);
        int[] anchos = {15, 17, 30, 22, 12, 11, 17, 11, 11, 70};
        for (int c = 0; c < anchos.length; c++) h.setColumnWidth(c, anchos[c] * 256);
    }

    // ---- Hoja 3 · Distritos (R01) con gráfico ----
    private void distritos(XSSFWorkbook libro, Estilos e, LocalDate desde, LocalDate hasta) {
        XSSFSheet h = libro.createSheet("Distritos");
        encabezado(h, e, "Reportes por distrito (incluye los vinculados)", desde, hasta);
        fila(h, 4, e.cabecera, "Distrito", "Reportes");
        List<ConteoZona> filas = dashboard.zonasCriticas(desde, hasta);
        int i = 5, total = 0;
        for (ConteoZona z : filas) {
            Row row = h.createRow(i++);
            texto(row, 0, z.zona(), e.normal); // tipo texto explícito: un nombre que empieza con «=» no es fórmula
            numero(row, 1, z.reportes(), e.entero);
            total += z.reportes();
        }
        Row t = h.createRow(i);
        texto(t, 0, "Total", e.cabecera);
        numero(t, 1, total, e.totalEntero);
        h.setColumnWidth(0, 30 * 256); h.setColumnWidth(1, 12 * 256);
        h.createFreezePane(0, 5);
        if (!filas.isEmpty()) grafico(h, 5, i - 1);
    }

    private static void grafico(XSSFSheet h, int primera, int ultima) {
        XSSFDrawing dibujo = h.createDrawingPatriarch();
        XSSFChart g = dibujo.createChart(dibujo.createAnchor(0, 0, 0, 0, 3, 3, 12, 3 + Math.max(16, (ultima - primera + 1) * 2)));
        g.setTitleText("Reportes por distrito");
        g.setTitleOverlay(false);
        XDDFCategoryAxis eje = g.createCategoryAxis(AxisPosition.LEFT);
        eje.setOrientation(AxisOrientation.MAX_MIN); // el distrito con más reportes arriba
        XDDFValueAxis valores = g.createValueAxis(AxisPosition.BOTTOM);
        valores.setCrosses(AxisCrosses.AUTO_ZERO);
        var nombres = XDDFDataSourcesFactory.fromStringCellRange(h, new CellRangeAddress(primera, ultima, 0, 0));
        var cuentas = XDDFDataSourcesFactory.fromNumericCellRange(h, new CellRangeAddress(primera, ultima, 1, 1));
        XDDFBarChartData datos = (XDDFBarChartData) g.createData(ChartTypes.BAR, eje, valores);
        datos.setBarDirection(BarDirection.BAR);
        datos.setVaryColors(false);
        XDDFBarChartData.Series serie = (XDDFBarChartData.Series) datos.addSeries(nombres, cuentas);
        serie.setTitle("Reportes", null);
        XDDFShapeProperties forma = new XDDFShapeProperties();
        forma.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(new byte[]{(byte) 0xF2, (byte) 0xA9, 0x00})));
        serie.setShapeProperties(forma);
        g.plot(datos);
    }

    // ---- Hoja 4 · Tiempos de atención (R02, solo administrador) ----
    private void tiempos(XSSFWorkbook libro, Estilos e, LocalDate desde, LocalDate hasta) {
        XSSFSheet h = libro.createSheet("Tiempos de atención");
        encabezado(h, e, "Tiempo promedio de atención por tipo de problema (R02)", desde, hasta);
        fila(h, 4, e.cabecera, "Tipo de problema", "Casos atendidos", "Días promedio");
        int i = 5;
        for (TiempoCategoria t : dashboard.tiemposAtencion(desde, hasta)) {
            Row row = h.createRow(i++);
            texto(row, 0, t.categoria(), e.normal);
            numero(row, 1, t.casosAtendidos(), e.entero);
            numero(row, 2, t.diasPromedio(), e.decimal);
        }
        if (i == 5) texto(h.createRow(5), 0, "Aún no hay casos atendidos en este periodo.", e.nota);
        h.setColumnWidth(0, 36 * 256); h.setColumnWidth(1, 16 * 256); h.setColumnWidth(2, 16 * 256);
    }

    // ---- auxiliares ----
    private void encabezado(XSSFSheet h, Estilos e, String titulo, LocalDate desde, LocalDate hasta) {
        texto(h.createRow(0), 0, titulo, e.titulo);
        texto(h.createRow(1), 0, "Periodo: del " + DMY.format(desde) + " al " + DMY.format(hasta), e.normal);
        texto(h.createRow(2), 0, "Generado el " + DMYHM.format(LocalDateTime.now(reloj)), e.nota);
    }

    private static void fila(Sheet h, int n, CellStyle estilo, String... textos) {
        Row row = h.createRow(n);
        for (int c = 0; c < textos.length; c++) texto(row, c, textos[c], estilo);
    }

    private static void texto(Row row, int c, String valor, CellStyle estilo) {
        Cell celda = row.createCell(c, CellType.STRING);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private static void numero(Row row, int c, double valor, CellStyle estilo) {
        Cell celda = row.createCell(c, CellType.NUMERIC);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private static void fecha(Row row, int c, LocalDateTime valor, CellStyle estilo) {
        if (valor == null) return;
        Cell celda = row.createCell(c);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private static double dias(LocalDateTime desde, LocalDateTime hasta) {
        return Math.round(Duration.between(desde, hasta).toMinutes() / 144.0) / 10.0; // días con un decimal
    }

    /** Estilos compartidos (Excel admite un número limitado: se crean una sola vez por libro). */
    private static final class Estilos {
        final CellStyle titulo, cabecera, normal, ajustado, nota, entero, totalEntero, decimal, fecha;
        Estilos(XSSFWorkbook libro) {
            DataFormat f = libro.createDataFormat();
            Font negrita = libro.createFont(); negrita.setBold(true);
            Font grande = libro.createFont(); grande.setBold(true); grande.setFontHeightInPoints((short) 14);
            Font blanca = libro.createFont(); blanca.setBold(true); blanca.setColor(IndexedColors.WHITE.getIndex());
            Font gris = libro.createFont(); gris.setItalic(true); gris.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            titulo = libro.createCellStyle(); titulo.setFont(grande);
            XSSFCellStyle cab = libro.createCellStyle();
            cab.setFont(blanca);
            cab.setFillForegroundColor(new XSSFColor(new byte[]{0x10, 0x28, 0x3B}, null));
            cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cab.setVerticalAlignment(VerticalAlignment.CENTER);
            cab.setWrapText(true);
            cabecera = cab;
            normal = libro.createCellStyle(); normal.setVerticalAlignment(VerticalAlignment.TOP);
            ajustado = libro.createCellStyle(); ajustado.setWrapText(true); ajustado.setVerticalAlignment(VerticalAlignment.TOP);
            nota = libro.createCellStyle(); nota.setFont(gris);
            entero = libro.createCellStyle(); entero.setDataFormat(f.getFormat("0")); entero.setVerticalAlignment(VerticalAlignment.TOP);
            totalEntero = libro.createCellStyle(); totalEntero.setDataFormat(f.getFormat("0")); totalEntero.setFont(negrita);
            decimal = libro.createCellStyle(); decimal.setDataFormat(f.getFormat("0.0")); decimal.setVerticalAlignment(VerticalAlignment.TOP);
            fecha = libro.createCellStyle(); fecha.setDataFormat(f.getFormat("dd/mm/yyyy hh:mm")); fecha.setVerticalAlignment(VerticalAlignment.TOP);
        }
    }
}
