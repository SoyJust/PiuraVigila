package pe.edu.utp.piuravigila.dto;

public record ResultadoControl(boolean permitido, String mensaje, long segundosFaltan, boolean regenerarSuma) {
    public static ResultadoControl ok() { return new ResultadoControl(true, "", 0, false); }
    public static ResultadoControl rechazo(String mensaje) { return new ResultadoControl(false, mensaje, 0, false); }
}
