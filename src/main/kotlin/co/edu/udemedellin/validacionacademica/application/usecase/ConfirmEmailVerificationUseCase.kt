package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ConfirmEmailVerificationUseCase(
    private val emailVerificationRepositoryPort: EmailVerificationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    private val mailPort: MailPort
) {
    private val log = LoggerFactory.getLogger(ConfirmEmailVerificationUseCase::class.java)

    fun execute(token: String, code: String): ConfirmResult {
        val verification = emailVerificationRepositoryPort.findByToken(token)
            ?: return ConfirmResult.Error("TOKEN_NOT_FOUND", "El token de verificación no es válido.")

        if (verification.used)
            return ConfirmResult.Error("TOKEN_ALREADY_USED", "Este código ya fue utilizado. Inicia una nueva solicitud.")

        if (LocalDateTime.now().isAfter(verification.expiresAt))
            return ConfirmResult.Error("TOKEN_EXPIRED", "El código de verificación ha expirado. Inicia una nueva solicitud.")

        if (verification.code != code)
            return ConfirmResult.Error("INVALID_CODE", "El código ingresado es incorrecto.")

        emailVerificationRepositoryPort.markAsUsed(verification.id!!)

        val student = studentRepositoryPort.findByDocument(verification.studentDocument)
            ?: return ConfirmResult.Error("STUDENT_NOT_FOUND", "No se encontró el estudiante asociado.")

        val validationRequest = validationRepositoryPort.findById(verification.validationRequestId)
            ?: return ConfirmResult.Error("VALIDATION_NOT_FOUND", "No se encontró la solicitud de validación.")

        val pdfBytes = try {
            pdfGeneratorPort.generateCertificate(
                studentName = student.fullName,
                studentDocument = student.document,
                program = student.program,
                verificationCode = validationRequest.verificationCode
            )
        } catch (e: Exception) {
            log.error("Error generando PDF para {}", validationRequest.verificationCode, e)
            return ConfirmResult.Error("PDF_ERROR", "Error generando el certificado PDF.")
        }

        try {
            mailPort.enviarCertificado(
                emailDestino = verification.email,
                nombreEstudiante = student.fullName,
                pdfBytes = pdfBytes
            )
            log.info("Certificado enviado a {} tras verificación de correo exitosa", verification.email)
        } catch (e: Exception) {
            log.error("No fue posible enviar el certificado a {}: {}", verification.email, e.message, e)
            // No falla: retorna el PDF de todas formas para descarga
        }

        return ConfirmResult.Success(
            pdfBytes = pdfBytes,
            studentName = student.fullName,
            email = verification.email,
            verificationCode = validationRequest.verificationCode
        )
    }
}

sealed class ConfirmResult {
    data class Success(
        val pdfBytes: ByteArray,
        val studentName: String,
        val email: String,
        val verificationCode: String
    ) : ConfirmResult()

    data class Error(val code: String, val message: String) : ConfirmResult()
}
