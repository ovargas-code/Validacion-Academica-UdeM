package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

data class ValidationResponseDto(
    val requestId: Long?,
    val status: String,
    val controlCode: String,
    val message: String,
    val letter: String
)
