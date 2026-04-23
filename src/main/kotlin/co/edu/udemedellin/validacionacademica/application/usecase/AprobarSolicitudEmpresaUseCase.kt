package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.InvalidStateTransitionException
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AprobarSolicitudEmpresaUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    private val mailPort: MailPort,
    private val saveAuditEventUseCase: SaveAuditEventUseCase,
    @Value("\${app.base-url:http://localhost:8080}")
    private val baseUrl: String = "http://localhost:8080"
) {
    private val log = LoggerFactory.getLogger(AprobarSolicitudEmpresaUseCase::class.java)

    @Transactional
    fun execute(numeroSolicitud: String, comentarioAdmin: String?, adminUsername: String): SolicitudEmpresa {
        val solicitud = solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
            ?: throw NoSuchElementException("Solicitud no encontrada: $numeroSolicitud")

        val estadoAnterior = solicitud.estado
        if (estadoAnterior == EstadoSolicitud.APROBADA || estadoAnterior == EstadoSolicitud.RECHAZADA) {
            throw InvalidStateTransitionException(
                "No se puede aprobar: la solicitud está en estado $estadoAnterior (estado final)."
            )
        }

        val student = studentRepositoryPort.findByDocument(solicitud.documentoEstudiante)
            ?: throw InvalidStateTransitionException(
                "No se puede aprobar: el estudiante referenciado no existe en el sistema."
            )

        val verificationUrl = "$baseUrl/api/v1/solicitudes-empresa/$numeroSolicitud"
        val pdfBytes = pdfGeneratorPort.generateCertificate(
            studentName = student.fullName,
            studentDocument = student.document,
            program = student.program,
            verificationUrl = verificationUrl
        )

        val actualizada = solicitudEmpresaRepositoryPort.update(
            solicitud.copy(
                estado = EstadoSolicitud.APROBADA,
                comentarioAdmin = comentarioAdmin,
                fechaRevision = LocalDateTime.now(),
                adminResponsable = adminUsername,
                updatedAt = LocalDateTime.now()
            )
        )

        try {
            mailPort.enviarNotificacionAprobada(
                emailDestino = solicitud.correoContacto,
                nombreContacto = solicitud.nombreContacto,
                numeroSolicitud = numeroSolicitud,
                pdfBytes = pdfBytes
            )
        } catch (e: Exception) {
            log.error("Error enviando notificación APROBADA para {}: {}", numeroSolicitud, e.message, e)
        }

        saveAuditEventUseCase.execute(
            action = AuditAction.SOLICITUD_EMPRESA_REVISADA,
            performedBy = adminUsername,
            targetDocument = numeroSolicitud,
            details = buildAuditDetails(estadoAnterior, EstadoSolicitud.APROBADA, comentarioAdmin)
        )

        return actualizada
    }

    private fun buildAuditDetails(anterior: EstadoSolicitud, nuevo: EstadoSolicitud, comentario: String?): String =
        """{"estadoAnterior":"$anterior","estadoNuevo":"$nuevo","comentario":${comentario?.let { "\"$it\"" } ?: "null"}}"""
}
