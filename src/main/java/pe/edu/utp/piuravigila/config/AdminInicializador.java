package pe.edu.utp.piuravigila.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.edu.utp.piuravigila.dao.UsuarioMunicipalDAO;
import pe.edu.utp.piuravigila.service.HashContrasena;

/**
 * El seed (APF2) deja el hash del usuario «admin» como HASH_PENDIENTE_DE_IMPLEMENTACION.
 * Si hay una contraseña inicial en la variable PIURA_ADMIN_PASSWORD, se guarda su hash BCrypt. Nada va en el código.
 */
@Component
public class AdminInicializador implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdminInicializador.class);
    private final UsuarioMunicipalDAO usuarios;
    private final HashContrasena hash;
    private final String claveInicial;

    public AdminInicializador(UsuarioMunicipalDAO usuarios, HashContrasena hash,
                              @Value("${piuravigila.admin.password-inicial:}") String claveInicial) {
        this.usuarios = usuarios; this.hash = hash; this.claveInicial = claveInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (claveInicial == null || claveInicial.isBlank()) return;
        usuarios.buscarPorUsuario("admin")
                .filter(u -> u.contrasenaHash().startsWith("HASH_PENDIENTE"))
                .ifPresent(u -> { usuarios.actualizarHash("admin", hash.codificar(claveInicial));
                                  LOG.info("Hash inicial del usuario admin guardado."); });
    }
}
