package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.StudentPage
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import org.springframework.stereotype.Service

private const val DEFAULT_PAGE_SIZE = 20
private const val MAX_PAGE_SIZE = 100

@Service
class ListStudentsUseCase(
    private val studentRepositoryPort: StudentRepositoryPort
) {
    fun execute(page: Int = 0, size: Int = DEFAULT_PAGE_SIZE): StudentPage =
        studentRepositoryPort.findAll(
            page = page.coerceAtLeast(0),
            size = size.coerceIn(1, MAX_PAGE_SIZE)
        )
}
