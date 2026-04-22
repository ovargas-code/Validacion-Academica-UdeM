package co.edu.udemedellin.validacionacademica.infrastructure.rest.exception

import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import java.sql.SQLException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `constraint idx_solicitud_numero responde con mensaje de conflicto interno`() {
        val hibernateEx = ConstraintViolationException("duplicate key", SQLException("simulated"), "idx_solicitud_numero")
        val ex = DataIntegrityViolationException("constraint violation", hibernateEx)

        val response = handler.handleDuplicateKey(ex)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(
            "No se pudo completar el registro por un conflicto interno. Por favor reintente la operación.",
            response.body?.message
        )
    }

    @Test
    fun `constraint idx_students_document responde con mensaje de documento duplicado`() {
        val hibernateEx = ConstraintViolationException("duplicate key", SQLException("simulated"), "idx_students_document")
        val ex = DataIntegrityViolationException("constraint violation", hibernateEx)

        val response = handler.handleDuplicateKey(ex)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("Ya existe un estudiante con ese documento.", response.body?.message)
    }

    @Test
    fun `constraint desconocido responde con mensaje generico de conflicto`() {
        val hibernateEx = ConstraintViolationException("duplicate key", SQLException("simulated"), "idx_otro_constraint_desconocido")
        val ex = DataIntegrityViolationException("constraint violation", hibernateEx)

        val response = handler.handleDuplicateKey(ex)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("Conflicto de datos al guardar el registro.", response.body?.message)
    }

    @Test
    fun `cause no es ConstraintViolationException responde con mensaje generico sin ClassCastException`() {
        // Escenario: el driver o la configuración produce un cause distinto a
        // org.hibernate.exception.ConstraintViolationException. El cast directo
        // lanzaría ClassCastException → 500. El pattern matching seguro devuelve el fallback.
        val cause = RuntimeException("unexpected cause from driver")
        val ex = DataIntegrityViolationException("integrity violation", cause)

        val response = handler.handleDuplicateKey(ex)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("Conflicto de datos al guardar el registro.", response.body?.message)
    }
}
