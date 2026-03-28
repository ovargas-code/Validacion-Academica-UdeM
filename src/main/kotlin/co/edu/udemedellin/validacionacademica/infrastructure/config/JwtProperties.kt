package co.edu.udemedellin.validacionacademica.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    /** Clave secreta HMAC-SHA256. Mínimo 32 caracteres. Configura vía JWT_SECRET en producción. */
    val secret: String = "changeme-replace-in-production-min32chars!",
    /** Tiempo de expiración del token en milisegundos (default: 1 hora) */
    val expirationMs: Long = 3_600_000L
) {
    init {
        require(secret.length >= 32) {
            "app.security.jwt.secret debe tener al menos 32 caracteres. " +
            "Genera uno seguro con: openssl rand -base64 48"
        }
    }
}
