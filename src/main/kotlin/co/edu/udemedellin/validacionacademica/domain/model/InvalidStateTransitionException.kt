package co.edu.udemedellin.validacionacademica.domain.model

/**
 * Excepción de dominio lanzada cuando se intenta realizar una transición de estado inválida.
 *
 * Ejemplo: intentar aprobar una solicitud que ya está APROBADA o RECHAZADA.
 * Se mapea a HTTP 409 Conflict en el [GlobalExceptionHandler].
 */
class InvalidStateTransitionException(message: String) : RuntimeException(message)
