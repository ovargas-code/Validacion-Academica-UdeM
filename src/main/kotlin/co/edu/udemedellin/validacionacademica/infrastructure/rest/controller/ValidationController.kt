package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.*
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.ConfirmVerificationRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CreateValidationRequestDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/validations")
@Tag(name = "Validaciones", description = "Flujo de validación académica con verificación por correo")
class ValidationController(
    private val initiateValidationUseCase: InitiateValidationUseCase,
    private val confirmEmailVerificationUseCase: ConfirmEmailVerificationUseCase
) {
    private val log = LoggerFactory.getLogger(ValidationController::class.java)

    @PostMapping("/initiate")
    @Operation(
        summary = "Paso 1 — Iniciar validación",
        description = "Verifica el estado académico del estudiante. Si es VÁLIDO, envía un código OTP de 6 dígitos " +
                "al correo del solicitante y retorna un token de sesión para usar en el Paso 2. " +
                "Si no es VÁLIDO, retorna el resultado directamente sin enviar código."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Validación iniciada. Campo 'token' presente solo cuando status=VALID.")
    )
    fun initiate(@Valid @RequestBody request: CreateValidationRequestDto): ResponseEntity<InitiateValidationResult> {
        log.info("Iniciar validación: tipo={}, doc={}", request.validationType, request.studentDocument)
        val domainRequest = ValidationRequest(
            requesterName = request.requesterName,
            requesterEmail = request.requesterEmail,
            studentDocument = request.studentDocument,
            validationType = request.validationType
        )
        val result = initiateValidationUseCase.execute(domainRequest)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/confirm", produces = [MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Paso 2 — Confirmar correo y descargar certificado",
        description = "Valida el código OTP recibido por correo. Si es correcto y no ha expirado, " +
                "genera el certificado PDF, lo envía al correo verificado y lo retorna para descarga inmediata."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Código válido — retorna el certificado PDF"),
        ApiResponse(
            responseCode = "422", description = "Código incorrecto, expirado o ya utilizado",
            content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
        )
    )
    fun confirm(@Valid @RequestBody request: ConfirmVerificationRequest): ResponseEntity<*> {
        log.info("Confirmar verificación OTP: token={}", request.token)
        return when (val result = confirmEmailVerificationUseCase.execute(request.token, request.code)) {
            is ConfirmResult.Success -> {
                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_PDF
                    setContentDispositionFormData("attachment", "Certificado_${result.verificationCode}.pdf")
                }
                ResponseEntity.ok().headers(headers).body(result.pdfBytes)
            }
            is ConfirmResult.Error -> {
                val status = when (result.code) {
                    "TOKEN_EXPIRED", "TOKEN_ALREADY_USED" -> HttpStatus.GONE
                    "INVALID_CODE" -> HttpStatus.UNPROCESSABLE_ENTITY
                    else -> HttpStatus.BAD_REQUEST
                }
                ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("code" to result.code, "message" to result.message))
            }
        }
    }
}
