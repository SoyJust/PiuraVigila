package pe.edu.utp.piuravigila.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: las reglas de 60 s y de «por día» se prueban con un reloj fijo. */
@Configuration
public class RelojConfig {
    @Bean
    public Clock reloj(@Value("${piuravigila.zona-horaria:America/Lima}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
