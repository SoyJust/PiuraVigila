package pe.edu.utp.piuravigila.dto;
import pe.edu.utp.piuravigila.model.Reporte;

public record ResultadoRegistro(Tipo tipo, String codigo, Reporte casoExistente, String mensaje, boolean regenerarSuma) {
    public enum Tipo { REGISTRADO, DUPLICADO_PENDIENTE, VINCULADO, RECHAZADO }
    public static ResultadoRegistro registrado(String codigo) { return new ResultadoRegistro(Tipo.REGISTRADO, codigo, null, "Código registrado", false); }
    public static ResultadoRegistro duplicado(Reporte caso) { return new ResultadoRegistro(Tipo.DUPLICADO_PENDIENTE, caso.codigoSeguimiento(), caso, "Posible duplicado", false); }
    public static ResultadoRegistro vinculado(String codigoCaso) { return new ResultadoRegistro(Tipo.VINCULADO, codigoCaso, null, "Reporte vinculado", false); }
    public static ResultadoRegistro rechazado(String mensaje, boolean regenerarSuma) { return new ResultadoRegistro(Tipo.RECHAZADO, null, null, mensaje, regenerarSuma); }
}
