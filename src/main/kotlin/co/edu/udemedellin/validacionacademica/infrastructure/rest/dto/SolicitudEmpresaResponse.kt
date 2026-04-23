package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.model.TipoDocumento
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import java.time.LocalDateTime

data class SolicitudEmpresaResponse(
    val numeroSolicitud: String,
    val estado: EstadoSolicitud,
    val createdAt: LocalDateTime,

    // ── Empresa solicitante ───────────────────────────────────────────────────
    val nombreEmpresa: String,
    val nitEmpresa: String,
    val nombreContacto: String,
    val cargoContacto: String,
    val correoContacto: String,
    val telefonoContacto: String?,

    // ── Estudiante a verificar ────────────────────────────────────────────────
    val tipoDocumentoEstudiante: TipoDocumento,
    val documentoEstudiante: String,
    val nombreEstudiante: String,
    val programaConsultado: String?,

    // ── Detalle de la solicitud ───────────────────────────────────────────────
    val tipoValidacion: ValidationType,
    val observaciones: String?,
    val aceptaTerminos: Boolean,

    // ── Revisión administrativa (null mientras esté PENDIENTE) ────────────────
    val comentarioAdmin: String?,
    val fechaRevision: LocalDateTime?,
    val adminResponsable: String?
    // Nota: rutaCarta se omite intencionalmente — es una ruta interna del servidor.
)

fun SolicitudEmpresa.toResponse() = SolicitudEmpresaResponse(
    numeroSolicitud = numeroSolicitud,
    estado = estado,
    createdAt = createdAt,
    nombreEmpresa = nombreEmpresa,
    nitEmpresa = nitEmpresa,
    nombreContacto = nombreContacto,
    cargoContacto = cargoContacto,
    correoContacto = correoContacto,
    telefonoContacto = telefonoContacto,
    tipoDocumentoEstudiante = tipoDocumentoEstudiante,
    documentoEstudiante = documentoEstudiante,
    nombreEstudiante = nombreEstudiante,
    programaConsultado = programaConsultado,
    tipoValidacion = tipoValidacion,
    observaciones = observaciones,
    aceptaTerminos = aceptaTerminos,
    comentarioAdmin = comentarioAdmin,
    fechaRevision = fechaRevision,
    adminResponsable = adminResponsable
)
