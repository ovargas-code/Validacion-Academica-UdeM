package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.ConsultarEstadoPublicoSolicitudUseCase
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.ConsultaPublicaSolicitudResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toPublicResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint público para consulta de estado de solicitudes de empresa.
 *
 * Solo expone un subconjunto reducido de campos a través de
 * [ConsultaPublicaSolicitudResponse]. No requiere autenticación.
 */
@RestController
@RequestMapping("/api/v1/public/solicitudes-empresa")
@Tag(name = "Consulta Pública — Solicitudes Empresa", description = "Estado público de una solicitud de verificación académica por empresa")
@Validated
class ConsultaPublicaSolicitudController(
    private val consultarEstadoPublicoSolicitudUseCase: ConsultarEstadoPublicoSolicitudUseCase
) {
    private val log = LoggerFactory.getLogger(ConsultaPublicaSolicitudController::class.java)

    @GetMapping("/{numero}/estado")
    @Operation(
        summary = "Consultar estado público de solicitud",
        description = """Retorna el estado actual de una solicitud de verificación académica por empresa.

Solo expone los campos necesarios para que el solicitante identifique su radicado:
número, estado, fechas, nombre de empresa y estudiante.
El motivo de rechazo se incluye únicamente cuando el estado es `RECHAZADA`.

No requiere autenticación. Sujeto a rate limiting."""
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
        ApiResponse(
            responseCode = "400",
            description = "Formato de número de solicitud inválido (debe ser SOL-YYYYMMDD-NNNNNN)",
            content = [Content(schema = Schema(hidden = true))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "No existe solicitud con ese número de radicado",
            content = [Content(schema = Schema(hidden = true))]
        )
    )
    fun consultarEstado(
        @Parameter(description = "Número de radicado", example = "SOL-20260422-000001")
        @PathVariable
        @Pattern(
            regexp = "^SOL-\\d{8}-\\d{6}$",
            message = "El número de solicitud debe tener el formato SOL-YYYYMMDD-NNNNNN"
        )
        numero: String
    ): ResponseEntity<ConsultaPublicaSolicitudResponse> {
        log.debug("Consulta pública: {}", numero)
        val solicitud = consultarEstadoPublicoSolicitudUseCase.execute(numero)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(solicitud.toPublicResponse())
    }
}
