package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentPage

interface StudentRepositoryPort {
    fun save(student: Student): Student
    fun findByDocument(document: String): Student?
    fun findAll(page: Int, size: Int): StudentPage
    fun updateByDocument(document: String, student: Student): Student?
    fun deleteByDocument(document: String): Boolean
}
