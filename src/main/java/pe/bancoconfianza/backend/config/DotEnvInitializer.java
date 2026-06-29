package pe.bancoconfianza.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Initializer que carga variables de entorno desde .env antes de que Spring inicie.
 * Permite usar ${VARIABLE} en application.properties sin tener que exportarlas al SO.
 */
public class DotEnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Intenta cargar .env desde el directorio actual (donde se ejecuta mvnw.cmd o java)
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()  // No falla si .env no existe
                    .load();

            ConfigurableEnvironment env = applicationContext.getEnvironment();

            // Inyecta todas las variables de .env al ambiente de Spring
            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );

            System.out.println("✅ [DotEnv] Variables cargadas desde .env");
        } catch (Exception e) {
            System.err.println("⚠️  [DotEnv] No se pudo cargar .env: " + e.getMessage());
        }
    }
}
