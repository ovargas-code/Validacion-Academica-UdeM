package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.InvalidStateTransitionException
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Genera bajo demanda el certificado final de verificacion academica asociado a
 * una solicitud de empresa aprobada.
 *
 * Este documento de salida no es la carta de autorizacion cargada por el usuario.
 */
@Service
class DescargarCertificadoSolicitudEmpresaUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    @Value("\${app.base-url:http://localhost:8080}")
    private val baseUrl: String = "http://localhost:8080"
) {

    @Transactional(readOnly = true)
    fun execute(numeroSolicitud: String): ByteArray {
        val solicitud = solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
            ?: throw NoSuchElementException("Solicitud no encontrada: $numeroSolicitud")

        if (solicitud.estado != EstadoSolicitud.APROBADA) {
            throw InvalidStateTransitionException(
                "El certificado final solo esta disponible para solicitudes aprobadas."
            )
        }

        val student = studentRepositoryPort.findByDocument(solicitud.documentoEstudiante)
            ?: throw NoSuchElementException(
                "No existe estudiante asociado a la solicitud $numeroSolicitud."
            )

        val verificationUrl = "$baseUrl/api/v1/solicitudes-empresa/$numeroSolicitud"
        return pdfGeneratorPort.generateCertificate(
            studentName = student.fullName,
            studentDocument = student.document,
            program = student.program,
            verificationUrl = verificationUrl
        )
    }
}
