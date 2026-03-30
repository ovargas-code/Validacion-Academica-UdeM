package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteStudentUseCase(
    private val studentRepositoryPort: StudentRepositoryPort
) {
    @Transactional
    fun execute(document: String): Boolean =
        studentRepositoryPort.deleteByDocument(document)
}
