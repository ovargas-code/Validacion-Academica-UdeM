package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateValidationRequestDto(
    @field:NotBlank
    val requesterName: String,
    @field:Email
    @field:NotBlank
    val requesterEmail: String,
    @field:NotBlank
    val studentDocument: String,
    val validationType: ValidationType
)
