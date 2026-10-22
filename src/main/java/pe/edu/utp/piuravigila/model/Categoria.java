package pe.edu.utp.piuravigila.model;
public record Categoria(int categoriaId, String nombre, boolean activo) {
    public boolean estaActiva() { return activo; }
}
