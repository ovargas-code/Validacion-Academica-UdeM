package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.GenerateCertificatePdfUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.VerifyCertificateUseCase
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CertificateVerificationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/verificaciones")
@Tag(name = "Verificaciones", description = "Verificar autenticidad de certificados emitidos")
@Validated
class VerificationController(
    private val verificationUseCase: VerifyCertificateUseCase,
    private val generateCertificatePdfUseCase: GenerateCertificatePdfUseCase
) {
    private val log = LoggerFactory.getLogger(VerificationController::class.java)

    @GetMapping("/{code}")
    @Operation(
        summary = "Verificar certificado (JSON)",
        description = "Dado un código de verificación (ej: UDEM-ABC12345), retorna los datos del estudiante y confirma si el certificado es auténtico."
    )
    fun verify(
        @Parameter(description = "Código de verificación del certificado", example = "UDEM-ABC12345")
        @PathVariable
        @Pattern(regexp = "^[A-Z0-9\\-]{6,30}$", message = "Código de verificación con formato inválido")
        code: String
    ): ResponseEntity<CertificateVerificationResponse> {
        log.info("Verificar certificado: código={}", code)
        val info = verificationUseCase.verify(code)
            ?: return ResponseEntity.notFound().build()

        val response = CertificateVerificationResponse(
            valid = info.valid,
            studentName = info.studentName,
            program = info.program,
            degreeTitle = info.degreeTitle,
            graduationDate = info.graduationDate,
            issuedAt = info.issuedAt
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{code}/pdf")
    @Operation(
        summary = "Descargar certificado (PDF)",
        description = "Descarga el certificado en formato PDF dado su código de verificación."
    )
    fun downloadPdf(
        @Parameter(description = "Código de verificación del certificado", example = "UDEM-ABC12345")
        @PathVariable
        @Pattern(regexp = "^[A-Z0-9\\-]{6,30}$", message = "Código de verificación con formato inválido")
        code: String
    ): ResponseEntity<ByteArray> {
        log.info("Descargar PDF certificado: código={}", code)
        val pdfBytes = generateCertificatePdfUseCase.execute(code)
            ?: return ResponseEntity.notFound().build()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "Certificado_$code.pdf")

        return ResponseEntity.ok().headers(headers).body(pdfBytes)
    }
}
