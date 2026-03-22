package co.edu.udemedellin.validacionacademica.domain.model

import java.time.LocalDate

data class Student(
    val id: Long? = null,
    val document: String,
    val fullName: String,
    val program: String,
    val academicLevel: AcademicLevel,
    val status: StudentStatus,
    val degreeTitle: String? = null,
    val graduationDate: LocalDate? = null
)
