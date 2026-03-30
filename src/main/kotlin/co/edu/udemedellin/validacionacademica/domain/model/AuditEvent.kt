package co.edu.udemedellin.validacionacademica.domain.model

import java.time.LocalDateTime

data class AuditEvent(
    val id: Long? = null,
    val action: AuditAction,
    val performedBy: String,
    val targetDocument: String? = null,
    val details: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
