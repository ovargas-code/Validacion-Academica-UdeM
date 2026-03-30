package co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "audit_events",
    indexes = [
        Index(name = "idx_audit_performed_by", columnList = "performed_by"),
        Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        Index(name = "idx_audit_action", columnList = "action")
    ]
)
class AuditEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var action: AuditAction = AuditAction.CREATE_STUDENT,

    @Column(name = "performed_by", nullable = false, length = 100)
    var performedBy: String = "",

    @Column(name = "target_document", length = 20)
    var targetDocument: String? = null,

    @Column(length = 500)
    var details: String? = null,

    @Column(nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now()
)
