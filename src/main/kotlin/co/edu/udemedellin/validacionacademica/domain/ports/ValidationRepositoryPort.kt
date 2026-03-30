package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest

/**
 * Puerto de salida para la persistencia de [ValidationRequest].
 *
 * Cada solicitud de validación tiene un [ValidationRequest.verificationCode] único
 * que sirve como identificador público del certificado emitido.
 */
interface ValidationRepositoryPort {

    /**
     * Persiste una solicitud de validación y retorna la instancia guardada con su ID asignado.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException si ya existe una solicitud con el mismo [ValidationRequest.verificationCode].
     */
    fun save(request: ValidationRequest): ValidationRequest

    /**
     * Busca una solicitud de validación por su ID interno.
     *
     * @return la solicitud encontrada, o `null` si no existe ningún registro con ese ID.
     */
    fun findById(id: Long): ValidationRequest?

    /**
     * Busca una solicitud de validación por su código de verificación único (QR / URL pública).
     *
     * @return la solicitud encontrada, o `null` si el código no corresponde a ningún certificado emitido.
     */
    fun findByVerificationCode(code: String): ValidationRequest?
}
