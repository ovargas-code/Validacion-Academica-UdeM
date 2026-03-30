package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "El nombre de usuario es obligatorio")
    @field:Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
    val username: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(max = 128, message = "La contraseña no puede superar los 128 caracteres")
    val password: String
)
