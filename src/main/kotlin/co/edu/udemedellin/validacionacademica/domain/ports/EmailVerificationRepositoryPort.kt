package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.EmailVerification

/**
 * Puerto de salida para la persistencia de [EmailVerification].
 *
 * Gestiona los códigos OTP de un solo uso enviados al correo del solicitante.
 * Un token solo puede ser consumido una vez: una vez marcado como usado, no puede
 * volver a verificarse aunque el código sea correcto.
 */
interface EmailVerificationRepositoryPort {

    /**
     * Persiste un nuevo código de verificación OTP y retorna la instancia guardada con su ID asignado.
     */
    fun save(verification: EmailVerification): EmailVerification

    /**
     * Busca un código de verificación por su token de sesión.
     *
     * @return la verificación encontrada (usada o no), o `null` si el token no existe.
     */
    fun findByToken(token: String): EmailVerification?

    /**
     * Marca el código de verificación como utilizado, impidiendo su reutilización.
     *
     * Debe llamarse únicamente después de validar correctamente el OTP para garantizar
     * que el token no pueda emplearse en un segundo intento.
     *
     * @param id ID interno del registro de verificación.
     */
    fun markAsUsed(id: Long)
}
