package pe.edu.utp.piuravigila.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.Properties;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Semana 15 · configuración de despliegue (TC-DEP). */
class DespliegueTest {

    private static Properties cargar(String recurso) throws Exception {
        Properties p = new Properties();
        try (InputStream in = DespliegueTest.class.getClassLoader().getResourceAsStream(recurso)) {
            assertNotNull(in, recurso + " debe existir");
            p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test @DisplayName("TC-DEP-01 · solo health e info quedan expuestos y health no muestra detalles")
    void actuator_minimo() throws Exception {
        Properties p = cargar("application.properties");
        assertEquals("health,info", p.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("never", p.getProperty("management.endpoint.health.show-details"));
        assertEquals("db", p.getProperty("management.endpoint.health.group.db.include"));
    }

    @Test @DisplayName("TC-DEP-02 · el perfil prod exige HTTPS, cookie de sesión Secure y toma los secretos de variables de entorno")
    void perfil_prod() throws Exception {
        Properties p = cargar("application-prod.properties");
        assertEquals("true", p.getProperty("server.ssl.enabled"));
        assertEquals("true", p.getProperty("server.servlet.session.cookie.secure"));
        assertEquals("true", p.getProperty("spring.thymeleaf.cache"));
        assertTrue(p.getProperty("server.ssl.key-store").startsWith("${"), "la ruta del almacén llega por variable");
        assertTrue(p.getProperty("server.ssl.key-store-password").startsWith("${"), "la clave del almacén llega por variable");
    }

    @Test @DisplayName("TC-DEP-03 · con TLS, toda la aplicación exige transporte confidencial (HTTP redirige a HTTPS)")
    void restriccion_confidencial() {
        StandardContext contexto = new StandardContext();
        RedireccionHttps.exigirConfidencial(contexto);
        SecurityConstraint[] restricciones = contexto.findConstraints();
        assertEquals(1, restricciones.length);
        assertEquals("CONFIDENTIAL", restricciones[0].getUserConstraint());
        assertTrue(restricciones[0].findCollections()[0].findPattern("/*"));
    }
}
