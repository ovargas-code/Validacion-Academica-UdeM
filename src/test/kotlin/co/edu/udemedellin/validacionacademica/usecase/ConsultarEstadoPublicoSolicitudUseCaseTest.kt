package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.ConsultarEstadoPublicoSolicitudUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.rest.dto.toPublicResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ConsultarEstadoPublicoSolicitudUseCaseTest {

    private val repo: SolicitudEmpresaRepositoryPort = mockk()
    private val useCase = ConsultarEstadoPublicoSolicitudUseCase(repo)

    private fun solicitud(
        estado: EstadoSolicitud,
        comentarioAdmin: String? = null,
        fechaRevision: LocalDateTime? = null
    ) = SolicitudEmpresa(
        id = 1L,
        numeroSolicitud = "SOL-20260423-000001",
        nombreEmpresa = "Empresa Test S.A.S.",
        nitEmpresa = "900111222-1",
        nombreContacto = "Juan Pérez",
        cargoContacto = "Gerente",
        correoContacto = "juan@empresa.com",
        tipoDocumentoEstudiante = TipoDocumento.CC,
        documentoEstudiante = "10350001",
        nombreEstudiante = "Ana Gomez",
        tipoValidacion = ValidationType.DEGREE,
        aceptaTerminos = true,
        rutaCarta = "uploads/carta.pdf",
        estado = estado,
        comentarioAdmin = comentarioAdmin,
        fechaRevision = fechaRevision,
        adminResponsable = if (fechaRevision != null) "admin" else null
    )

    // ── Use case devuelve el dominio sin transformar ───────────────────────────

    @Test
    fun `retorna la solicitud cuando existe`() {
        every { repo.findByNumeroSolicitud("SOL-20260423-000001") } returns solicitud(EstadoSolicitud.PENDIENTE)

        val result = useCase.execute("SOL-20260423-000001")

        assertNotNull(result)
        assertEquals("SOL-20260423-000001", result!!.numeroSolicitud)
    }

    @Test
    fun `retorna null cuando la solicitud no existe`() {
        every { repo.findByNumeroSolicitud(any()) } returns null

        assertNull(useCase.execute("SOL-19990101-000001"))
    }

    // ── Lógica de privacidad en toPublicResponse ──────────────────────────────

    @Test
    fun `estado PENDIENTE no expone motivoRechazo ni campos sensibles`() {
        every { repo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.PENDIENTE)

        val response = useCase.execute("SOL-20260423-000001")!!.toPublicResponse()

        assertEquals(EstadoSolicitud.PENDIENTE, response.estado)
        assertNull(response.motivoRechazo)
        assertNull(response.fechaRevision)
        assertEquals("Empresa Test S.A.S.", response.nombreEmpresa)
        assertEquals("Ana Gomez", response.nombreEstudiante)
    }

    @Test
    fun `estado EN_REVISION no expone motivoRechazo`() {
        val revision = LocalDateTime.now()
        every { repo.findByNumeroSolicitud(any()) } returns solicitud(EstadoSolicitud.EN_REVISION, fechaRevision = revision)

        val response = useCase.execute("SOL-20260423-000001")!!.toPublicResponse()

        assertEquals(EstadoSolicitud.EN_REVISION, response.estado)
        assertNull(response.motivoRechazo)
        assertEquals(revision, response.fechaRevision)
    }

    @Test
    fun `estado APROBADA no expone motivoRechazo aunque comentarioAdmin no sea null`() {
        val revision = LocalDateTime.now()
        every { repo.findByNumeroSolicitud(any()) } returns
            solicitud(EstadoSolicitud.APROBADA, comentarioAdmin = "Aprobada sin observaciones", fechaRevision = revision)

        val response = useCase.execute("SOL-20260423-000001")!!.toPublicResponse()

        assertEquals(EstadoSolicitud.APROBADA, response.estado)
        // comentarioAdmin del admin NO se revela aunque exista
        assertNull(response.motivoRechazo)
    }

    @Test
    fun `estado RECHAZADA expone motivoRechazo con el comentarioAdmin`() {
        val revision = LocalDateTime.now()
        every { repo.findByNumeroSolicitud(any()) } returns
            solicitud(EstadoSolicitud.RECHAZADA, comentarioAdmin = "Documentación incompleta", fechaRevision = revision)

        val response = useCase.execute("SOL-20260423-000001")!!.toPublicResponse()

        assertEquals(EstadoSolicitud.RECHAZADA, response.estado)
        assertEquals("Documentación incompleta", response.motivoRechazo)
        assertEquals(revision, response.fechaRevision)
    }
}
