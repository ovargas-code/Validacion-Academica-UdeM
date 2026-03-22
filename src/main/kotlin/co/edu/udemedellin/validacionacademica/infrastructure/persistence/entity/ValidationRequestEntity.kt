package co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity

import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "validation_requests")
class ValidationRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var requesterName: String = "",
    @Column(nullable = false)
    var requesterEmail: String = "",
    @Column(nullable = false)
    var studentDocument: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var validationType: ValidationType = ValidationType.DEGREE,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(), // <-- ¡No olvides poner esta coma!

    @Column(nullable = false, unique = true)
    var verificationCode: String = ""

)
