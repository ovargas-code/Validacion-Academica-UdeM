package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest

interface ValidationRepositoryPort {
    fun save(request: ValidationRequest): ValidationRequest
    fun findById(id: Long): ValidationRequest?

    // Agrega esta línea:
    fun findByVerificationCode(code: String): ValidationRequest?
}
