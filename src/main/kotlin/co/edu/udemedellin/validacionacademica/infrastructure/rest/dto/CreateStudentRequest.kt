package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateStudentRequest(
    @field:NotBlank
    val document: String,
    @field:NotBlank
    val fullName: String,
    @field:NotBlank
    val program: String,
    val academicLevel: AcademicLevel,
    val status: StudentStatus,
    val degreeTitle: String? = null,
    val graduationDate: LocalDate? = null
)
