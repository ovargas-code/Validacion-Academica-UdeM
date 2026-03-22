package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import java.time.LocalDate

data class StudentResponse(
    val id: Long?,
    val document: String,
    val fullName: String,
    val program: String,
    val academicLevel: AcademicLevel,
    val status: StudentStatus,
    val degreeTitle: String?,
    val graduationDate: LocalDate?
)

fun Student.toResponse() = StudentResponse(
    id = id,
    document = document,
    fullName = fullName,
    program = program,
    academicLevel = academicLevel,
    status = status,
    degreeTitle = degreeTitle,
    graduationDate = graduationDate
)
