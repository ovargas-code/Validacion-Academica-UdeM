package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.InvalidStateTransitionException
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RechazarSolicitudEmpresaUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort,
    private val mailPort: MailPort,
    private val saveAuditEventUseCase: SaveAuditEventUseCase
) {
    private val log = LoggerFactory.getLogger(RechazarSolicitudEmpresaUseCase::class.java)

    @Transactional
    fun execute(numeroSolicitud: String, comentarioAdmin: String?, adminUsername: String): SolicitudEmpresa {
        if (comentarioAdmin.isNullOrBlank()) {
            throw IllegalArgumentException("El comentario del administrador es obligatorio para rechazar una solicitud.")
        }

        val solicitud = solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
            ?: throw NoSuchElementException("Solicitud no encontrada: $numeroSolicitud")

        val estadoAnterior = solicitud.estado
        if (estadoAnterior == EstadoSolicitud.APROBADA || estadoAnterior == EstadoSolicitud.RECHAZADA) {
            throw InvalidStateTransitionException(
                "No se puede rechazar: la solicitud está en estado $estadoAnterior (estado final)."
            )
        }

        val actualizada = solicitudEmpresaRepositoryPort.update(
            solicitud.copy(
                estado = EstadoSolicitud.RECHAZADA,
                comentarioAdmin = comentarioAdmin,
                fechaRevision = LocalDateTime.now(),
                adminResponsable = adminUsername,
                updatedAt = LocalDateTime.now()
            )
        )

        try {
            mailPort.enviarNotificacionRechazada(
                emailDestino = solicitud.correoContacto,
                nombreContacto = solicitud.nombreContacto,
                numeroSolicitud = numeroSolicitud,
                comentarioAdmin = comentarioAdmin
            )
        } catch (e: Exception) {
            log.error("Error enviando notificación RECHAZADA para {}: {}", numeroSolicitud, e.message, e)
        }

        saveAuditEventUseCase.execute(
            action = AuditAction.SOLICITUD_EMPRESA_REVISADA,
            performedBy = adminUsername,
            targetDocument = numeroSolicitud,
            details = buildAuditDetails(estadoAnterior, EstadoSolicitud.RECHAZADA, comentarioAdmin)
        )

        return actualizada
    }

    private fun buildAuditDetails(anterior: EstadoSolicitud, nuevo: EstadoSolicitud, comentario: String?): String =
        """{"estadoAnterior":"$anterior","estadoNuevo":"$nuevo","comentario":${comentario?.let { "\"$it\"" } ?: "null"}}"""
}
