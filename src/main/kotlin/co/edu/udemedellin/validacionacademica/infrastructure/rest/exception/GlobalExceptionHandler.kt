package co.edu.udemedellin.validacionacademica.infrastructure.rest.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

// Este es el formato estándar que devolveremos en todos los errores
data class ApiError(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String> = emptyList()
)

// @RestControllerAdvice intercepta las excepciones de TODOS los controllers
@RestControllerAdvice
class GlobalExceptionHandler {

    // Cuando un campo @NotBlank o @Email falla la validación
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        // Recolectamos todos los mensajes de error de cada campo
        val details = ex.bindingResult.fieldErrors.map { error ->
            "Campo '${error.field}': ${error.defaultMessage}"
        }
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Error de validación",
            message = "Uno o más campos tienen valores incorrectos",
            details = details
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    // Cuando se intenta guardar un estudiante que ya existe (documento duplicado)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException::class)
    fun handleDuplicateKey(ex: Exception): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.CONFLICT.value(),
            error = "Conflicto de datos",
            message = "Ya existe un registro con ese documento"
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError)
    }

    // Cuando un argumento es inválido (por ejemplo, un enum que no existe)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Argumento inválido",
            message = ex.message ?: "Valor no permitido"
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    // Captura cualquier otro error inesperado para no exponer el stack trace
    @ExceptionHandler(Exception::class)
    fun handleGenericError(ex: Exception): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Error interno del servidor",
            message = "Ocurrió un error inesperado. Por favor contacte al administrador."
        )
        return ResponseEntity.internalServerError().body(apiError)
    }
}
