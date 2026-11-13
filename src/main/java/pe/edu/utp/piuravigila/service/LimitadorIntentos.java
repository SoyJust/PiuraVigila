package pe.edu.utp.piuravigila.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * v1.1 · límite de intentos en memoria por clave (por ejemplo «ip:203.0.113.5» o «login:admin») con ventana deslizante.
 * Complementa al control por dispositivo: quien borra la cookie obtiene un «dispositivo» nuevo, pero no una IP nueva.
 * Limitaciones (documentadas): se reinicia si la aplicación se reinicia y no se comparte entre varios servidores; varias
 * personas detrás de la misma IP (datos móviles con CGNAT, una oficina) comparten el límite, por eso es holgado.
 */
@Component
public class LimitadorIntentos {
    private static final int MAX_CLAVES = 50_000; // tope de memoria: si se llena, se olvidan las ventanas ya vencidas
    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimitadorIntentos(Clock reloj) { this.reloj = reloj; }

    /** ¿Ya hay {@code max} intentos o más dentro de la ventana? (no registra nada) */
    public boolean bloqueado(String clave, int max, Duration ventana) {
        Deque<Instant> d = intentos.get(clave);
        if (d == null) return false;
        synchronized (d) { podar(d, ventana); return d.size() >= max; }
    }

    /** Segundos que faltan para que se libere un intento (0 si no está bloqueado). */
    public long segundosParaLiberar(String clave, int max, Duration ventana) {
        Deque<Instant> d = intentos.get(clave);
        if (d == null) return 0;
        synchronized (d) {
            podar(d, ventana);
            if (d.size() < max) return 0;
            return Math.max(1, Duration.between(Instant.now(reloj), d.peekFirst().plus(ventana)).getSeconds());
        }
    }

    public void registrar(String clave) {
        if (intentos.size() > MAX_CLAVES) intentos.entrySet().removeIf(e -> e.getValue().isEmpty());
        Deque<Instant> d = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) { d.addLast(Instant.now(reloj)); if (d.size() > 1000) d.pollFirst(); }
    }

    /** Registra y responde si con este intento se supera el máximo (de forma atómica para la clave). */
    public boolean registrarYExcede(String clave, int max, Duration ventana) {
        Deque<Instant> d = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) {
            podar(d, ventana);
            if (d.size() >= max) return true;
            d.addLast(Instant.now(reloj));
            return false;
        }
    }

    public void limpiar(String clave) { intentos.remove(clave); }

    private void podar(Deque<Instant> d, Duration ventana) {
        Instant limite = Instant.now(reloj).minus(ventana);
        while (!d.isEmpty() && d.peekFirst().isBefore(limite)) d.pollFirst();
    }
}
