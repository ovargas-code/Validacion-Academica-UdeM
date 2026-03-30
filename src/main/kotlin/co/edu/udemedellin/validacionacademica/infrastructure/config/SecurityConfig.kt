package co.edu.udemedellin.validacionacademica.infrastructure.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class, RateLimitProperties::class)
open class SecurityConfig(
    private val jwtProperties: JwtProperties,
    private val securityProperties: SecurityProperties,
    @org.springframework.beans.factory.annotation.Value("\${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private val corsAllowedOrigins: List<String>
) {

    // ─── JWT Encoder / Decoder (HMAC-SHA256) ──────────────────────────────────

    @Bean
    open fun jwtDecoder(): JwtDecoder {
        val key = SecretKeySpec(jwtProperties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(key).build()
    }

    @Bean
    open fun jwtEncoder(): JwtEncoder {
        val key = SecretKeySpec(jwtProperties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val jwk = OctetSequenceKey.Builder(key).build()
        val source = ImmutableJWKSet<com.nimbusds.jose.proc.SecurityContext>(JWKSet(jwk))
        return NimbusJwtEncoder(source)
    }

    /**
     * Lee el claim "roles" del JWT y lo convierte en GrantedAuthority sin prefijo adicional.
     * Así ROLE_ADMIN en el token → hasAuthority("ROLE_ADMIN") en las reglas de seguridad.
     */
    @Bean
    open fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val authoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("roles")
            setAuthorityPrefix("")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        }
    }

    // ─── UserDetailsService ───────────────────────────────────────────────────
    // Se define explícitamente porque al registrar JwtDecoder, la auto-configuración
    // de UserDetailsServiceAutoConfiguration hace back-off.

    @Bean
    open fun userDetailsService(): UserDetailsService {
        val userProps = securityProperties.user
        val encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        val user = User.withUsername(userProps.name)
            .password(userProps.password)
            .roles(*userProps.roles.toTypedArray())
            .passwordEncoder(encoder::encode)
            .build()
        return InMemoryUserDetailsManager(user)
    }

    // ─── AuthenticationManager ────────────────────────────────────────────────

    @Bean
    open fun authenticationManager(): AuthenticationManager {
        val provider = DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService())
            setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder())
        }
        return ProviderManager(provider)
    }

    // ─── H2 Console: excluida de Spring Security (solo desarrollo) ────────────

    @Bean
    open fun webSecurityCustomizer(): org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer {
        return org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer { web ->
            web.ignoring().requestMatchers(AntPathRequestMatcher("/h2-console/**"))
        }
    }

    // ─── Security Filter Chain ────────────────────────────────────────────────

    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .headers { headers ->
                headers.frameOptions { fo -> fo.sameOrigin() }
                headers.contentTypeOptions { }
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(31_536_000)
                    hsts.preload(true)
                }
                headers.referrerPolicy { rp ->
                    rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                }
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                        "img-src 'self' data:; " +
                        "script-src 'self'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "object-src 'none'; " +
                        "frame-ancestors 'self'"
                    )
                }
                headers.permissionsPolicy { pp ->
                    pp.policy("camera=(), microphone=(), geolocation=(), payment=()")
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->

                // --- PÚBLICAS ---
                auth.requestMatchers(
                    "/api/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/prometheus", // Prometheus scrapes sin JWT; restringir por IP en producción
                    "/",
                    "/verificar"
                ).permitAll()

                // Validaciones públicas (protegidas por RateLimitFilter)
                auth.requestMatchers("/api/validations/**").permitAll()

                // Verificación de certificados — pública
                auth.requestMatchers("/api/v1/verificaciones/**").permitAll()

                // Gestión de estudiantes y auditoría — solo ADMIN con JWT
                auth.requestMatchers(HttpMethod.POST, "/api/v1/students/**").hasAuthority("ROLE_ADMIN")
                auth.requestMatchers(HttpMethod.GET, "/api/v1/students/**").hasAuthority("ROLE_ADMIN")
                auth.requestMatchers(HttpMethod.PUT, "/api/v1/students/**").hasAuthority("ROLE_ADMIN")
                auth.requestMatchers(HttpMethod.DELETE, "/api/v1/students/**").hasAuthority("ROLE_ADMIN")
                auth.requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
            }

        return http.build()
    }

    // ─── CORS ─────────────────────────────────────────────────────────────────

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            // Orígenes permitidos configurables vía app.security.cors.allowed-origins
            // En producción definir via variable de entorno CORS_ALLOWED_ORIGINS
            allowedOrigins = corsAllowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
