package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.infrastructure.config.JwtProperties
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.LoginRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.LoginResponse
import co.edu.udemedellin.validacionacademica.infrastructure.security.JwtTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Obtener token JWT para acceso a rutas protegidas")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenService: JwtTokenService,
    private val jwtProperties: JwtProperties
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Autentica con usuario y contraseña. " +
                "Retorna un JWT Bearer token válido para llamar a endpoints protegidos (ej: POST /api/v1/students). " +
                "Incluir en requests posteriores como: Authorization: Bearer <token>"
    )
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        log.info("Intento de login para usuario: {}", request.username)
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        val token = jwtTokenService.generateToken(authentication)
        log.info("Login exitoso para usuario: {}", request.username)
        return ResponseEntity.ok(
            LoginResponse(
                accessToken = token,
                tokenType = "Bearer",
                expiresIn = jwtProperties.expirationMs / 1000
            )
        )
    }
}
