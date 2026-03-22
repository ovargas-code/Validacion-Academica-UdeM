package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.springframework.stereotype.Service

@Service
class GetStudentByDocumentUseCase(
    private val studentRepositoryPort: StudentRepositoryPort
) {
    fun execute(document: String): Student? {
        return studentRepositoryPort.findByDocument(document)
    }
}
