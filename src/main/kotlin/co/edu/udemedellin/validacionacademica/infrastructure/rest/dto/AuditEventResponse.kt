package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.AuditEvent
import java.time.LocalDateTime

data class AuditEventResponse(
    val id: Long?,
    val action: AuditAction,
    val performedBy: String,
    val targetDocument: String?,
    val details: String?,
    val timestamp: LocalDateTime
)

fun AuditEvent.toResponse() = AuditEventResponse(
    id = id,
    action = action,
    performedBy = performedBy,
    targetDocument = targetDocument,
    details = details,
    timestamp = timestamp
)
