package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GenerateCertificatePdfUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    @Value("\${app.base-url:http://localhost:8080}")
    private val baseUrl: String = "http://localhost:8080"
) {
    @Transactional(readOnly = true)
    fun execute(code: String): ByteArray? {
        val validation = validationRepositoryPort.findByVerificationCode(code) ?: return null
        val student = studentRepositoryPort.findByDocument(validation.studentDocument)
        val programa = student?.program ?: "Programa no registrado"
        val verificationUrl = "$baseUrl/api/v1/verificaciones/${validation.verificationCode}"

        return pdfGeneratorPort.generateCertificate(
            studentName = student?.fullName ?: "Nombre no disponible",
            studentDocument = validation.studentDocument,
            program = programa,
            verificationUrl = verificationUrl
        )
    }
}
