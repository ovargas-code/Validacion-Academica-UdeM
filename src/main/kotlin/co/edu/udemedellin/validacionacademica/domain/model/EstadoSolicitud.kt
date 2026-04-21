package co.edu.udemedellin.validacionacademica.domain.model

enum class EstadoSolicitud {
    /** Recibida y pendiente de revisión por el equipo académico. */
    PENDIENTE,
    /** En proceso de verificación interna. */
    EN_REVISION,
    /** Verificación completada con resultado positivo. */
    APROBADA,
    /** Verificación completada con resultado negativo o solicitud inválida. */
    RECHAZADA
}