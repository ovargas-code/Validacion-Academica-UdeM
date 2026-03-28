package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "El nombre de usuario es obligatorio")
    val username: String,
    @field:NotBlank(message = "La contraseña es obligatoria")
    val password: String
)
