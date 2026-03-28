package co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository

import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.EmailVerificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface EmailVerificationJpaRepository : JpaRepository<EmailVerificationEntity, Long> {
    fun findByToken(token: String): EmailVerificationEntity?

    @Modifying
    @Transactional
    @Query("UPDATE EmailVerificationEntity e SET e.used = true WHERE e.id = :id")
    fun markAsUsed(id: Long)
}
