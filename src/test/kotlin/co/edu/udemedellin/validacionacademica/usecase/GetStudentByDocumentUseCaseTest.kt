package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.GetStudentByDocumentUseCase
import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetStudentByDocumentUseCaseTest {

    private val studentRepositoryPort: StudentRepositoryPort = mockk()
    private val useCase = GetStudentByDocumentUseCase(studentRepositoryPort)

    @Test
    fun `debe retornar el estudiante cuando existe`() {
        val estudiante = Student(
            id = 1L,
            document = "10350001",
            fullName = "Ana Gómez",
            program = "Medicina",
            academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.ACTIVE
        )
        every { studentRepositoryPort.findByDocument("10350001") } returns estudiante

        val resultado = useCase.execute("10350001")

        assertNotNull(resultado)
        assertEquals("Ana Gómez", resultado?.fullName)
    }

    @Test
    fun `debe retornar null cuando el estudiante no existe`() {
        every { studentRepositoryPort.findByDocument("99999999") } returns null

        val resultado = useCase.execute("99999999")

        assertNull(resultado)
    }
}
