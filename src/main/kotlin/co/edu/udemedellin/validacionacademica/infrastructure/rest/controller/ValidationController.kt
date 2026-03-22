package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.CreateValidationUseCase
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CreateValidationRequestDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/validations")
@Tag(name = "Validaciones", description = "Solicitar validación académica de un estudiante")
class ValidationController(
    private val createValidationUseCase: CreateValidationUseCase,
    private val pdfGeneratorPort: PdfGeneratorPort
) {

    @PostMapping("/verify", produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(
        summary = "Verificar y generar certificado",
        description = "Recibe una solicitud de validación, verifica el estado académico del estudiante y devuelve un certificado PDF por descarga directa. También envía el certificado al correo del solicitante."
    )
    fun verify(
        // @Valid activa las validaciones @NotBlank y @Email del DTO
        @Valid @RequestBody request: CreateValidationRequestDto
    ): ResponseEntity<ByteArray> {

        // Convertimos el DTO de entrada al modelo de dominio
        val domainRequest = ValidationRequest(
            requesterName = request.requesterName,
            requesterEmail = request.requesterEmail,
            studentDocument = request.studentDocument,
            validationType = request.validationType
        )

        // Ejecutamos el caso de uso (guarda en BD y envía correo)
        val response = createValidationUseCase.execute(domainRequest)

        // Obtenemos el programa real del estudiante (ya no hardcodeado)
        val estudiante = response.student
        val programaReal = estudiante?.program ?: "Programa no registrado"
        val nombreRealEstudiante = estudiante?.fullName ?: response.request.requesterName

        // Generamos el PDF con los datos correctos
        val pdfBytes = pdfGeneratorPort.generateCertificate(
            studentName = nombreRealEstudiante,
            studentDocument = response.request.studentDocument,
            program = programaReal,
            verificationCode = response.request.verificationCode
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        val filename = "Certificado_UDEM_${response.request.studentDocument}.pdf"
        headers.setContentDispositionFormData("attachment", filename)

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes)
    }
}
