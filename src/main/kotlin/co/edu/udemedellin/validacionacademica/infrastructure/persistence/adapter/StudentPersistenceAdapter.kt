package co.edu.udemedellin.validacionacademica.infrastructure.persistence.adapter

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentPage
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.StudentEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.StudentJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

    override fun findAll(page: Int, size: Int): StudentPage {
        val pageable = PageRequest.of(page, size, Sort.by("fullName").ascending())
        val result = studentJpaRepository.findAll(pageable)
        return StudentPage(
            content = result.content.map { it.toDomain() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            currentPage = result.number,
            pageSize = result.size
        )
    }

    override fun updateByDocument(document: String, student: Student): Student? {
        val existing = studentJpaRepository.findByDocument(document) ?: return null
        existing.fullName = student.fullName
        existing.program = student.program
        existing.academicLevel = student.academicLevel
        existing.status = student.status
        existing.degreeTitle = student.degreeTitle
        existing.graduationDate = student.graduationDate
        return studentJpaRepository.save(existing).toDomain()
    }

    override fun deleteByDocument(document: String): Boolean {
        val existing = studentJpaRepository.findByDocument(document) ?: return false
        studentJpaRepository.delete(existing)
        return true
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
