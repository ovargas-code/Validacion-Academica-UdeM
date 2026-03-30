package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ConfirmVerificationRequest(
    @field:NotBlank(message = "El token de sesión es obligatorio")
    @field:Size(max = 50, message = "El token de sesión no puede superar los 50 caracteres")
    val token: String,

    @field:NotBlank(message = "El código de verificación es obligatorio")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "El código debe ser de exactamente 6 dígitos")
    val code: String
)
