package co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity

import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "validation_requests",
    indexes = [
        Index(name = "idx_validation_student_document", columnList = "student_document"),
        Index(name = "idx_validation_verification_code", columnList = "verification_code", unique = true)
    ]
)
class ValidationRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 150)
    var requesterName: String = "",

    @Column(nullable = false, length = 254)
    var requesterEmail: String = "",

    @Column(name = "student_document", nullable = false, length = 20)
    var studentDocument: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var validationType: ValidationType = ValidationType.DEGREE,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "verification_code", nullable = false, unique = true, length = 20)
    var verificationCode: String = ""
)
