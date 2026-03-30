package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.AuditEvent
import co.edu.udemedellin.validacionacademica.domain.ports.AuditRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SaveAuditEventUseCase(
    private val auditRepositoryPort: AuditRepositoryPort
) {
    private val log = LoggerFactory.getLogger(SaveAuditEventUseCase::class.java)

    fun execute(
        action: AuditAction,
        performedBy: String,
        targetDocument: String? = null,
        details: String? = null
    ) {
        try {
            auditRepositoryPort.save(
                AuditEvent(
                    action = action,
                    performedBy = performedBy,
                    targetDocument = targetDocument,
                    details = details
                )
            )
        } catch (e: Exception) {
            // La auditoría no debe interrumpir la operación principal
            log.error("Error al guardar evento de auditoría [{} / {}]: {}", action, performedBy, e.message)
        }
    }
}
