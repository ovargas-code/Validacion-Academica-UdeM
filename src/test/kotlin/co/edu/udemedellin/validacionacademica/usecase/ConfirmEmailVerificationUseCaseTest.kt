package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.ConfirmEmailVerificationUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.ConfirmResult
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ConfirmEmailVerificationUseCaseTest {

    private val emailVerificationRepositoryPort: EmailVerificationRepositoryPort = mockk()
    private val studentRepositoryPort: StudentRepositoryPort = mockk()
    private val validationRepositoryPort: ValidationRepositoryPort = mockk()
    private val pdfGeneratorPort: PdfGeneratorPort = mockk()
    private val mailPort: MailPort = mockk()

    private val useCase = ConfirmEmailVerificationUseCase(
        emailVerificationRepositoryPort,
        studentRepositoryPort,
        validationRepositoryPort,
        pdfGeneratorPort,
        mailPort
    )

    private val validVerification = EmailVerification(
        id = 1L,
        token = "test-token",
        email = "juan@example.com",
        code = "123456",
        validationRequestId = 10L,
        studentDocument = "10350001",
        validationType = ValidationType.DEGREE,
        requesterName = "Juan Pérez",
        expiresAt = LocalDateTime.now().plusMinutes(5),
        used = false
    )

    private val student = Student(
        id = 1L, document = "10350001", fullName = "Ana Gomez",
        program = "Medicina", academicLevel = AcademicLevel.PREGRADO,
        status = StudentStatus.GRADUADO
    )

    private val validationRequest = ValidationRequest(
        id = 10L, requesterName = "Juan Pérez", requesterEmail = "juan@example.com",
        studentDocument = "10350001", validationType = ValidationType.DEGREE,
        verificationCode = "UDEM-ABCD1234"
    )

    @Test
    fun `retorna TOKEN_NOT_FOUND cuando el token no existe`() {
        every { emailVerificationRepositoryPort.findByToken("bad-token") } returns null

        val result = useCase.execute("bad-token", "123456")

        assertTrue(result is ConfirmResult.Error)
        assertEquals("TOKEN_NOT_FOUND", (result as ConfirmResult.Error).code)
    }

    @Test
    fun `retorna TOKEN_ALREADY_USED cuando ya fue utilizado`() {
        every { emailVerificationRepositoryPort.findByToken("test-token") } returns validVerification.copy(used = true)

        val result = useCase.execute("test-token", "123456")

        assertTrue(result is ConfirmResult.Error)
        assertEquals("TOKEN_ALREADY_USED", (result as ConfirmResult.Error).code)
    }

    @Test
    fun `retorna TOKEN_EXPIRED cuando el token ha expirado`() {
        every { emailVerificationRepositoryPort.findByToken("test-token") } returns
                validVerification.copy(expiresAt = LocalDateTime.now().minusMinutes(1))

        val result = useCase.execute("test-token", "123456")

        assertTrue(result is ConfirmResult.Error)
        assertEquals("TOKEN_EXPIRED", (result as ConfirmResult.Error).code)
    }

    @Test
    fun `retorna INVALID_CODE cuando el codigo es incorrecto`() {
        every { emailVerificationRepositoryPort.findByToken("test-token") } returns validVerification

        val result = useCase.execute("test-token", "999999")

        assertTrue(result is ConfirmResult.Error)
        assertEquals("INVALID_CODE", (result as ConfirmResult.Error).code)
    }

    @Test
    fun `retorna Success con PDF cuando el codigo es correcto`() {
        val pdfBytes = byteArrayOf(1, 2, 3)
        every { emailVerificationRepositoryPort.findByToken("test-token") } returns validVerification
        every { emailVerificationRepositoryPort.markAsUsed(1L) } just Runs
        every { studentRepositoryPort.findByDocument("10350001") } returns student
        every { validationRepositoryPort.findById(10L) } returns validationRequest
        every { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) } returns pdfBytes
        every { mailPort.enviarCertificado(any(), any(), any()) } just Runs

        val result = useCase.execute("test-token", "123456")

        assertTrue(result is ConfirmResult.Success)
        val success = result as ConfirmResult.Success
        assertArrayEquals(pdfBytes, success.pdfBytes)
        assertEquals("Ana Gomez", success.studentName)
        assertEquals("juan@example.com", success.email)
        assertEquals("UDEM-ABCD1234", success.verificationCode)
        verify(exactly = 1) { emailVerificationRepositoryPort.markAsUsed(1L) }
    }

    @Test
    fun `retorna Success aunque el envio del certificado por correo falle`() {
        val pdfBytes = byteArrayOf(1, 2, 3)
        every { emailVerificationRepositoryPort.findByToken("test-token") } returns validVerification
        every { emailVerificationRepositoryPort.markAsUsed(1L) } just Runs
        every { studentRepositoryPort.findByDocument("10350001") } returns student
        every { validationRepositoryPort.findById(10L) } returns validationRequest
        every { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) } returns pdfBytes
        every { mailPort.enviarCertificado(any(), any(), any()) } throws RuntimeException("SMTP error")

        val result = useCase.execute("test-token", "123456")

        assertTrue(result is ConfirmResult.Success)
    }

    @Test
    fun `marca el token como usado exactamente una vez cuando es exitoso`() {
        val pdfBytes = byteArrayOf(1, 2, 3)
        every { emailVerificationRepositoryPort.findByToken("test-token") } returns validVerification
        every { emailVerificationRepositoryPort.markAsUsed(1L) } just Runs
        every { studentRepositoryPort.findByDocument("10350001") } returns student
        every { validationRepositoryPort.findById(10L) } returns validationRequest
        every { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) } returns pdfBytes
        every { mailPort.enviarCertificado(any(), any(), any()) } just Runs

        useCase.execute("test-token", "123456")

        verify(exactly = 1) { emailVerificationRepositoryPort.markAsUsed(1L) }
    }
}
