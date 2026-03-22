package co.edu.udemedellin.validacionacademica.infrastructure.persistence.adapter

import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.ValidationRequestEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.ValidationRequestJpaRepository
import org.springframework.stereotype.Component

@Component
class ValidationPersistenceAdapter(
    private val validationRequestJpaRepository: ValidationRequestJpaRepository
) : ValidationRepositoryPort {

    override fun save(request: ValidationRequest): ValidationRequest {
        val saved = validationRequestJpaRepository.save(request.toEntity())
        return saved.toDomain()
    }

    override fun findById(id: Long): ValidationRequest? {
        return validationRequestJpaRepository.findById(id)
            .orElse(null)
            ?.toDomain()
    }

    override fun findByVerificationCode(code: String): ValidationRequest? {
        return validationRequestJpaRepository.findByVerificationCode(code)
            ?.toDomain()
    }

    private fun ValidationRequest.toEntity(): ValidationRequestEntity = ValidationRequestEntity(
        id = id,
        requesterName = requesterName,
        requesterEmail = requesterEmail,
        studentDocument = studentDocument,
        validationType = validationType,
        createdAt = createdAt,
        verificationCode = verificationCode // <-- Lo pasamos hacia la base de datos
    )

    private fun ValidationRequestEntity.toDomain(): ValidationRequest = ValidationRequest(
        id = id,
        requesterName = requesterName,
        requesterEmail = requesterEmail,
        studentDocument = studentDocument,
        validationType = validationType,
        createdAt = createdAt,
        verificationCode = verificationCode // <-- Lo recuperamos desde la base de datos
    )
}
