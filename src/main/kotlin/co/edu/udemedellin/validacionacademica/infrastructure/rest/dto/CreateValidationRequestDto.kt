package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateValidationRequestDto(
    @field:NotBlank
    @field:Size(max = 150, message = "El nombre del solicitante no puede superar los 150 caracteres")
    val requesterName: String,

    @field:Email
    @field:NotBlank
    @field:Size(max = 254, message = "El correo electrónico no puede superar los 254 caracteres")
    val requesterEmail: String,

    @field:NotBlank
    @field:Size(min = 1, max = 20, message = "El documento debe tener entre 1 y 20 caracteres")
    @field:Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "El documento solo puede contener letras, números y guiones")
    val studentDocument: String,

    @field:NotNull(message = "El tipo de validación es obligatorio (DEGREE, ENROLLMENT)")
    val validationType: ValidationType
)
