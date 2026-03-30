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
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "students",
    indexes = [
        Index(name = "idx_students_document", columnList = "document", unique = true),
        Index(name = "idx_students_status", columnList = "status")
    ]
)
class StudentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 20)
    var document: String = "",

    @Column(nullable = false, length = 150)
    var fullName: String = "",

    @Column(nullable = false, length = 200)
    var program: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var academicLevel: AcademicLevel = AcademicLevel.PREGRADO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StudentStatus = StudentStatus.ACTIVO,

    @Column(length = 200)
    var degreeTitle: String? = null,

    var graduationDate: LocalDate? = null
)
