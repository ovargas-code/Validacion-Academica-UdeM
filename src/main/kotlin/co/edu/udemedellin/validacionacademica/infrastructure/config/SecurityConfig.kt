package co.edu.udemedellin.validacionacademica.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
open class SecurityConfig {

    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Desactivamos CSRF porque la API REST usa tokens, no cookies de sesión
            .csrf { it.disable() }
            // Permitimos que los frames de H2 Console funcionen en el navegador
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .authorizeHttpRequests { auth ->
                // --- RUTAS PÚBLICAS (no requieren login) ---

                // Swagger UI y documentación OpenAPI
                auth.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()

                // API de estudiantes
                auth.requestMatchers("/api/v1/students/**").permitAll()

                // API de validaciones
                auth.requestMatchers("/api/validations/**").permitAll()

                // API de verificaciones de certificados
                auth.requestMatchers("/api/v1/verificaciones/**").permitAll()

                // Portal web de consulta pública
                auth.requestMatchers("/", "/verificar").permitAll()

                // H2 Console (solo en desarrollo - no habilitar en producción)
                auth.requestMatchers("/h2-console/**").permitAll()

                // --- RUTAS PROTEGIDAS (requieren login) ---
                auth.anyRequest().authenticated()
            }
            .formLogin { it.permitAll() }

        return http.build()
    }
}
