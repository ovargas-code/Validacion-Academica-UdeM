package co.edu.udemedellin.validacionacademica.infrastructure.persistence.adapter

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.AuditEvent
import co.edu.udemedellin.validacionacademica.domain.ports.AuditRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.AuditEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.AuditJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class AuditPersistenceAdapter(
    private val auditJpaRepository: AuditJpaRepository
) : AuditRepositoryPort {

    override fun save(event: AuditEvent): AuditEvent {
        val saved = auditJpaRepository.save(event.toEntity())
        return saved.toDomain()
    }

    override fun findRecent(performedBy: String?, action: AuditAction?, limit: Int): List<AuditEvent> =
        auditJpaRepository
            .findFiltered(performedBy, action, PageRequest.of(0, limit))
            .map { it.toDomain() }

    private fun AuditEvent.toEntity() = AuditEntity(
        id = id,
        action = action,
        performedBy = performedBy,
        targetDocument = targetDocument,
        details = details,
        timestamp = timestamp
    )

    private fun AuditEntity.toDomain() = AuditEvent(
        id = id,
        action = action,
        performedBy = performedBy,
        targetDocument = targetDocument,
        details = details,
        timestamp = timestamp
    )
}
