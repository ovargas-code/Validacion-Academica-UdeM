package co.edu.udemedellin.validacionacademica.domain.model

import java.time.LocalDateTime

data class EmailVerification(
    val id: Long? = null,
    val token: String,
    val email: String,
    val code: String,
    val validationRequestId: Long,
    val studentDocument: String,
    val validationType: ValidationType,
    val requesterName: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val used: Boolean = false
)
