package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.*
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val OTP_EXPIRY_MINUTES = 10L

@Service
class InitiateValidationUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val emailVerificationRepositoryPort: EmailVerificationRepositoryPort,
    private val mailPort: MailPort,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(InitiateValidationUseCase::class.java)
    private val random = SecureRandom()

    @Transactional
    fun execute(request: ValidationRequest): InitiateValidationResult {
        val generatedCode = "UDEM-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val savedRequest = validationRepositoryPort.save(request.copy(verificationCode = generatedCode))
        val requestId = savedRequest.id
            ?: return InitiateValidationResult(
                token = null, status = "ERROR",
                message = "Error interno al procesar la solicitud.", controlCode = "", maskedEmail = null
            )
        val student = studentRepositoryPort.findByDocument(savedRequest.studentDocument)
        val result = buildResult(requestId, savedRequest.validationType, student)

        meterRegistry.counter(
            "validations.initiated",
            "type", savedRequest.validationType.name,
            "status", result.status.name
        ).increment()

        if (result.status != ValidationStatus.VALID) {
            return InitiateValidationResult(
                token = null, status = result.status.name,
                message = result.message, controlCode = result.controlCode, maskedEmail = null
            )
        }

        val otp = String.format("%06d", random.nextInt(1_000_000))
        val sessionToken = UUID.randomUUID().toString()

        emailVerificationRepositoryPort.save(
            EmailVerification(
                token = sessionToken,
                email = savedRequest.requesterEmail,
                code = otp,
                validationRequestId = savedRequest.id,
                studentDocument = savedRequest.studentDocument,
                validationType = savedRequest.validationType,
                requesterName = savedRequest.requesterName,
                expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
            )
        )

        try {
            mailPort.enviarCodigoVerificacion(
                emailDestino = savedRequest.requesterEmail,
                nombreSolicitante = savedRequest.requesterName,
                codigo = otp,
                expiresInMinutes = OTP_EXPIRY_MINUTES
            )
            log.info("OTP enviado a {} para solicitud {}", savedRequest.requesterEmail, savedRequest.verificationCode)
        } catch (e: Exception) {
            log.error("Error enviando OTP a {}: {}", savedRequest.requesterEmail, e.message, e)
        }

        return InitiateValidationResult(
            token = sessionToken,
            status = result.status.name,
            message = result.message,
            controlCode = result.controlCode,
            maskedEmail = maskEmail(savedRequest.requesterEmail)
        )
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 0) return "****"
        val local = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        return "${local[0]}***$domain"
    }

    private fun buildResult(requestId: Long, validationType: ValidationType, student: Student?): ValidationResult {
        val controlCode = "VAL-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
        if (student == null) return ValidationResult(requestId, ValidationStatus.NOT_FOUND, controlCode,
            "No se encontró información académica asociada al documento consultado.")
        return when (validationType) {
            ValidationType.DEGREE ->
                if (student.status == StudentStatus.GRADUADO)
                    ValidationResult(requestId, ValidationStatus.VALID, controlCode, "Se valida que la persona sí obtuvo el título registrado.")
                else
                    ValidationResult(requestId, ValidationStatus.REQUIRES_REVIEW, controlCode, "La persona existe, pero no figura como graduada.")
            ValidationType.ENROLLMENT ->
                if (student.status == StudentStatus.ACTIVO)
                    ValidationResult(requestId, ValidationStatus.VALID, controlCode, "Se valida que la persona se encuentra con matrícula activa.")
                else
                    ValidationResult(requestId, ValidationStatus.REQUIRES_REVIEW, controlCode, "La persona existe, pero no registra matrícula activa.")
        }
    }
}

data class InitiateValidationResult(
    val token: String?,
    val status: String,
    val message: String,
    val controlCode: String,
    val maskedEmail: String?
)
