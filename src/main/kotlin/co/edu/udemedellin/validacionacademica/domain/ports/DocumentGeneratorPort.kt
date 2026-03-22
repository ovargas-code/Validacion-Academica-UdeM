package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.model.ValidationResult

interface DocumentGeneratorPort {
    fun generateLetter(request: ValidationRequest, student: Student?, result: ValidationResult): String
}
