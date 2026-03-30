package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.AuditEvent
import co.edu.udemedellin.validacionacademica.domain.ports.AuditRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListAuditEventsUseCase(
    private val auditRepositoryPort: AuditRepositoryPort
) {
    @Transactional(readOnly = true)
    fun execute(
        performedBy: String? = null,
        action: AuditAction? = null,
        limit: Int = 100
    ): List<AuditEvent> =
        auditRepositoryPort.findRecent(
            performedBy = performedBy,
            action = action,
            limit = limit.coerceIn(1, 500)
        )
}
