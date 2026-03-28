package co.edu.udemedellin.validacionacademica.infrastructure.rest.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ApiError(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String> = emptyList()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "No autorizado",
            message = "Credenciales incorrectas"
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
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

    // Cuando Jackson no puede deserializar el body (enum inválido, formato incorrecto, etc.)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        val message = when (val cause = ex.cause) {
            is InvalidFormatException -> {
                val targetType = cause.targetType
                if (targetType != null && targetType.isEnum) {
                    val validValues = targetType.enumConstants
                        .joinToString(", ") { (it as Enum<*>).name }
                    "Valor '${cause.value}' no es válido para el campo '${cause.path.lastOrNull()?.fieldName}'. " +
                            "Valores permitidos: $validValues"
                } else {
                    "Formato de datos inválido: se esperaba ${targetType?.simpleName ?: "otro tipo"}"
                }
            }
            else -> "El cuerpo de la solicitud está mal formado o tiene un formato inesperado"
        }
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Solicitud inválida",
            message = message
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException::class)
    fun handleDuplicateKey(ex: Exception): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.CONFLICT.value(),
            error = "Conflicto de datos",
            message = "Ya existe un registro con ese documento"
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Argumento inválido",
            message = ex.message ?: "Valor no permitido"
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiError> {
        val details = ex.constraintViolations.map { violation ->
            val field = violation.propertyPath.toList().lastOrNull()?.name ?: violation.propertyPath.toString()
            "$field: ${violation.message}"
        }
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Parámetro inválido",
            message = "Uno o más parámetros de la solicitud tienen valores incorrectos",
            details = details
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericError(ex: Exception): ResponseEntity<ApiError> {
        log.error("Error inesperado: ${ex.javaClass.simpleName} — ${ex.message}", ex)
        val apiError = ApiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Error interno del servidor",
            message = "Ocurrió un error inesperado. Por favor contacte al administrador."
        )
        return ResponseEntity.internalServerError().body(apiError)
    }
}
