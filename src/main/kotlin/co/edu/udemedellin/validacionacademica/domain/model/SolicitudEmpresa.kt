package co.edu.udemedellin.validacionacademica.domain.model

import java.time.LocalDateTime

/**
 * Modelo de dominio para una solicitud de verificación académica iniciada por una empresa o entidad externa.
 *
 * Flujo de estados: [EstadoSolicitud.PENDIENTE] → [EstadoSolicitud.EN_REVISION] →
 * [EstadoSolicitud.APROBADA] | [EstadoSolicitud.RECHAZADA]
 */
data class SolicitudEmpresa(
    val id: Long? = null,

    /** Número único de radicado en formato SOL-YYYYMMDD-NNNNNN. Generado por el sistema. */
    val numeroSolicitud: String,

    // ── Datos de la empresa solicitante ──────────────────────────────────────

    val nombreEmpresa: String,
    val nitEmpresa: String,
    val nombreContacto: String,
    val cargoContacto: String,
    val correoContacto: String,
    val telefonoContacto: String? = null,

    // ── Datos del estudiante a verificar ─────────────────────────────────────

    val tipoDocumentoEstudiante: TipoDocumento,
    val documentoEstudiante: String,
    val nombreEstudiante: String,
    /** Programa académico sobre el que se solicita la verificación. */
    val programaConsultado: String? = null,

    // ── Detalle de la solicitud ───────────────────────────────────────────────

    val tipoValidacion: ValidationType,
    val observaciones: String? = null,
    /** Indica que la empresa aceptó los términos de uso y política de datos. Requerido = true. */
    val aceptaTerminos: Boolean,

    // ── Archivo adjunto ───────────────────────────────────────────────────────

    /** Ruta relativa al directorio de uploads donde se almacenó la carta de autorización en PDF. */
    val rutaCarta: String,

    // ── Trazabilidad ─────────────────────────────────────────────────────────

    val estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null
)
