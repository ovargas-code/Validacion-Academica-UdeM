package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.ListAuditEventsUseCase
import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.AuditEventResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/audit")
@Tag(name = "Auditoría", description = "Historial de acciones realizadas por administradores")
@SecurityRequirement(name = "bearerAuth")
class AuditController(
    private val listAuditEventsUseCase: ListAuditEventsUseCase
) {

    @GetMapping
    @Operation(
        summary = "Consultar historial de auditoría",
        description = """Retorna las últimas acciones administrativas registradas en el sistema.
Filtros opcionales: performedBy (usuario), action (CREATE_STUDENT | UPDATE_STUDENT | DELETE_STUDENT | IMPORT_STUDENTS).
El parámetro limit acepta entre 1 y 500 (por defecto 100)."""
    )
    fun list(
        @RequestParam(required = false) performedBy: String?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<AuditEventResponse>> {
        val events = listAuditEventsUseCase.execute(
            performedBy = performedBy,
            action = action,
            limit = limit
        )
        return ResponseEntity.ok(events.map { it.toResponse() })
    }
}
