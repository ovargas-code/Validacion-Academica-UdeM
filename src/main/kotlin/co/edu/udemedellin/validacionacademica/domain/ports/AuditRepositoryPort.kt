package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.AuditEvent

interface AuditRepositoryPort {
    fun save(event: AuditEvent): AuditEvent
    fun findRecent(performedBy: String?, action: AuditAction?, limit: Int): List<AuditEvent>
}
