package pe.edu.utp.piuravigila.model;

/**
 * Estado de un caso. Avanza Recibido → En proceso → Atendido (P03).
 * v1.2 · un caso abierto (Recibido o En proceso) también puede pasar a Descartado cuando el reporte es falso, de prueba o
 * no corresponde a la vía pública; exige un motivo que ve el vecino. Atendido y Descartado son estados finales.
 */
public enum EstadoReporte {
    RECIBIDO("Recibido"), EN_PROCESO("En proceso"), ATENDIDO("Atendido"), DESCARTADO("Descartado");

    /** Largo mínimo del motivo al descartar (también lo exige la BD: ck_historial_motivo). */
    public static final int MIN_MOTIVO = 10;

    private final String etiqueta;
    EstadoReporte(String etiqueta) { this.etiqueta = etiqueta; }
    public String etiqueta() { return etiqueta; }

    /** Abierto = admite vinculaciones (RN-01) y cambios de estado. */
    public boolean estaAbierto() { return this == RECIBIDO || this == EN_PROCESO; }

    public boolean puedePasarA(EstadoReporte nuevo) {
        if (nuevo == null) return false;
        return switch (this) {
            case RECIBIDO -> nuevo == EN_PROCESO || nuevo == DESCARTADO;
            case EN_PROCESO -> nuevo == ATENDIDO || nuevo == DESCARTADO;
            case ATENDIDO, DESCARTADO -> false;
        };
    }

    /** Pasar a este estado exige un comentario con el motivo. */
    public boolean exigeMotivo() { return this == DESCARTADO; }

    public static EstadoReporte desdeEtiqueta(String texto) {
        for (EstadoReporte e : values()) if (e.etiqueta.equals(texto)) return e;
        throw new IllegalArgumentException("Estado desconocido: " + texto);
    }
}
