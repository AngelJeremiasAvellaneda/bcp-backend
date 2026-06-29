package pe.bancoconfianza.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import pe.bancoconfianza.backend.config.DotEnvInitializer;

@SpringBootApplication
@EnableAsync
public class HomeBankingApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(HomeBankingApplication.class);
        app.addInitializers(new DotEnvInitializer());
        app.run(args);
    }
}
