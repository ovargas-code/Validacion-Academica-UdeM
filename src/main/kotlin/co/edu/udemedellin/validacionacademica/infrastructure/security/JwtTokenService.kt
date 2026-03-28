package co.edu.udemedellin.validacionacademica.infrastructure.security

import co.edu.udemedellin.validacionacademica.infrastructure.config.JwtProperties
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class JwtTokenService(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties
) {
    fun generateToken(authentication: Authentication): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer("validacion-academica-ms")
            .issuedAt(now)
            .expiresAt(now.plusMillis(jwtProperties.expirationMs))
            .subject(authentication.name)
            .claim("roles", authentication.authorities.map { it.authority })
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
