package pe.edu.utp.piuravigila.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * Semana 15: con TLS activo (perfil prod) se abre además un conector HTTP que solo redirige a HTTPS.
 * Toda la aplicación exige transporte confidencial, así que ninguna página se sirve en claro.
 */
@Component
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class RedireccionHttps implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    private final int puertoHttp;
    private final int puertoHttps;

    public RedireccionHttps(@Value("${piuravigila.http.puerto:8080}") int puertoHttp,
                            @Value("${server.port:8443}") int puertoHttps) {
        this.puertoHttp = puertoHttp;
        this.puertoHttps = puertoHttps;
    }

    @Override
    public void customize(TomcatServletWebServerFactory fabrica) {
        Connector http = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        http.setScheme("http");
        http.setPort(puertoHttp);
        http.setSecure(false);
        http.setRedirectPort(puertoHttps);
        fabrica.addAdditionalTomcatConnectors(http);
        fabrica.addContextCustomizers(RedireccionHttps::exigirConfidencial);
    }

    static void exigirConfidencial(Context contexto) {
        SecurityConstraint restriccion = new SecurityConstraint();
        restriccion.setUserConstraint("CONFIDENTIAL");
        SecurityCollection todo = new SecurityCollection();
        todo.addPattern("/*");
        restriccion.addCollection(todo);
        contexto.addConstraint(restriccion);
    }
}
