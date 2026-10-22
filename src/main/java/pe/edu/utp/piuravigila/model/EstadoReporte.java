package pe.edu.utp.piuravigila.model;

/** Estado de un caso. Solo avanza: Recibido → En proceso → Atendido (P03). */
public enum EstadoReporte {
    RECIBIDO("Recibido"), EN_PROCESO("En proceso"), ATENDIDO("Atendido");

    private final String etiqueta;
    EstadoReporte(String etiqueta) { this.etiqueta = etiqueta; }
    public String etiqueta() { return etiqueta; }

    /** Abierto = admite vinculaciones (RN-01). */
    public boolean estaAbierto() { return this != ATENDIDO; }

    public boolean puedePasarA(EstadoReporte nuevo) { return nuevo != null && nuevo.ordinal() == ordinal() + 1; }

    public static EstadoReporte desdeEtiqueta(String texto) {
        for (EstadoReporte e : values()) if (e.etiqueta.equals(texto)) return e;
        throw new IllegalArgumentException("Estado desconocido: " + texto);
    }
}
