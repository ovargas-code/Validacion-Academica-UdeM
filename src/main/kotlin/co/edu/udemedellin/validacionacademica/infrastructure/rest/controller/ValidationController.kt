package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.CreateValidationUseCase
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.model.ValidationStatus
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CreateValidationRequestDto
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.ValidationResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    private val createValidationUseCase: CreateValidationUseCase
) {

    @PostMapping("/verify", produces = [MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Verificar y generar certificado",
        description = "Recibe una solicitud de validación, verifica el estado académico del estudiante y " +
                "devuelve un certificado PDF si la validación es VÁLIDA, o un JSON con el resultado si no lo es. " +
                "Cuando el resultado es VÁLIDO, también envía el certificado al correo del solicitante."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Validación exitosa — se devuelve el certificado PDF"),
        ApiResponse(
            responseCode = "422", description = "Validación no exitosa (estudiante no encontrado o sin estado válido)",
            content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ValidationResponseDto::class))]
        )
    )
    fun verify(
        @Valid @RequestBody request: CreateValidationRequestDto
    ): ResponseEntity<*> {

        val domainRequest = ValidationRequest(
            requesterName = request.requesterName,
            requesterEmail = request.requesterEmail,
            studentDocument = request.studentDocument,
            validationType = request.validationType
        )

        val response = createValidationUseCase.execute(domainRequest)

        if (response.result.status != ValidationStatus.VALID || response.pdfBytes == null) {
            val dto = ValidationResponseDto(
                requestId = response.request.id,
                status = response.result.status.name,
                controlCode = response.result.controlCode,
                message = response.result.message,
                letter = response.letter
            )
            return ResponseEntity.unprocessableEntity()
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        val filename = "Certificado_UDEM_${response.request.studentDocument}.pdf"
        headers.setContentDispositionFormData("attachment", filename)

        return ResponseEntity.ok()
            .headers(headers)
            .body(response.pdfBytes)
    }
}
