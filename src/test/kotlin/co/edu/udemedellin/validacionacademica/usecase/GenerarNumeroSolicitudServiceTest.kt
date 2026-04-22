package co.edu.udemedellin.validacionacademica.usecase

import co.edu.udemedellin.validacionacademica.application.usecase.GenerarNumeroSolicitudService
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GenerarNumeroSolicitudServiceTest {

    private val solicitudRepo: SolicitudEmpresaRepositoryPort = mockk()
    private val service = GenerarNumeroSolicitudService(solicitudRepo)

    private val fecha = LocalDate.of(2026, 4, 22)

    @Test
    fun `cuando no hay registros del dia, genera 000001`() {
        every { solicitudRepo.findMaxNumeroSolicitudByFecha(fecha) } returns null

        val resultado = service.generar(fecha)

        assertEquals("SOL-20260422-000001", resultado)
        verify(exactly = 1) { solicitudRepo.findMaxNumeroSolicitudByFecha(fecha) }
    }

    @Test
    fun `cuando hay registros del dia, usa el maximo mas uno`() {
        every { solicitudRepo.findMaxNumeroSolicitudByFecha(fecha) } returns "SOL-20260422-000003"

        val resultado = service.generar(fecha)

        assertEquals("SOL-20260422-000004", resultado)
        verify(exactly = 1) { solicitudRepo.findMaxNumeroSolicitudByFecha(fecha) }
    }

    @Test
    fun `cuando se borro una solicitud intermedia, no colisiona con registros existentes`() {
        // Escenario: existían SOL-20260422-000001, SOL-20260422-000002, SOL-20260422-000003
        // Se borró SOL-20260422-000002. MAX del día = 000003.
        // Enfoque anterior (COUNT): count=2 → siguiente=000003 → COLISIÓN con el registro existente.
        // Enfoque corregido  (MAX):  max=000003 → siguiente=000004 → SIN COLISIÓN.
        every { solicitudRepo.findMaxNumeroSolicitudByFecha(fecha) } returns "SOL-20260422-000003"

        val resultado = service.generar(fecha)

        assertEquals("SOL-20260422-000004", resultado)
    }
}
