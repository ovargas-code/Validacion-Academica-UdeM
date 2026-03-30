package co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity

import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "students")
class StudentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var document: String = "",

    @Column(nullable = false)
    var fullName: String = "",

    @Column(nullable = false)
    var program: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var academicLevel: AcademicLevel = AcademicLevel.PREGRADO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StudentStatus = StudentStatus.ACTIVO,

    var degreeTitle: String? = null,

    var graduationDate: LocalDate? = null
)
