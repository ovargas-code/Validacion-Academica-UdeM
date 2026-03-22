package co.edu.udemedellin.validacionacademica.infrastructure.persistence.adapter

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.StudentEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.StudentJpaRepository
import org.springframework.stereotype.Component

@Component
class StudentPersistenceAdapter(
    private val studentJpaRepository: StudentJpaRepository
) : StudentRepositoryPort {

    override fun save(student: Student): Student {
        val saved = studentJpaRepository.save(student.toEntity())
        return saved.toDomain()
    }

    override fun findByDocument(document: String): Student? {
        return studentJpaRepository.findByDocument(document)?.toDomain()
    }

    override fun findAll(): List<Student> {
        return studentJpaRepository.findAll().map { it.toDomain() }
    }

    private fun Student.toEntity(): StudentEntity = StudentEntity(
        id = id,
        document = document,
        fullName = fullName,
        program = program,
        academicLevel = academicLevel,
        status = status,
        degreeTitle = degreeTitle,
        graduationDate = graduationDate
    )

    private fun StudentEntity.toDomain(): Student = Student(
        id = id,
        document = document,
        fullName = fullName,
        program = program,
        academicLevel = academicLevel,
        status = status,
        degreeTitle = degreeTitle,
        graduationDate = graduationDate
    )
}
