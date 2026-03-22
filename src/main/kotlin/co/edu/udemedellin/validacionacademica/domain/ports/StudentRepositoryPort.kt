package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.Student

interface StudentRepositoryPort {
    fun save(student: Student): Student
    fun findByDocument(document: String): Student?
    fun findAll(): List<Student>
}
