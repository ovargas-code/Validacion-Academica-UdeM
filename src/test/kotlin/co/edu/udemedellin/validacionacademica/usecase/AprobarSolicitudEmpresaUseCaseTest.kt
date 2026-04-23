package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.AprobarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.SaveAuditEventUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AprobarSolicitudEmpresaUseCaseTest {

    private val solicitudRepo: SolicitudEmpresaRepositoryPort = mockk()
    private val studentRepo: StudentRepositoryPort = mockk()
    private val pdfGeneratorPort: PdfGeneratorPort = mockk()
    private val mailPort: MailPort = mockk()
    private val saveAuditEventUseCase: SaveAuditEventUseCase = mockk()

    private val useCase = AprobarSolicitudEmpresaUseCase(
        solicitudRepo, studentRepo, pdfGeneratorPort, mailPort, saveAuditEventUseCase,
        baseUrl = "http://localhost:8080"
    )

    private val pdfBytes = byteArrayOf(1, 2, 3)

    private val student = Student(
        id = 1L, document = "10350001", fullName = "Ana Gomez",
        program = "Medicina", academicLevel = AcademicLevel.PREGRADO,
        status = StudentStatus.GRADUADO
    )

    private fun solicitud(estado: EstadoSolicitud) = SolicitudEmpresa(
        id = 1L,
        numeroSolicitud = "SOL-20260422-000001",
        nombreEmpresa = "Acme S.A.S.",
        nitEmpresa = "900111222-1",
        nombreContacto = "Juan Pérez",
        cargoContacto = "Gerente",
        correoContacto = "juan@acme.com",
        tipoDocumentoEstudiante = TipoDocumento.CC,
        documentoEstudiante = "10350001",
        nombreEstudiante = "Ana Gomez",
        tipoValidacion = ValidationType.DEGREE,
        aceptaTerminos = true,
        rutaCarta = "uploads/carta.pdf",
        estado = estado
    )

    @Test
    fun `aprueba desde PENDIENTE exitosamente`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.PENDIENTE)
        every { studentRepo.findByDocument("10350001") } returns student
        every { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) } returns pdfBytes
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionAprobada(any(), any(), any(), any()) } just Runs
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", "Verificado correctamente", "admin")

        assertEquals(EstadoSolicitud.APROBADA, result.estado)
        assertEquals("admin", result.adminResponsable)
        assertNotNull(result.fechaRevision)
        verify(exactly = 1) {
            pdfGeneratorPort.generateCertificate(
                studentName = "Ana Gomez",
                studentDocument = "10350001",
                program = "Medicina",
                verificationUrl = "http://localhost:8080/api/v1/solicitudes-empresa/SOL-20260422-000001"
            )
        }
        verify(exactly = 1) { mailPort.enviarNotificacionAprobada("juan@acme.com", "Juan Pérez", "SOL-20260422-000001", pdfBytes) }
    }

    @Test
    fun `aprueba desde EN_REVISION exitosamente`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.EN_REVISION)
        every { studentRepo.findByDocument("10350001") } returns student
        every { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) } returns pdfBytes
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionAprobada(any(), any(), any(), any()) } just Runs
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", null, "admin")

        assertEquals(EstadoSolicitud.APROBADA, result.estado)
    }

    @Test
    fun `aprobar desde APROBADA lanza InvalidStateTransitionException`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.APROBADA)

        assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
        verify(exactly = 0) { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) }
    }

    @Test
    fun `aprobar desde RECHAZADA lanza InvalidStateTransitionException`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.RECHAZADA)

        assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
    }

    @Test
    fun `estudiante no encontrado lanza InvalidStateTransitionException con mensaje apropiado`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.PENDIENTE)
        every { studentRepo.findByDocument("10350001") } returns null

        val ex = assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
        assertTrue(ex.message!!.contains("estudiante referenciado no existe"))
    }

    @Test
    fun `fallo de correo no interrumpe la aprobacion`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.PENDIENTE)
        every { studentRepo.findByDocument("10350001") } returns student
        every { pdfGeneratorPort.generateCertificate(any(), any(), any(), any()) } returns pdfBytes
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionAprobada(any(), any(), any(), any()) } throws RuntimeException("SMTP error")
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", null, "admin")

        assertEquals(EstadoSolicitud.APROBADA, result.estado)
    }
}
