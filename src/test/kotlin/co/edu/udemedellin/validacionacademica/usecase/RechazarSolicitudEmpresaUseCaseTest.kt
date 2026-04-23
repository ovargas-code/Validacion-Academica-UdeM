package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.RechazarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.SaveAuditEventUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RechazarSolicitudEmpresaUseCaseTest {

    private val solicitudRepo: SolicitudEmpresaRepositoryPort = mockk()
    private val mailPort: MailPort = mockk()
    private val saveAuditEventUseCase: SaveAuditEventUseCase = mockk()

    private val useCase = RechazarSolicitudEmpresaUseCase(solicitudRepo, mailPort, saveAuditEventUseCase)

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
    fun `rechaza desde PENDIENTE exitosamente`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.PENDIENTE)
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionRechazada(any(), any(), any(), any()) } just Runs
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", "Documentación incompleta", "admin")

        assertEquals(EstadoSolicitud.RECHAZADA, result.estado)
        assertEquals("Documentación incompleta", result.comentarioAdmin)
        assertEquals("admin", result.adminResponsable)
        assertNotNull(result.fechaRevision)
        verify(exactly = 1) { mailPort.enviarNotificacionRechazada("juan@acme.com", "Juan Pérez", "SOL-20260422-000001", "Documentación incompleta") }
    }

    @Test
    fun `rechaza desde EN_REVISION exitosamente`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.EN_REVISION)
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionRechazada(any(), any(), any(), any()) } just Runs
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", "Datos no verificables", "admin")

        assertEquals(EstadoSolicitud.RECHAZADA, result.estado)
    }

    @Test
    fun `comentarioAdmin nulo lanza IllegalArgumentException antes de consultar la BD`() {
        assertThrows<IllegalArgumentException> {
            useCase.execute("SOL-20260422-000001", null, "admin")
        }
        verify(exactly = 0) { solicitudRepo.findByNumeroSolicitud(any()) }
    }

    @Test
    fun `comentarioAdmin en blanco lanza IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            useCase.execute("SOL-20260422-000001", "   ", "admin")
        }
    }

    @Test
    fun `rechazar desde APROBADA lanza InvalidStateTransitionException`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.APROBADA)

        assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", "Motivo", "admin")
        }
        verify(exactly = 0) { solicitudRepo.update(any()) }
    }

    @Test
    fun `rechazar desde RECHAZADA lanza InvalidStateTransitionException`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.RECHAZADA)

        assertThrows<InvalidStateTransitionException> {
            useCase.execute("SOL-20260422-000001", "Motivo", "admin")
        }
    }

    @Test
    fun `fallo de correo no interrumpe el rechazo`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.PENDIENTE)
        every { solicitudRepo.update(any()) } answers { firstArg() }
        every { mailPort.enviarNotificacionRechazada(any(), any(), any(), any()) } throws RuntimeException("SMTP error")
        every { saveAuditEventUseCase.execute(any(), any(), any(), any()) } just Runs

        val result = useCase.execute("SOL-20260422-000001", "Motivo válido", "admin")

        assertEquals(EstadoSolicitud.RECHAZADA, result.estado)
    }
}
