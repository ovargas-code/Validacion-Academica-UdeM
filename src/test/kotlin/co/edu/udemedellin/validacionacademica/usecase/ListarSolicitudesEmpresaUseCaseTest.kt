package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.ListarSolicitudesEmpresaUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class ListarSolicitudesEmpresaUseCaseTest {

    private val repo: SolicitudEmpresaRepositoryPort = mockk()
    private val useCase = ListarSolicitudesEmpresaUseCase(repo)

    private fun solicitud(numero: String, estado: EstadoSolicitud) = SolicitudEmpresa(
        id = 1L,
        numeroSolicitud = numero,
        nombreEmpresa = "Empresa Test",
        nitEmpresa = "900111222-1",
        nombreContacto = "Juan",
        cargoContacto = "Gerente",
        correoContacto = "juan@test.com",
        tipoDocumentoEstudiante = TipoDocumento.CC,
        documentoEstudiante = "10350001",
        nombreEstudiante = "Ana Gomez",
        tipoValidacion = ValidationType.DEGREE,
        aceptaTerminos = true,
        rutaCarta = "uploads/carta.pdf",
        estado = estado
    )

    @Test
    fun `sin filtro devuelve todas las solicitudes`() {
        val contenido = listOf(
            solicitud("SOL-20260423-000001", EstadoSolicitud.PENDIENTE),
            solicitud("SOL-20260423-000002", EstadoSolicitud.APROBADA)
        )
        val pageableSlot = slot<Pageable>()
        every { repo.findAll(capture(pageableSlot), null) } returns PageImpl(contenido)

        val result = useCase.execute(page = 0, size = 20, estado = null)

        assertEquals(2, result.totalElements)
        assertNull(pageableSlot.captured.sort.getOrderFor("createdAt")?.let { null }
            .also {
                // Verificar orden createdAt DESC
                val order = pageableSlot.captured.sort.getOrderFor("createdAt")
                assertNotNull(order)
                assertEquals(Sort.Direction.DESC, order!!.direction)
            }
        )
    }

    @Test
    fun `con filtro de estado pasa el estado al repositorio`() {
        val contenido = listOf(solicitud("SOL-20260423-000001", EstadoSolicitud.PENDIENTE))
        every { repo.findAll(any(), EstadoSolicitud.PENDIENTE) } returns PageImpl(contenido)

        val result = useCase.execute(page = 0, size = 20, estado = EstadoSolicitud.PENDIENTE)

        assertEquals(1, result.totalElements)
        assertEquals(EstadoSolicitud.PENDIENTE, result.content.first().estado)
    }

    @Test
    fun `paginacion se pasa correctamente al repositorio`() {
        val pageableSlot = slot<Pageable>()
        every { repo.findAll(capture(pageableSlot), null) } returns PageImpl(emptyList())

        useCase.execute(page = 2, size = 15, estado = null)

        assertEquals(2, pageableSlot.captured.pageNumber)
        assertEquals(15, pageableSlot.captured.pageSize)
    }

    @Test
    fun `pagina vacia devuelve Page vacio sin errores`() {
        every { repo.findAll(any(), any()) } returns PageImpl(emptyList())

        val result = useCase.execute(page = 0, size = 20, estado = null)

        assertTrue(result.isEmpty)
        assertEquals(0, result.totalElements)
    }
}
