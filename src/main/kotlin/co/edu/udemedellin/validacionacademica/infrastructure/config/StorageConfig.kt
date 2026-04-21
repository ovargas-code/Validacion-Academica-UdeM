package co.edu.udemedellin.validacionacademica.infrastructure.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Registra [StorageProperties] en el contexto de Spring.
 * Separado de [SecurityConfig] para mantener la cohesión de cada clase de configuración.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig
