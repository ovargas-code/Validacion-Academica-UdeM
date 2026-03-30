package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.InitiateValidationUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InitiateValidationUseCaseTest {

    private val validationRepositoryPort: ValidationRepositoryPort = mockk()
    private val studentRepositoryPort: StudentRepositoryPort = mockk()
    private val emailVerificationRepositoryPort: EmailVerificationRepositoryPort = mockk()
    private val mailPort: MailPort = mockk()
    private val meterRegistry = SimpleMeterRegistry()

    private val useCase = InitiateValidationUseCase(
        validationRepositoryPort,
        studentRepositoryPort,
        emailVerificationRepositoryPort,
        mailPort,
        meterRegistry
    )

    private val baseRequest = ValidationRequest(
        requesterName = "Juan Pérez",
        requesterEmail = "juan@example.com",
        studentDocument = "10350001",
        validationType = ValidationType.DEGREE
    )

    private fun savedRequest(request: ValidationRequest) = request.copy(id = 1L, verificationCode = "UDEM-ABCD1234")

    @Test
    fun `retorna NOT_FOUND cuando el estudiante no existe`() {
        every { validationRepositoryPort.save(any()) } answers { savedRequest(firstArg()) }
        every { studentRepositoryPort.findByDocument("10350001") } returns null

        val result = useCase.execute(baseRequest)

        assertEquals("NOT_FOUND", result.status)
        assertNull(result.token)
        assertNull(result.maskedEmail)
        verify(exactly = 0) { mailPort.enviarCodigoVerificacion(any(), any(), any(), any()) }
    }

    @Test
    fun `retorna REQUIRES_REVIEW para validacion DEGREE cuando estudiante esta ACTIVE`() {
        every { validationRepositoryPort.save(any()) } answers { savedRequest(firstArg()) }
        every { studentRepositoryPort.findByDocument("10350001") } returns Student(
            id = 1L, document = "10350001", fullName = "Ana Gomez",
            program = "Medicina", academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.ACTIVO
        )

        val result = useCase.execute(baseRequest.copy(validationType = ValidationType.DEGREE))

        assertEquals("REQUIRES_REVIEW", result.status)
        assertNull(result.token)
        verify(exactly = 0) { mailPort.enviarCodigoVerificacion(any(), any(), any(), any()) }
    }

    @Test
    fun `retorna REQUIRES_REVIEW para validacion ENROLLMENT cuando estudiante esta GRADUATED`() {
        every { validationRepositoryPort.save(any()) } answers { savedRequest(firstArg()) }
        every { studentRepositoryPort.findByDocument("10350001") } returns Student(
            id = 1L, document = "10350001", fullName = "Ana Gomez",
            program = "Medicina", academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.GRADUADO
        )

        val result = useCase.execute(baseRequest.copy(validationType = ValidationType.ENROLLMENT))

        assertEquals("REQUIRES_REVIEW", result.status)
        assertNull(result.token)
    }

    @Test
    fun `retorna VALID con token cuando DEGREE y estudiante GRADUATED`() {
        every { validationRepositoryPort.save(any()) } answers { savedRequest(firstArg()) }
        every { studentRepositoryPort.findByDocument("10350001") } returns Student(
            id = 1L, document = "10350001", fullName = "Ana Gomez",
            program = "Medicina", academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.GRADUADO
        )
        every { emailVerificationRepositoryPort.save(any()) } answers { firstArg() }
        every { mailPort.enviarCodigoVerificacion(any(), any(), any(), any()) } just Runs

        val result = useCase.execute(baseRequest.copy(validationType = ValidationType.DEGREE))

        assertEquals("VALID", result.status)
        assertNotNull(result.token)
        assertNotNull(result.maskedEmail)
        assertTrue(result.maskedEmail!!.startsWith("j***"))
        verify(exactly = 1) { mailPort.enviarCodigoVerificacion("juan@example.com", any(), any(), any()) }
    }

    @Test
    fun `retorna VALID con token cuando ENROLLMENT y estudiante ACTIVE`() {
        every { validationRepositoryPort.save(any()) } answers { savedRequest(firstArg()) }
        every { studentRepositoryPort.findByDocument("10350001") } returns Student(
            id = 1L, document = "10350001", fullName = "Carlos",
            program = "Sistemas", academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.ACTIVO
        )
        every { emailVerificationRepositoryPort.save(any()) } answers { firstArg() }
        every { mailPort.enviarCodigoVerificacion(any(), any(), any(), any()) } just Runs

        val result = useCase.execute(baseRequest.copy(validationType = ValidationType.ENROLLMENT))

        assertEquals("VALID", result.status)
        assertNotNull(result.token)
    }

    @Test
    fun `sigue retornando VALID aunque el envio de correo falle`() {
        every { validationRepositoryPort.save(any()) } answers { savedRequest(firstArg()) }
        every { studentRepositoryPort.findByDocument("10350001") } returns Student(
            id = 1L, document = "10350001", fullName = "Ana Gomez",
            program = "Medicina", academicLevel = AcademicLevel.PREGRADO,
            status = StudentStatus.GRADUADO
        )
        every { emailVerificationRepositoryPort.save(any()) } answers { firstArg() }
        every { mailPort.enviarCodigoVerificacion(any(), any(), any(), any()) } throws RuntimeException("SMTP error")

        val result = useCase.execute(baseRequest.copy(validationType = ValidationType.DEGREE))

        assertEquals("VALID", result.status)
        assertNotNull(result.token)
    }
}
