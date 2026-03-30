package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.DocumentGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class CreateValidationUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val documentGeneratorPort: DocumentGeneratorPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    private val mailPort: MailPort
) {
    private val logger = LoggerFactory.getLogger(CreateValidationUseCase::class.java)

    @Transactional
    fun execute(request: ValidationRequest): ValidationExecutionResponse {
        val generatedCode = "UDEM-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val requestWithCode = request.copy(verificationCode = generatedCode)
        val savedRequest = validationRepositoryPort.save(requestWithCode)
        val requestId = savedRequest.id
            ?: throw IllegalStateException("El repositorio no asignó un ID a la solicitud de validación guardada.")
        val student = studentRepositoryPort.findByDocument(savedRequest.studentDocument)
        val result = buildResult(requestId, savedRequest.validationType, student)
        val letter = documentGeneratorPort.generateLetter(savedRequest, student, result)

        var mailResult = MailDeliveryResult.notAttempted("No se envía correo cuando la validación no es válida.")
        var pdfBytes: ByteArray? = null

        if (student != null && result.status == ValidationStatus.VALID) {
            try {
                pdfBytes = pdfGeneratorPort.generateCertificate(
                    studentName = student.fullName,
                    studentDocument = student.document,
                    program = student.program,
                    verificationCode = savedRequest.verificationCode
                )
            } catch (e: Exception) {
                logger.error("Error generando PDF para el código {}", savedRequest.verificationCode, e)
            }

            mailResult = if (pdfBytes != null) {
                try {
                    mailPort.enviarCertificado(
                        emailDestino = savedRequest.requesterEmail,
                        nombreEstudiante = student.fullName,
                        pdfBytes = pdfBytes
                    )
                    logger.info(
                        "Certificado enviado a {} con código de verificación {}",
                        savedRequest.requesterEmail,
                        savedRequest.verificationCode
                    )
                    MailDeliveryResult.sent(savedRequest.requesterEmail)
                } catch (e: Exception) {
                    logger.error(
                        "No fue posible enviar el certificado a {}. Revise la configuración SMTP y el archivo .env",
                        savedRequest.requesterEmail,
                        e
                    )
                    MailDeliveryResult.failed(
                        savedRequest.requesterEmail,
                        e.message ?: "Error SMTP no especificado"
                    )
                }
            } else {
                MailDeliveryResult.failed(savedRequest.requesterEmail, "No se pudo generar el PDF del certificado")
            }
        }

        return ValidationExecutionResponse(
            request = savedRequest,
            result = result,
            letter = letter,
            student = student,
            mailResult = mailResult,
            pdfBytes = pdfBytes
        )
    }

    private fun buildResult(
        requestId: Long,
        validationType: ValidationType,
        student: Student?
    ): ValidationResult {
        val controlCode = "VAL-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"

        if (student == null) {
            return ValidationResult(
                requestId = requestId,
                status = ValidationStatus.NOT_FOUND,
                controlCode = controlCode,
                message = "No se encontró información académica asociada al documento consultado."
            )
        }

        return when (validationType) {
            ValidationType.DEGREE -> {
                if (student.status == StudentStatus.GRADUADO) {
                    ValidationResult(
                        requestId,
                        ValidationStatus.VALID,
                        controlCode,
                        "Se valida que la persona sí obtuvo el título registrado."
                    )
                } else {
                    ValidationResult(
                        requestId,
                        ValidationStatus.REQUIRES_REVIEW,
                        controlCode,
                        "La persona existe, pero no figura como graduada."
                    )
                }
            }

            ValidationType.ENROLLMENT -> {
                if (student.status == StudentStatus.ACTIVO) {
                    ValidationResult(
                        requestId,
                        ValidationStatus.VALID,
                        controlCode,
                        "Se valida que la persona se encuentra con matrícula activa."
                    )
                } else {
                    ValidationResult(
                        requestId,
                        ValidationStatus.REQUIRES_REVIEW,
                        controlCode,
                        "La persona existe, pero no registra matrícula activa."
                    )
                }
            }
        }
    }
}

data class ValidationExecutionResponse(
    val request: ValidationRequest,
    val result: ValidationResult,
    val letter: String,
    val student: Student?,
    val mailResult: MailDeliveryResult,
    val pdfBytes: ByteArray? = null
)

data class MailDeliveryResult(
    val attempted: Boolean,
    val sent: Boolean,
    val destination: String?,
    val message: String
) {
    companion object {
        fun sent(destination: String) = MailDeliveryResult(
            attempted = true,
            sent = true,
            destination = destination,
            message = "Certificado enviado correctamente a $destination."
        )

        fun failed(destination: String, reason: String) = MailDeliveryResult(
            attempted = true,
            sent = false,
            destination = destination,
            message = "No fue posible enviar el certificado a $destination. Motivo: $reason"
        )

        fun notAttempted(reason: String) = MailDeliveryResult(
            attempted = false,
            sent = false,
            destination = null,
            message = reason
        )
    }
}
