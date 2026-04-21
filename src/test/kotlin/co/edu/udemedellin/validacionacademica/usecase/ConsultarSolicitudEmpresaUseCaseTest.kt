package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.ConsultarSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.model.TipoDocumento
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConsultarSolicitudEmpresaUseCaseTest {

    private val solicitudRepo: SolicitudEmpresaRepositoryPort = mockk()
    private val useCase = ConsultarSolicitudEmpresaUseCase(solicitudRepo)

    @Test
    fun `debe retornar la solicitud cuando el numero de radicado existe`() {
        val numero = "SOL-20260421-000001"
        val solicitud = solicitudDummy(numero)
        every { solicitudRepo.findByNumeroSolicitud(numero) } returns solicitud

        val resultado = useCase.execute(numero)

        assertEquals(numero, resultado?.numeroSolicitud)
        assertEquals("Empresa Test S.A.S.", resultado?.nombreEmpresa)
        verify(exactly = 1) { solicitudRepo.findByNumeroSolicitud(numero) }
    }

    @Test
    fun `debe retornar null cuando el numero de radicado no existe`() {
        every { solicitudRepo.findByNumeroSolicitud(any()) } returns null

        val resultado = useCase.execute("SOL-19990101-000001")

        assertNull(resultado)
        verify(exactly = 1) { solicitudRepo.findByNumeroSolicitud("SOL-19990101-000001") }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private fun solicitudDummy(numero: String) = SolicitudEmpresa(
        id = 1L,
        numeroSolicitud = numero,
        nombreEmpresa = "Empresa Test S.A.S.",
        nitEmpresa = "890123456-1",
        nombreContacto = "Juan Pérez",
        cargoContacto = "Gerente RRHH",
        correoContacto = "rrhh@empresa.com",
        tipoDocumentoEstudiante = TipoDocumento.CC,
        documentoEstudiante = "10350001",
        nombreEstudiante = "Ana Gomez",
        tipoValidacion = ValidationType.DEGREE,
        aceptaTerminos = true,
        rutaCarta = "solicitudes-empresa/2026/04/uuid.pdf",
        estado = EstadoSolicitud.PENDIENTE
    )
}
