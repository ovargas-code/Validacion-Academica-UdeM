package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CertificateVerificationResponse
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class VerifyCertificateUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort
) {
    fun verify(code: String): CertificateVerificationResponse? {
        // 1. Buscamos la validación usando el código
        val validation = validationRepositoryPort.findByVerificationCode(code) ?: return null

        // 2. Buscamos al estudiante asociado
        val student = studentRepositoryPort.findByDocument(validation.studentDocument) ?: return null

        // 3. Formateamos la fecha de graduación si existe
        val graduationFormatted = student.graduationDate
            ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ?: "No registrada"

        // 4. Armamos la respuesta con datos REALES del estudiante (sin hardcodear)
        return CertificateVerificationResponse(
            valid = true,
            studentName = student.fullName,
            program = student.program,              // Programa real del estudiante
            degreeTitle = student.degreeTitle,      // Título real (puede ser null)
            graduationDate = graduationFormatted,   // Fecha formateada o "No registrada"
            issuedAt = validation.createdAt
        )
    }
}
