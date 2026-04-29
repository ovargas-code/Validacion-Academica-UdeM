package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.AprobarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.DescargarCertificadoSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.DescargarCartaSolicitudUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.ListarSolicitudesEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.MarcarEnRevisionUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.RechazarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.RevisionAdminRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.SolicitudEmpresaResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
@Validated
class AdminSolicitudEmpresaController(
    private val marcarEnRevisionUseCase: MarcarEnRevisionUseCase,
    private val aprobarSolicitudEmpresaUseCase: AprobarSolicitudEmpresaUseCase,
    private val rechazarSolicitudEmpresaUseCase: RechazarSolicitudEmpresaUseCase,
    private val listarSolicitudesEmpresaUseCase: ListarSolicitudesEmpresaUseCase,
    private val descargarCartaSolicitudUseCase: DescargarCartaSolicitudUseCase,
    private val descargarCertificadoSolicitudEmpresaUseCase: DescargarCertificadoSolicitudEmpresaUseCase
) {
    private val log = LoggerFactory.getLogger(AdminSolicitudEmpresaController::class.java)

    // ── GET /api/v1/admin/solicitudes-empresa ─────────────────────────────────

    @GetMapping
    @Operation(
        summary = "Listar solicitudes de empresa",
        description = """Retorna una página de solicitudes ordenada por fecha de creación (más recientes primero).

**Parámetros opcionales:**
- `estado`: filtro por estado (`PENDIENTE`, `EN_REVISION`, `APROBADA`, `RECHAZADA`). Omitir para ver todas.
- `page`: número de página, base 0 (por defecto 0).
- `size`: registros por página, entre 1 y 100 (por defecto 20)."""
    )
    fun listar(
        @Parameter(description = "Número de página (base 0)", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Tamaño de página (1–100)", example = "20")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @Parameter(description = "Filtro por estado (opcional)")
        @RequestParam(required = false) estado: String?
    ): ResponseEntity<Page<SolicitudEmpresaResponse>> {
        log.info("Listar solicitudes: page={}, size={}, estado={}", page, size, estado)
        val estadoEnum = parseEstado(estado)
        val solicitudes = listarSolicitudesEmpresaUseCase.execute(page, size, estadoEnum)
        return ResponseEntity.ok(solicitudes.map { it.toResponse() })
    }

    // ── GET /api/v1/admin/solicitudes-empresa/{numero}/carta ──────────────────

    @GetMapping("/{numero}/carta")
    @Operation(
        summary = "Descargar carta de autorización",
        description = """Descarga el PDF de la carta de autorización adjunta a la solicitud.

- HTTP 404 si no existe solicitud con ese número.
- HTTP 400 si el archivo no está disponible en el sistema de archivos."""
    )
    fun descargarCarta(
        @Parameter(description = "Número de radicado", example = "SOL-20260423-000001")
        @PathVariable numero: String
    ): ResponseEntity<ByteArray> {
        log.info("Descargar carta: {}", numero)
        val bytes = descargarCartaSolicitudUseCase.execute(numero)
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.attachment()
                .filename("Carta_$numero.pdf")
                .build()
        }
        return ResponseEntity.ok().headers(headers).body(bytes)
    }

    // ── POST — transiciones de estado ─────────────────────────────────────────

    // Descarga del documento final de salida. No depende de carta de autorizacion adjunta.

    @GetMapping("/{numero}/certificado-final")
    @Operation(
        summary = "Descargar certificado final",
        description = """Genera y descarga el certificado final de verificacion academica para una solicitud aprobada.

Este PDF es el resultado del tramite. No es la carta de autorizacion de entrada."""
    )
    fun descargarCertificadoFinal(
        @Parameter(description = "Numero de radicado", example = "SOL-20260423-000001")
        @PathVariable numero: String
    ): ResponseEntity<ByteArray> {
        log.info("Descargar certificado final: {}", numero)
        val certificadoFinal = descargarCertificadoSolicitudEmpresaUseCase.execute(numero)
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.attachment()
                .filename("Certificado_$numero.pdf")
                .build()
        }
        return ResponseEntity.ok().headers(headers).body(certificadoFinal)
    }

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

    // ── Utilidades privadas ───────────────────────────────────────────────────

    private fun parseEstado(estadoStr: String?): EstadoSolicitud? {
        if (estadoStr.isNullOrBlank()) return null
        return runCatching { EstadoSolicitud.valueOf(estadoStr) }.getOrElse {
            val valid = EstadoSolicitud.values().joinToString(", ")
            throw IllegalArgumentException("Estado inválido: '$estadoStr'. Valores válidos: $valid")
        }
    }
}
