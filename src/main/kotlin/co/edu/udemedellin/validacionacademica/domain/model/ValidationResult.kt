package co.edu.udemedellin.validacionacademica.domain.model

import java.time.LocalDateTime

data class ValidationResult(
    val requestId: Long,
    val status: ValidationStatus,
    val controlCode: String,
    val message: String,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)
