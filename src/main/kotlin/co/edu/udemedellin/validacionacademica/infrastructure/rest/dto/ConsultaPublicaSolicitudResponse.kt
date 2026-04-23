package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import java.time.LocalDateTime

/**
 * DTO reducido para la consulta pública de estado de una solicitud de empresa.
 *
 * Expone únicamente los campos necesarios para que el solicitante confirme
 * su radicado y conozca el estado actual. Campos sensibles (NIT, contacto,
 * documento del estudiante, adminResponsable, etc.) quedan excluidos.
 *
 * [motivoRechazo] solo está presente cuando [estado] == RECHAZADA;
 * en cualquier otro estado su valor es null.
 */
data class ConsultaPublicaSolicitudResponse(
    val numeroSolicitud: String,
    val estado: EstadoSolicitud,
    val createdAt: LocalDateTime,
    val fechaRevision: LocalDateTime?,
    val nombreEmpresa: String,
    val nombreEstudiante: String,
    val motivoRechazo: String?
)

/**
 * Mapea un modelo de dominio al DTO público.
 *
 * Lógica de privacidad:
 * - [motivoRechazo] expone [SolicitudEmpresa.comentarioAdmin] SOLO cuando el estado es RECHAZADA.
 * - [SolicitudEmpresa.adminResponsable] se omite siempre (identidad del funcionario).
 * - Todos los demás campos sensibles (NIT, contacto, documento, etc.) se omiten siempre.
 */
fun SolicitudEmpresa.toPublicResponse() = ConsultaPublicaSolicitudResponse(
    numeroSolicitud = numeroSolicitud,
    estado = estado,
    createdAt = createdAt,
    fechaRevision = fechaRevision,
    nombreEmpresa = nombreEmpresa,
    nombreEstudiante = nombreEstudiante,
    motivoRechazo = if (estado == EstadoSolicitud.RECHAZADA) comentarioAdmin else null
)
