package pe.edu.utp.piuravigila.model;

public enum RolUsuario {
    ADMINISTRADOR("administrador"), OPERADOR("operador");
    private final String valorBD;
    RolUsuario(String valorBD) { this.valorBD = valorBD; }
    public String valorBD() { return valorBD; }
    public static RolUsuario desdeBD(String v) {
        for (RolUsuario r : values()) if (r.valorBD.equals(v)) return r;
        throw new IllegalArgumentException("Rol desconocido: " + v);
    }
}
