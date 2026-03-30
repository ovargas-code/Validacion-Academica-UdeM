package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetStudentByDocumentUseCase(
    private val studentRepositoryPort: StudentRepositoryPort
) {
    @Transactional(readOnly = true)
    fun execute(document: String): Student? {
        return studentRepositoryPort.findByDocument(document)
    }
}
