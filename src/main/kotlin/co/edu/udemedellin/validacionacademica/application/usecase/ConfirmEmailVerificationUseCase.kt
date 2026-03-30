package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ConfirmEmailVerificationUseCase(
    private val emailVerificationRepositoryPort: EmailVerificationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    private val mailPort: MailPort,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(ConfirmEmailVerificationUseCase::class.java)

    private val certificateTimer = Timer.builder("certificates.generation.duration")
        .description("Tiempo de generación del certificado PDF")
        .publishPercentiles(0.5, 0.95, 0.99)

    @Transactional
    fun execute(token: String, code: String): ConfirmResult {
        val verification = emailVerificationRepositoryPort.findByToken(token)
            ?: return otpFailure("TOKEN_NOT_FOUND", "El token de verificación no es válido.")

        if (verification.used)
            return otpFailure("TOKEN_ALREADY_USED", "Este código ya fue utilizado. Inicia una nueva solicitud.")

        if (LocalDateTime.now().isAfter(verification.expiresAt))
            return otpFailure("TOKEN_EXPIRED", "El código de verificación ha expirado. Inicia una nueva solicitud.")

        if (verification.code != code)
            return otpFailure("INVALID_CODE", "El código ingresado es incorrecto.")

        meterRegistry.counter("otp.verifications", "result", "success").increment()
        val verificationId = verification.id
            ?: return ConfirmResult.Error("INTERNAL_ERROR", "Error interno procesando la verificación.")
        emailVerificationRepositoryPort.markAsUsed(verificationId)

        val student = studentRepositoryPort.findByDocument(verification.studentDocument)
            ?: return ConfirmResult.Error("STUDENT_NOT_FOUND", "No se encontró el estudiante asociado.")

        val validationRequest = validationRepositoryPort.findById(verification.validationRequestId)
            ?: return ConfirmResult.Error("VALIDATION_NOT_FOUND", "No se encontró la solicitud de validación.")

        val pdfBytes = try {
            certificateTimer.register(meterRegistry).recordCallable {
                pdfGeneratorPort.generateCertificate(
                    studentName = student.fullName,
                    studentDocument = student.document,
                    program = student.program,
                    verificationCode = validationRequest.verificationCode
                )
            } ?: run {
                log.error("El generador de PDF retornó null para {}", validationRequest.verificationCode)
                meterRegistry.counter("certificates.issued", "result", "error").increment()
                return ConfirmResult.Error("PDF_ERROR", "Error generando el certificado PDF.")
            }
        } catch (e: Exception) {
            log.error("Error generando PDF para {}", validationRequest.verificationCode, e)
            meterRegistry.counter("certificates.issued", "result", "error").increment()
            return ConfirmResult.Error("PDF_ERROR", "Error generando el certificado PDF.")
        }

        meterRegistry.counter("certificates.issued", "result", "success").increment()

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

    private fun otpFailure(code: String, message: String): ConfirmResult.Error {
        meterRegistry.counter("otp.verifications", "result", "failure", "reason", code).increment()
        return ConfirmResult.Error(code, message)
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
