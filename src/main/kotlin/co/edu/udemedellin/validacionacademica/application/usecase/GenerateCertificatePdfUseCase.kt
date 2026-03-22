package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import org.springframework.stereotype.Service

@Service
class GenerateCertificatePdfUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort, // <-- Agregamos este puerto
    private val pdfGeneratorPort: PdfGeneratorPort
) {
    fun execute(code: String): ByteArray? {
        // 1. Buscamos la solicitud de validación por código
        val validation = validationRepositoryPort.findByVerificationCode(code) ?: return null

        // 2. Buscamos al estudiante para obtener su programa REAL
        val student = studentRepositoryPort.findByDocument(validation.studentDocument)

        // 3. Usamos el programa real; si por alguna razón no existe, ponemos un valor genérico
        val programa = student?.program ?: "Programa no registrado"

        // 4. Generamos el PDF con todos los datos correctos
        return pdfGeneratorPort.generateCertificate(
            studentName = validation.requesterName,
            studentDocument = validation.studentDocument,
            program = programa,
            verificationCode = validation.verificationCode
        )
    }
}
