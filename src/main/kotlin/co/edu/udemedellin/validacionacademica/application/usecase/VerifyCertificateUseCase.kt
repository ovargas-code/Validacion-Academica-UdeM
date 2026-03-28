package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CertificateInfo(
    val valid: Boolean,
    val studentName: String,
    val program: String,
    val degreeTitle: String?,
    val graduationDate: String?,
    val issuedAt: LocalDateTime
)

@Service
class VerifyCertificateUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort
) {
    fun verify(code: String): CertificateInfo? {
        val validation = validationRepositoryPort.findByVerificationCode(code) ?: return null
        val student = studentRepositoryPort.findByDocument(validation.studentDocument) ?: return null

        val graduationFormatted = student.graduationDate
            ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ?: "No registrada"

        val valid = when (validation.validationType) {
            ValidationType.DEGREE -> student.status == StudentStatus.GRADUATED
            ValidationType.ENROLLMENT -> student.status == StudentStatus.ACTIVE
        }

        return CertificateInfo(
            valid = valid,
            studentName = student.fullName,
            program = student.program,
            degreeTitle = student.degreeTitle,
            graduationDate = graduationFormatted,
            issuedAt = validation.createdAt
        )
    }
}
