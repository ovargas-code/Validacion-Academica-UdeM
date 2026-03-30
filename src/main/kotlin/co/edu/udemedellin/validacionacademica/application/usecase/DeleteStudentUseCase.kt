package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.springframework.stereotype.Service

@Service
class DeleteStudentUseCase(
    private val studentRepositoryPort: StudentRepositoryPort
) {
    fun execute(document: String): Boolean =
        studentRepositoryPort.deleteByDocument(document)
}
