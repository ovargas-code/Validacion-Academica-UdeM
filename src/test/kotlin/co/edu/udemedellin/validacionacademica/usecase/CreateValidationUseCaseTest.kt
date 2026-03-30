package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.CreateValidationUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.*
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CreateValidationUseCaseTest {

    private val validationRepo: ValidationRepositoryPort = mockk()
    private val studentRepo: StudentRepositoryPort = mockk()
    private val documentGenerator: DocumentGeneratorPort = mockk()
    private val pdfGenerator: PdfGeneratorPort = mockk()
    private val mailPort: MailPort = mockk()

    private val useCase = CreateValidationUseCase(
        validationRepo, studentRepo, documentGenerator, pdfGenerator, mailPort
    )

    @Test
    fun `debe retornar NOT_FOUND cuando el estudiante no existe`() {
        val request = ValidationRequest(
            requesterName = "Empresa ABC",
            requesterEmail = "rrhh@empresa.com",
            studentDocument = "99999999",
            validationType = ValidationType.DEGREE
        )
        val savedRequest = request.copy(id = 1L, verificationCode = "UDEM-TEST01")

        every { validationRepo.save(any()) } returns savedRequest
        every { studentRepo.findByDocument("99999999") } returns null
        every { documentGenerator.generateLetter(any(), null, any()) } returns "carta"

        val resultado = useCase.execute(request)

        assertEquals(ValidationStatus.NOT_FOUND, resultado.result.status)
    }

    @Test
    fun `debe retornar VALID cuando el estudiante esta graduado y pide DEGREE`() {
        val request = ValidationRequest(
            requesterName = "Empresa ABC",
            requesterEmail = "rrhh@empresa.com",
            studentDocument = "10350003",
            validationType = ValidationType.DEGREE
        )
        val savedRequest = request.copy(id = 1L, verificationCode = "UDEM-TEST02")
        val estudiante = Student(
            id = 1L, document = "10350003", fullName = "María Torres",
            program = "Derecho", academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.GRADUADO, degreeTitle = "Abogada"
        )

        every { validationRepo.save(any()) } returns savedRequest
        every { studentRepo.findByDocument("10350003") } returns estudiante
        every { documentGenerator.generateLetter(any(), any(), any()) } returns "carta"
        every { pdfGenerator.generateCertificate(any(), any(), any(), any()) } returns ByteArray(0)
        every { mailPort.enviarCertificado(any(), any(), any()) } just runs

        val resultado = useCase.execute(request)

        assertEquals(ValidationStatus.VALID, resultado.result.status)
        assertEquals("Derecho", resultado.student?.program)
    }
}
