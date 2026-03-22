package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import java.time.LocalDateTime

data class CertificateVerificationResponse(
    val valid: Boolean,
    val studentName: String,
    val program: String,
    val degreeTitle: String?,
    val graduationDate: String?,
    val issuedAt: LocalDateTime
)