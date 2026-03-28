package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateStudentRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 20, message = "El documento debe tener entre 1 y 20 caracteres")
    @field:Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "El documento solo puede contener letras, números y guiones")
    val document: String,

    @field:NotBlank
    @field:Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    val fullName: String,

    @field:NotBlank
    @field:Size(max = 200, message = "El programa no puede superar los 200 caracteres")
    val program: String,

    val academicLevel: AcademicLevel,
    val status: StudentStatus,

    @field:Size(max = 200, message = "El título no puede superar los 200 caracteres")
    val degreeTitle: String? = null,
    val graduationDate: LocalDate? = null
)
