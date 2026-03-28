package co.edu.udemedellin.validacionacademica.infrastructure.persistence.adapter

import co.edu.udemedellin.validacionacademica.domain.model.EmailVerification
import co.edu.udemedellin.validacionacademica.domain.ports.EmailVerificationRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.EmailVerificationEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.EmailVerificationJpaRepository
import org.springframework.stereotype.Component

@Component
class EmailVerificationPersistenceAdapter(
    private val repo: EmailVerificationJpaRepository
) : EmailVerificationRepositoryPort {

    override fun save(verification: EmailVerification): EmailVerification =
        repo.save(verification.toEntity()).toDomain()

    override fun findByToken(token: String): EmailVerification? =
        repo.findByToken(token)?.toDomain()

    override fun markAsUsed(id: Long) = repo.markAsUsed(id)

    private fun EmailVerification.toEntity() = EmailVerificationEntity(
        id = id, token = token, email = email, code = code,
        validationRequestId = validationRequestId, studentDocument = studentDocument,
        validationType = validationType, requesterName = requesterName,
        expiresAt = expiresAt, createdAt = createdAt, used = used
    )

    private fun EmailVerificationEntity.toDomain() = EmailVerification(
        id = id, token = token, email = email, code = code,
        validationRequestId = validationRequestId, studentDocument = studentDocument,
        validationType = validationType, requesterName = requesterName,
        expiresAt = expiresAt, createdAt = createdAt, used = used
    )
}
