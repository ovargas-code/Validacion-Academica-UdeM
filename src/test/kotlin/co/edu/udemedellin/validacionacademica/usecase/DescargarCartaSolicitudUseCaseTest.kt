package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.DescargarCartaSolicitudUseCase
import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.FileStoragePort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.NoSuchFileException

class DescargarCartaSolicitudUseCaseTest {

    private val repo: SolicitudEmpresaRepositoryPort = mockk()
    private val fileStorage: FileStoragePort = mockk()
    private val useCase = DescargarCartaSolicitudUseCase(repo, fileStorage)

    private val pdfBytes = "contenido-pdf".toByteArray()

    private val solicitud = SolicitudEmpresa(
        id = 1L,
        numeroSolicitud = "SOL-20260423-000001",
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
        rutaCarta = "solicitudes-empresa/2026/04/uuid.pdf",
        estado = EstadoSolicitud.PENDIENTE
    )

    @Test
    fun `descarga exitosa retorna los bytes del archivo`() {
        every { repo.findByNumeroSolicitud("SOL-20260423-000001") } returns solicitud
        every { fileStorage.loadAsBytes("solicitudes-empresa/2026/04/uuid.pdf") } returns pdfBytes

        val result = useCase.execute("SOL-20260423-000001")

        assertArrayEquals(pdfBytes, result)
    }

    @Test
    fun `solicitud no encontrada lanza NoSuchElementException`() {
        every { repo.findByNumeroSolicitud(any()) } returns null

        val ex = assertThrows<NoSuchElementException> {
            useCase.execute("SOL-19990101-000001")
        }
        assertTrue(ex.message!!.contains("SOL-19990101-000001"))
    }

    @Test
    fun `archivo no encontrado en disco lanza IllegalArgumentException con mensaje claro`() {
        every { repo.findByNumeroSolicitud("SOL-20260423-000001") } returns solicitud
        every { fileStorage.loadAsBytes(any()) } throws NoSuchFileException("solicitudes-empresa/2026/04/uuid.pdf")

        val ex = assertThrows<IllegalArgumentException> {
            useCase.execute("SOL-20260423-000001")
        }
        assertTrue(ex.message!!.contains("SOL-20260423-000001"))
        assertTrue(ex.message!!.contains("no está disponible"))
        assertTrue(ex.message!!.contains("Contacta al administrador"))
    }
}
