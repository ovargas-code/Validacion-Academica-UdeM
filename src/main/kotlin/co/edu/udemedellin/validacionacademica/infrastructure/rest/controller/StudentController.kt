package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.CreateStudentUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.DeleteStudentUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.GetStudentByDocumentUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.ImportStudentsUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.ListStudentsUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.SaveAuditEventUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.UpdateStudentUseCase
import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CreateStudentRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.StudentPageResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.StudentResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Estudiantes", description = "Gestión del registro de estudiantes")
@SecurityRequirement(name = "bearerAuth")
@Validated
class StudentController(
    private val createStudentUseCase: CreateStudentUseCase,
    private val getStudentByDocumentUseCase: GetStudentByDocumentUseCase,
    private val listStudentsUseCase: ListStudentsUseCase,
    private val updateStudentUseCase: UpdateStudentUseCase,
    private val deleteStudentUseCase: DeleteStudentUseCase,
    private val importStudentsUseCase: ImportStudentsUseCase,
    private val saveAuditEventUseCase: SaveAuditEventUseCase
) {

    @PostMapping
    @Operation(
        summary = "Registrar estudiante",
        description = "Crea un nuevo registro de estudiante en el sistema. El campo 'document' debe ser único."
    )
    fun create(
        @Valid @RequestBody request: CreateStudentRequest,
        authentication: Authentication
    ): ResponseEntity<StudentResponse> {
        val student = Student(
            document = request.document,
            fullName = request.fullName,
            program = request.program,
            academicLevel = request.academicLevel,
            status = request.status,
            degreeTitle = request.degreeTitle,
            graduationDate = request.graduationDate
        )
        val saved = createStudentUseCase.execute(student)
        saveAuditEventUseCase.execute(
            action = AuditAction.CREATE_STUDENT,
            performedBy = authentication.name,
            targetDocument = saved.document,
            details = "${saved.fullName} — ${saved.program} (${saved.academicLevel})"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping("/{document}")
    @Operation(
        summary = "Buscar estudiante por documento",
        description = "Retorna la información académica de un estudiante dado su número de documento."
    )
    fun findByDocument(
        @Parameter(description = "Número de documento del estudiante", example = "10350001")
        @PathVariable
        @Pattern(regexp = "^[A-Za-z0-9\\-]{1,20}$", message = "Documento con formato inválido")
        document: String
    ): ResponseEntity<StudentResponse> {
        val student = getStudentByDocumentUseCase.execute(document)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(student.toResponse())
    }

    @GetMapping
    @Operation(
        summary = "Listar estudiantes paginados",
        description = "Retorna una página de estudiantes ordenados por nombre. " +
                "page empieza en 0, size entre 1 y 100 (por defecto 20)."
    )
    fun listAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<StudentPageResponse> {
        return ResponseEntity.ok(listStudentsUseCase.execute(page, size).toResponse())
    }

    @PutMapping("/{document}")
    @Operation(
        summary = "Actualizar estudiante",
        description = "Actualiza los datos académicos de un estudiante existente. El documento no puede cambiarse."
    )
    fun update(
        @Parameter(description = "Número de documento del estudiante", example = "10350001")
        @PathVariable
        @Pattern(regexp = "^[A-Za-z0-9\\-]{1,20}$", message = "Documento con formato inválido")
        document: String,
        @Valid @RequestBody request: CreateStudentRequest,
        authentication: Authentication
    ): ResponseEntity<StudentResponse> {
        val student = Student(
            document = document,
            fullName = request.fullName,
            program = request.program,
            academicLevel = request.academicLevel,
            status = request.status,
            degreeTitle = request.degreeTitle,
            graduationDate = request.graduationDate
        )
        val updated = updateStudentUseCase.execute(document, student)
            ?: return ResponseEntity.notFound().build()
        saveAuditEventUseCase.execute(
            action = AuditAction.UPDATE_STUDENT,
            performedBy = authentication.name,
            targetDocument = document,
            details = "Estado: ${updated.status} — Programa: ${updated.program}"
        )
        return ResponseEntity.ok(updated.toResponse())
    }

    @DeleteMapping("/{document}")
    @Operation(
        summary = "Eliminar estudiante",
        description = "Elimina el registro de un estudiante dado su número de documento."
    )
    fun delete(
        @Parameter(description = "Número de documento del estudiante", example = "10350001")
        @PathVariable
        @Pattern(regexp = "^[A-Za-z0-9\\-]{1,20}$", message = "Documento con formato inválido")
        document: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val deleted = deleteStudentUseCase.execute(document)
        if (deleted) {
            saveAuditEventUseCase.execute(
                action = AuditAction.DELETE_STUDENT,
                performedBy = authentication.name,
                targetDocument = document
            )
        }
        return if (deleted) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }

    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Importar estudiantes desde CSV",
        description = """Carga masiva de estudiantes desde un archivo CSV con encabezado:
document,fullName,program,academicLevel,status,degreeTitle,graduationDate

Valores válidos para academicLevel: TECNICO, TECNOLOGICO, PREGRADO, ESPECIALIZACION, MAESTRIA, DOCTORADO
Valores válidos para status: ACTIVO, GRADUADO, INACTIVO, SUSPENDIDO
graduationDate en formato yyyy-MM-dd (opcional)"""
    )
    fun importCsv(
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<Any> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("message" to "El archivo CSV no puede estar vacío"))
        }
        if (!file.originalFilename.orEmpty().endsWith(".csv", ignoreCase = true)) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Solo se aceptan archivos .csv"))
        }
        val result = importStudentsUseCase.execute(file.inputStream)
        saveAuditEventUseCase.execute(
            action = AuditAction.IMPORT_STUDENTS,
            performedBy = authentication.name,
            details = "Archivo: ${file.originalFilename} — Importados: ${result.imported}, Errores: ${result.errors.size}"
        )
        val status = if (result.errors.isEmpty()) HttpStatus.OK else HttpStatus.MULTI_STATUS
        return ResponseEntity.status(status).body(result)
    }
}
