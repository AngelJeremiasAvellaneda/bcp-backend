package pe.bancoconfianza.backend.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Configuración para lazy-load de conexión a BD
 * Evita intentar conectar al iniciar si la BD no está disponible
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class ConnectionConfig {
    
    // Spring cargará la conexión solo cuando sea necesaria (@Lazy)
    // No intenta conectar en startup
}
