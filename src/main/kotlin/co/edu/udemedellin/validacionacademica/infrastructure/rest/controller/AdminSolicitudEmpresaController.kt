package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.AprobarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.MarcarEnRevisionUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.RechazarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.RevisionAdminRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.SolicitudEmpresaResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints protegidos (ROLE_ADMIN + JWT) para la revisión de solicitudes de validación de empresa.
 *
 * La seguridad viene del matcher `/api/v1/admin/...` definido en SecurityConfig — no es necesario
 * añadir reglas adicionales.
 */
@RestController
@RequestMapping("/api/v1/admin/solicitudes-empresa")
@Tag(name = "Admin — Solicitudes Empresa", description = "Revisión y resolución de solicitudes de verificación por empresa")
@SecurityRequirement(name = "bearerAuth")
class AdminSolicitudEmpresaController(
    private val marcarEnRevisionUseCase: MarcarEnRevisionUseCase,
    private val aprobarSolicitudEmpresaUseCase: AprobarSolicitudEmpresaUseCase,
    private val rechazarSolicitudEmpresaUseCase: RechazarSolicitudEmpresaUseCase
) {
    private val log = LoggerFactory.getLogger(AdminSolicitudEmpresaController::class.java)

    @PostMapping("/{numero}/marcar-en-revision")
    @Operation(
        summary = "Marcar solicitud en revisión",
        description = "Transición PENDIENTE → EN_REVISION. Notifica al contacto por correo. " +
                "HTTP 409 si el estado actual no permite la transición."
    )
    fun marcarEnRevision(
        @PathVariable numero: String,
        @RequestBody(required = false) request: RevisionAdminRequest?,
        authentication: Authentication
    ): ResponseEntity<SolicitudEmpresaResponse> {
        log.info("Admin '{}' marca en revisión: {}", authentication.name, numero)
        val solicitud = marcarEnRevisionUseCase.execute(
            numeroSolicitud = numero,
            comentarioAdmin = request?.comentarioAdmin,
            adminUsername = authentication.name
        )
        return ResponseEntity.ok(solicitud.toResponse())
    }

    @PostMapping("/{numero}/aprobar")
    @Operation(
        summary = "Aprobar solicitud",
        description = "Transición PENDIENTE o EN_REVISION → APROBADA. Genera el certificado PDF del estudiante " +
                "y lo envía adjunto al contacto por correo. HTTP 409 si el estado no permite la transición " +
                "o si el estudiante no existe en el sistema."
    )
    fun aprobar(
        @PathVariable numero: String,
        @RequestBody(required = false) request: RevisionAdminRequest?,
        authentication: Authentication
    ): ResponseEntity<SolicitudEmpresaResponse> {
        log.info("Admin '{}' aprueba solicitud: {}", authentication.name, numero)
        val solicitud = aprobarSolicitudEmpresaUseCase.execute(
            numeroSolicitud = numero,
            comentarioAdmin = request?.comentarioAdmin,
            adminUsername = authentication.name
        )
        return ResponseEntity.ok(solicitud.toResponse())
    }

    @PostMapping("/{numero}/rechazar")
    @Operation(
        summary = "Rechazar solicitud",
        description = "Transición PENDIENTE o EN_REVISION → RECHAZADA. El campo `comentarioAdmin` es **obligatorio** " +
                "(HTTP 400 si se omite o está vacío). Notifica al contacto con el motivo del rechazo. " +
                "HTTP 409 si el estado no permite la transición."
    )
    fun rechazar(
        @PathVariable numero: String,
        @RequestBody(required = false) request: RevisionAdminRequest?,
        authentication: Authentication
    ): ResponseEntity<SolicitudEmpresaResponse> {
        log.info("Admin '{}' rechaza solicitud: {}", authentication.name, numero)
        val solicitud = rechazarSolicitudEmpresaUseCase.execute(
            numeroSolicitud = numero,
            comentarioAdmin = request?.comentarioAdmin,
            adminUsername = authentication.name
        )
        return ResponseEntity.ok(solicitud.toResponse())
    }
}
