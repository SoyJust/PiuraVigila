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
 * El seed (APF2) deja el hash de los usuarios «admin» y «operador» como HASH_PENDIENTE_DE_IMPLEMENTACION.
 * Si hay una contraseña inicial en PIURA_ADMIN_PASSWORD o PIURA_OPERADOR_PASSWORD, se guarda su hash BCrypt
 * (cada variable inicializa solo a su usuario). Nada va en el código.
 */
@Component
public class AdminInicializador implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdminInicializador.class);
    private final UsuarioMunicipalDAO usuarios;
    private final HashContrasena hash;
    private final String claveAdmin;
    private final String claveOperador;

    public AdminInicializador(UsuarioMunicipalDAO usuarios, HashContrasena hash,
                              @Value("${piuravigila.admin.password-inicial:}") String claveAdmin,
                              @Value("${piuravigila.operador.password-inicial:}") String claveOperador) {
        this.usuarios = usuarios; this.hash = hash; this.claveAdmin = claveAdmin; this.claveOperador = claveOperador;
    }

    @Override
    public void run(ApplicationArguments args) {
        inicializar("admin", claveAdmin);
        inicializar("operador", claveOperador);
    }

    private void inicializar(String usuario, String claveInicial) {
        if (claveInicial == null || claveInicial.isBlank()) return;
        usuarios.buscarPorUsuario(usuario)
                .filter(u -> u.contrasenaHash().startsWith("HASH_PENDIENTE"))
                .ifPresent(u -> { usuarios.actualizarHash(usuario, hash.codificar(claveInicial));
                                  LOG.info("Hash inicial del usuario {} guardado.", usuario); });
    }
}
