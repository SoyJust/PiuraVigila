package pe.edu.utp.piuravigila.service;

public interface HashContrasena {
    String codificar(String claveEnClaro);
    boolean coincide(String claveEnClaro, String hash);
}
