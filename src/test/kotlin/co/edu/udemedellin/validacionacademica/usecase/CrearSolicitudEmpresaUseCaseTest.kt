package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.CrearSolicitudEmpresaCommand
import co.edu.udemedellin.validacionacademica.application.usecase.CrearSolicitudEmpresaUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.GenerarNumeroSolicitudService
import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.model.TipoDocumento
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import co.edu.udemedellin.validacionacademica.domain.ports.FileStoragePort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.IOException

class CrearSolicitudEmpresaUseCaseTest {

    private val solicitudRepo: SolicitudEmpresaRepositoryPort = mockk()
    private val fileStorage: FileStoragePort = mockk()
    // MockK puede mockear clases concretas (no solo interfaces)
    private val numerador: GenerarNumeroSolicitudService = mockk()

    private val useCase = CrearSolicitudEmpresaUseCase(solicitudRepo, fileStorage, numerador)

    @Test
    fun `debe crear la solicitud exitosamente y retornar la entidad guardada`() {
        // ARRANGE
        val command = commandDummy()
        val cartaStream = ByteArrayInputStream("contenido-pdf-simulado".toByteArray())
        val rutaEsperada = "solicitudes-empresa/2026/04/uuid-test.pdf"
        val numeroEsperado = "SOL-20260421-000001"
        val solicitudGuardada = solicitudDummy(numeroEsperado, rutaEsperada)

        every { fileStorage.store(any(), any(), any()) } returns rutaEsperada
        every { numerador.generar(any()) } returns numeroEsperado
        every { solicitudRepo.save(any()) } returns solicitudGuardada

        // ACT
        val resultado = useCase.execute(command, cartaStream)

        // ASSERT
        assertEquals(numeroEsperado, resultado.numeroSolicitud)
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.estado)
        assertEquals(rutaEsperada, resultado.rutaCarta)
        assertEquals("Empresa Test S.A.S.", resultado.nombreEmpresa)

        // Verificamos que los tres colaboradores fueron invocados exactamente una vez
        verify(exactly = 1) { fileStorage.store(any(), any(), any()) }
        verify(exactly = 1) { numerador.generar(any()) }
        verify(exactly = 1) { solicitudRepo.save(any()) }
    }

    @Test
    fun `debe lanzar IllegalArgumentException cuando aceptaTerminos es false`() {
        val commandSinTerminos = commandDummy().copy(aceptaTerminos = false)
        val cartaStream = ByteArrayInputStream("pdf".toByteArray())

        assertThrows<IllegalArgumentException> {
            useCase.execute(commandSinTerminos, cartaStream)
        }

        // El storage y el repositorio NO deben ser invocados
        verify(exactly = 0) { fileStorage.store(any(), any(), any()) }
        verify(exactly = 0) { solicitudRepo.save(any()) }
    }

    @Test
    fun `debe propagar IOException cuando el storage falla al guardar la carta`() {
        val command = commandDummy()
        val cartaStream = ByteArrayInputStream("pdf".toByteArray())

        every { fileStorage.store(any(), any(), any()) } throws IOException("Disco lleno")

        assertThrows<IOException> {
            useCase.execute(command, cartaStream)
        }

        // Si el storage falla, el repositorio no debe ser invocado
        verify(exactly = 0) { solicitudRepo.save(any()) }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private fun commandDummy() = CrearSolicitudEmpresaCommand(
        nombreEmpresa = "Empresa Test S.A.S.",
        nitEmpresa = "890123456-1",
        nombreContacto = "Juan Pérez",
        cargoContacto = "Gerente RRHH",
        correoContacto = "rrhh@empresa.com",
        tipoDocumentoEstudiante = TipoDocumento.CC,
        documentoEstudiante = "10350001",
        nombreEstudiante = "Ana Gomez",
        tipoValidacion = ValidationType.DEGREE,
        aceptaTerminos = true
    )

    private fun solicitudDummy(numero: String, rutaCarta: String) = SolicitudEmpresa(
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
        rutaCarta = rutaCarta,
        estado = EstadoSolicitud.PENDIENTE
    )
}
