package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.CreateStudentUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.GetStudentByDocumentUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.ListStudentsUseCase
import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.CreateStudentRequest
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.StudentResponse
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Estudiantes", description = "Gestión del registro de estudiantes")
class StudentController(
    private val createStudentUseCase: CreateStudentUseCase,
    private val getStudentByDocumentUseCase: GetStudentByDocumentUseCase,
    private val listStudentsUseCase: ListStudentsUseCase
) {

    @PostMapping
    @Operation(
        summary = "Registrar estudiante",
        description = "Crea un nuevo registro de estudiante en el sistema. El campo 'document' debe ser único."
    )
    fun create(@Valid @RequestBody request: CreateStudentRequest): ResponseEntity<StudentResponse> {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping("/{document}")
    @Operation(
        summary = "Buscar estudiante por documento",
        description = "Retorna la información académica de un estudiante dado su número de documento."
    )
    fun findByDocument(
        @Parameter(description = "Número de documento del estudiante", example = "10350001")
        @PathVariable document: String
    ): ResponseEntity<StudentResponse> {
        val student = getStudentByDocumentUseCase.execute(document)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(student.toResponse())
    }

    @GetMapping
    @Operation(
        summary = "Listar todos los estudiantes",
        description = "Retorna la lista completa de estudiantes registrados en el sistema."
    )
    fun listAll(): ResponseEntity<List<StudentResponse>> {
        return ResponseEntity.ok(listStudentsUseCase.execute().map { it.toResponse() })
    }
}
