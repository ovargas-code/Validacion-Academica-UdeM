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
class MarcarEnRevisionUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort,
    private val mailPort: MailPort,
    private val saveAuditEventUseCase: SaveAuditEventUseCase
) {
    private val log = LoggerFactory.getLogger(MarcarEnRevisionUseCase::class.java)

    @Transactional
    fun execute(numeroSolicitud: String, comentarioAdmin: String?, adminUsername: String): SolicitudEmpresa {
        val solicitud = solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
            ?: throw NoSuchElementException("Solicitud no encontrada: $numeroSolicitud")

        val estadoAnterior = solicitud.estado
        if (estadoAnterior != EstadoSolicitud.PENDIENTE) {
            throw InvalidStateTransitionException(
                "No se puede marcar en revisión: la solicitud está en estado $estadoAnterior. " +
                "Solo se permite la transición desde PENDIENTE."
            )
        }

        val actualizada = solicitudEmpresaRepositoryPort.update(
            solicitud.copy(
                estado = EstadoSolicitud.EN_REVISION,
                comentarioAdmin = comentarioAdmin,
                fechaRevision = LocalDateTime.now(),
                adminResponsable = adminUsername,
                updatedAt = LocalDateTime.now()
            )
        )

        try {
            mailPort.enviarNotificacionEnRevision(
                emailDestino = solicitud.correoContacto,
                nombreContacto = solicitud.nombreContacto,
                numeroSolicitud = numeroSolicitud
            )
        } catch (e: Exception) {
            log.error("Error enviando notificación EN_REVISION para {}: {}", numeroSolicitud, e.message, e)
        }

        saveAuditEventUseCase.execute(
            action = AuditAction.SOLICITUD_EMPRESA_REVISADA,
            performedBy = adminUsername,
            targetDocument = numeroSolicitud,
            details = buildAuditDetails(estadoAnterior, EstadoSolicitud.EN_REVISION, comentarioAdmin)
        )

        return actualizada
    }

    private fun buildAuditDetails(anterior: EstadoSolicitud, nuevo: EstadoSolicitud, comentario: String?): String =
        """{"estadoAnterior":"$anterior","estadoNuevo":"$nuevo","comentario":${comentario?.let { "\"$it\"" } ?: "null"}}"""
}
