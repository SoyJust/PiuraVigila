package pe.edu.utp.piuravigila.dto;
/** Punto del mapa de P04: un reporte con ubicación marcada (sin foto). El estado es el del caso original si es un vinculado. */
public record PuntoReporte(double lat, double lng, String zona, String categoria, String estado, String codigo) {}
