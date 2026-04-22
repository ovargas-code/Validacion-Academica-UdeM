package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.ConsultarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.CrearSolicitudEmpresaCommand
import co.edu.udemedellin.validacionacademica.application.usecase.CrearSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CrearSolicitudEmpresaRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.SolicitudEmpresaResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Endpoints para la gestión de solicitudes de verificación académica iniciadas por empresas externas.
 *
 * ## Contrato multipart para POST /api/v1/solicitudes-empresa
 *
 * El request debe enviarse como `multipart/form-data` con exactamente dos partes:
 *
 * | Parte   | Content-Type       | Descripción                                              |
 * |---------|--------------------|----------------------------------------------------------|
 * | `datos` | `application/json` | JSON con los campos de [CrearSolicitudEmpresaRequest]    |
 * | `carta` | `application/pdf`  | PDF de la carta de autorización firmada (máx. 10 MB)    |
 *
 * Ejemplo con Fetch API:
 * ```js
 * const formData = new FormData()
 * formData.append("datos", new Blob([JSON.stringify(datos)], { type: "application/json" }))
 * formData.append("carta", archivoPdf)
 * fetch("/api/v1/solicitudes-empresa", { method: "POST", body: formData })
 * ```
 *
 * ## Nota sobre atomicidad
 * El PDF se escribe en disco antes de persistir el registro en BD. Si la transacción de BD
 * falla (ej: número de solicitud duplicado por concurrencia), el archivo quedará huérfano en
 * `uploads/`. Esto es un trade-off aceptado; en el futuro considerar limpieza periódica o
 * almacenamiento transaccional (MinIO + outbox pattern).
 */
@RestController
@RequestMapping("/api/v1/solicitudes-empresa")
@Tag(name = "Solicitudes Empresa", description = "Verificación académica solicitada por empresas o entidades externas")
@Validated
class SolicitudEmpresaController(
    private val crearSolicitudEmpresaUseCase: CrearSolicitudEmpresaUseCase,
    private val consultarSolicitudEmpresaUseCase: ConsultarSolicitudEmpresaUseCase
) {
    private val log = LoggerFactory.getLogger(SolicitudEmpresaController::class.java)

    // ── POST /api/v1/solicitudes-empresa ─────────────────────────────────────

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Crear solicitud de verificación",
        description = """Registra una nueva solicitud de verificación académica por parte de una empresa o entidad externa.

**Partes del multipart:**
- `datos` (application/json): campos del formulario según el esquema de CrearSolicitudEmpresaRequest.
- `carta` (application/pdf): carta de autorización firmada en formato PDF (máximo 10 MB).

La respuesta incluye el **número de radicado** (`numeroSolicitud`) en formato `SOL-YYYYMMDD-NNNNNN`."""
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Solicitud creada. El campo `numeroSolicitud` identifica el radicado."),
        ApiResponse(responseCode = "400", description = "Datos inválidos, archivo faltante o no es PDF",
            content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]),
        ApiResponse(responseCode = "413", description = "El archivo supera el tamaño máximo permitido (10 MB)",
            content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)])
    )
    fun crear(
        @RequestPart("datos") @Valid datos: CrearSolicitudEmpresaRequest,
        @RequestPart("carta") carta: MultipartFile
    ): ResponseEntity<SolicitudEmpresaResponse> {
        log.info(
            "Nueva solicitud empresa: empresa={}, doc={}",
            datos.nombreEmpresa, datos.documentoEstudiante
        )

        validarArchivoPdf(carta)

        val command = CrearSolicitudEmpresaCommand(
            nombreEmpresa = datos.nombreEmpresa,
            nitEmpresa = datos.nitEmpresa,
            nombreContacto = datos.nombreContacto,
            cargoContacto = datos.cargoContacto,
            correoContacto = datos.correoContacto,
            telefonoContacto = datos.telefonoContacto,
            tipoDocumentoEstudiante = datos.tipoDocumentoEstudiante,
            documentoEstudiante = datos.documentoEstudiante,
            nombreEstudiante = datos.nombreEstudiante,
            programaConsultado = datos.programaConsultado,
            tipoValidacion = datos.tipoValidacion,
            observaciones = datos.observaciones,
            aceptaTerminos = datos.aceptaTerminos
        )

        val solicitud = crearSolicitudEmpresaUseCase.execute(command, carta.inputStream)
        log.info("Solicitud registrada: {}", solicitud.numeroSolicitud)
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitud.toResponse())
    }

    // ── GET /api/v1/solicitudes-empresa/{numeroSolicitud} ─────────────────────

    @GetMapping("/{numeroSolicitud}")
    @Operation(
        summary = "Consultar solicitud por número de radicado",
        description = "Retorna los datos de una solicitud dado su número de radicado en formato `SOL-YYYYMMDD-NNNNNN`."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
        ApiResponse(responseCode = "404", description = "No existe solicitud con ese número",
            content = [Content(schema = Schema(hidden = true))])
    )
    fun consultar(
        @Parameter(description = "Número de radicado", example = "SOL-20260421-000001")
        @PathVariable
        @Pattern(
            regexp = "^SOL-\\d{8}-\\d{6}$",
            message = "El número de solicitud debe tener el formato SOL-YYYYMMDD-NNNNNN"
        )
        numeroSolicitud: String
    ): ResponseEntity<SolicitudEmpresaResponse> {
        log.info("Consultar solicitud: {}", numeroSolicitud)
        val solicitud = consultarSolicitudEmpresaUseCase.execute(numeroSolicitud)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(solicitud.toResponse())
    }

    // ── GET /api/v1/solicitudes-empresa/recursos/plantilla-carta ─────────────

    @GetMapping("/recursos/plantilla-carta")
    @Operation(
        summary = "Descargar plantilla de carta de autorización",
        description = "Descarga el archivo `.docx` modelo que la empresa debe completar, firmar y adjuntar en formato PDF al crear la solicitud."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Archivo .docx de la plantilla"),
        ApiResponse(responseCode = "404", description = "Plantilla no encontrada en el servidor",
            content = [Content(schema = Schema(hidden = true))])
    )
    fun descargarPlantilla(): ResponseEntity<ByteArray> {
        val resource = ClassPathResource("static/plantillas/carta-autorizacion-verificacion-academica.docx")
        if (!resource.exists()) {
            log.error("Plantilla de carta de autorización no encontrada en el classpath")
            return ResponseEntity.notFound().build()
        }
        val bytes = resource.inputStream.use { it.readBytes() }
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            contentDisposition = ContentDisposition.attachment()
                .filename("carta-autorizacion-verificacion-academica.docx")
                .build()
        }
        return ResponseEntity.ok().headers(headers).body(bytes)
    }

    // ── Validaciones privadas ─────────────────────────────────────────────────

    private fun validarArchivoPdf(carta: MultipartFile) {
        require(!carta.isEmpty) { "La carta de autorización no puede estar vacía" }
        val esExtensionPdf = carta.originalFilename?.endsWith(".pdf", ignoreCase = true) == true
        val esMimePdf = carta.contentType?.equals("application/pdf", ignoreCase = true) == true
        require(esExtensionPdf && esMimePdf) {
            "La carta de autorización debe ser un archivo PDF (extensión .pdf y tipo application/pdf)"
        }
    }
}
