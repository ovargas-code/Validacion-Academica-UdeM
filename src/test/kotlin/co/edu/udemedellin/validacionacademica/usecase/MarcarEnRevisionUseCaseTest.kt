package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.MarcarEnRevisionUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.SaveAuditEventUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class MarcarEnRevisionUseCaseTest {

    private val solicitudRepo: SolicitudEmpresaRepositoryPort = mockk()
    private val mailPort: MailPort = mockk()
    private val saveAuditEventUseCase: SaveAuditEventUseCase = mockk()

    private val useCase = MarcarEnRevisionUseCase(solicitudRepo, mailPort, saveAuditEventUseCase)

    private fun solicitudPendiente(numero: String = "SOL-20260422-000001") = SolicitudEmpresa(
        id = 1L,
        numeroSolicitud = numero,
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
        estado = EstadoSolicitud.PENDIENTE
    )

    @Test
    fun `transicion PENDIENTE a EN_REVISION exitosa`() {
        val solicitud = solicitudPendiente()
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionEnRevision(any(), any(), any()) } just Runs
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", null, "admin")

        assertEquals(EstadoSolicitud.EN_REVISION, result.estado)
        assertEquals("admin", result.adminResponsable)
        assertNotNull(result.fechaRevision)
        verify(exactly = 1) { mailPort.enviarNotificacionEnRevision("juan@acme.com", "Juan Pérez", "SOL-20260422-000001") }
        verify(exactly = 1) { saveAuditEventUseCase.execute(AuditAction.SOLICITUD_EMPRESA_REVISADA, "admin", "SOL-20260422-000001", any()) }
    }

    @Test
    fun `transicion desde EN_REVISION lanza InvalidStateTransitionException`() {
        val solicitud = solicitudPendiente().copy(estado = EstadoSolicitud.EN_REVISION)
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud

        val ex = assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
        assertTrue(ex.message!!.contains("EN_REVISION"))
        verify(exactly = 0) { solicitudRepo.update(any()) }
    }

    @Test
    fun `transicion desde APROBADA lanza InvalidStateTransitionException`() {
        val solicitud = solicitudPendiente().copy(estado = EstadoSolicitud.APROBADA)
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud

        assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
    }

    @Test
    fun `solicitud no encontrada lanza NoSuchElementException`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns null

        assertThrows<NoSuchElementException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
    }

    @Test
    fun `fallo de correo no interrumpe la operacion`() {
        val solicitud = solicitudPendiente()
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionEnRevision(any(), any(), any()) } throws RuntimeException("SMTP error")
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", null, "admin")

        assertEquals(EstadoSolicitud.EN_REVISION, result.estado)
    }
}
