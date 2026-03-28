package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.EmailVerification

interface EmailVerificationRepositoryPort {
    fun save(verification: EmailVerification): EmailVerification
    fun findByToken(token: String): EmailVerification?
    fun markAsUsed(id: Long)
}
