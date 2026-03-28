package co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity

import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification_codes")
class EmailVerificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 36)
    var token: String = "",

    @Column(nullable = false, length = 254)
    var email: String = "",

    @Column(nullable = false, length = 6)
    var code: String = "",

    @Column(nullable = false)
    var validationRequestId: Long = 0,

    @Column(nullable = false, length = 20)
    var studentDocument: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var validationType: ValidationType = ValidationType.DEGREE,

    @Column(nullable = false, length = 150)
    var requesterName: String = "",

    @Column(nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var used: Boolean = false
)
