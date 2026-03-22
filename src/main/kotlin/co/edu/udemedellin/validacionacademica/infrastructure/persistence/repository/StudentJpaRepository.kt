package co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository

import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.StudentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StudentJpaRepository : JpaRepository<StudentEntity, Long> {
    fun findByDocument(document: String): StudentEntity?
}
