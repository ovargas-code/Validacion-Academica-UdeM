package co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository

import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.ValidationRequestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ValidationRequestJpaRepository : JpaRepository<ValidationRequestEntity, Long> {

    // Agrega esta línea:
    fun findByVerificationCode(verificationCode: String): ValidationRequestEntity?

}
