package pe.edu.utp.piuravigila.model;
public record Zona(int zonaId, String nombre, String sector) {
    public String getNombre() { return nombre; }
}
