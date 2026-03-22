package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.CreateStudentUseCase
import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateStudentUseCaseTest {

    // mockk crea un "doble" del repositorio sin necesitar base de datos real
    private val studentRepositoryPort: StudentRepositoryPort = mockk()
    private val useCase = CreateStudentUseCase(studentRepositoryPort)

    @Test
    fun `debe guardar un estudiante y retornarlo`() {
        // ARRANGE: preparamos los datos de entrada y lo que esperamos que devuelva el repo
        val estudiante = Student(
            document = "10350001",
            fullName = "Ana Gómez",
            program = "Medicina",
            academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.ACTIVE
        )
        val estudianteGuardado = estudiante.copy(id = 1L)

        // Le decimos al mock: "cuando llamen save con este estudiante, devuelve estudianteGuardado"
        every { studentRepositoryPort.save(estudiante) } returns estudianteGuardado

        // ACT: ejecutamos el caso de uso
        val resultado = useCase.execute(estudiante)

        // ASSERT: verificamos que el resultado es el esperado
        assertEquals(1L, resultado.id)
        assertEquals("Ana Gómez", resultado.fullName)
        assertEquals("Medicina", resultado.program)

        // Verificamos que el repositorio fue llamado exactamente una vez
        verify(exactly = 1) { studentRepositoryPort.save(estudiante) }
    }
}
