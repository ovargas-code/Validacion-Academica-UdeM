package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

/**
 * Cuerpo de las peticiones de revisión administrativa de solicitudes de empresa.
 *
 * [comentarioAdmin] es opcional para las transiciones EN_REVISION y APROBADA,
 * pero **obligatorio** para RECHAZADA (validado en el use case).
 */
data class RevisionAdminRequest(
    val comentarioAdmin: String? = null
)
