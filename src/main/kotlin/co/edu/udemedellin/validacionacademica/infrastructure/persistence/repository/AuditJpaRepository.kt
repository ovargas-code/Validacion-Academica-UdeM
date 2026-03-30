package co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.AuditEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AuditJpaRepository : JpaRepository<AuditEntity, Long> {

    @Query("""
        SELECT a FROM AuditEntity a
        WHERE (:performedBy IS NULL OR a.performedBy = :performedBy)
          AND (:action IS NULL OR a.action = :action)
        ORDER BY a.timestamp DESC
    """)
    fun findFiltered(
        performedBy: String?,
        action: AuditAction?,
        pageable: Pageable
    ): List<AuditEntity>
}
