package pe.edu.utp.piuravigila.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada. Escanea pe.edu.utp.piuravigila.* (controller, service, dao.jdbc, config). */
@SpringBootApplication(scanBasePackages = "pe.edu.utp.piuravigila")
public class PiuraVigilaApplication {
    public static void main(String[] args) { SpringApplication.run(PiuraVigilaApplication.class, args); }
}
