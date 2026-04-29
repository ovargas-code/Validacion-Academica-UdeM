package co.edu.udemedellin.validacionacademica.infrastructure.rest.exception

import co.edu.udemedellin.validacionacademica.domain.model.InvalidStateTransitionException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
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
        log.warn("Error de validacion en request: {}", ex.message, ex)
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

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingServletRequestPart(ex: MissingServletRequestPartException): ResponseEntity<ApiError> {
        log.warn("Parte multipart faltante: {}", ex.requestPartName, ex)
        val message = when (ex.requestPartName) {
            "datos" -> "La parte multipart 'datos' es obligatoria y debe enviarse como application/json"
            "carta" -> "La parte multipart 'carta' es opcional. Si aparece este error, el backend desplegado no tiene la version actualizada."
            else -> "Falta la parte multipart '${ex.requestPartName}'"
        }
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Solicitud multipart invalida",
            message = message
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(MultipartException::class)
    fun handleMultipartException(ex: MultipartException): ResponseEntity<ApiError> {
        log.warn("Error procesando multipart/form-data", ex)
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Solicitud multipart invalida",
            message = "No se pudo procesar el formulario multipart. Verifique que la parte 'datos' sea JSON valido y que los archivos adjuntos, si se envian, tengan un formato permitido."
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiError> {
        log.warn("Tipo de parametro invalido: {}", ex.message, ex)
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Parametro invalido",
            message = "El valor '${ex.value}' no es valido para el parametro '${ex.name}'"
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException): ResponseEntity<ApiError> {
        log.warn("Content-Type no soportado: {}", ex.message, ex)
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Content-Type no soportado",
            message = "El formulario debe enviarse como multipart/form-data y la parte 'datos' debe enviarse como application/json"
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    // Cuando Jackson no puede deserializar el body (enum inválido, formato incorrecto, etc.)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        log.warn("Cuerpo de solicitud no legible: {}", ex.message, ex)
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

    @ExceptionHandler(HttpMessageConversionException::class)
    fun handleHttpMessageConversion(ex: HttpMessageConversionException): ResponseEntity<ApiError> {
        log.warn("Error convirtiendo el cuerpo de la solicitud", ex)
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Solicitud invalida",
            message = "El cuerpo de la solicitud no coincide con el formato esperado"
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException::class)
    fun handleDuplicateKey(ex: org.springframework.dao.DataIntegrityViolationException): ResponseEntity<ApiError> {
        log.error("Error de integridad al guardar datos", ex)
        val rootMessage = generateSequence(ex as Throwable) { it.cause }
            .lastOrNull()
            ?.message
            .orEmpty()
        val message = when (val cause = ex.cause) {
            is org.hibernate.exception.ConstraintViolationException -> when (cause.constraintName) {
                "idx_solicitud_numero" ->
                    "No se pudo completar el registro por un conflicto interno. Por favor reintente la operación."
                "idx_students_document" ->
                    "Ya existe un estudiante con ese documento."
                else -> integrityMessage(rootMessage)
            }
            else -> integrityMessage(rootMessage)
        }
        val apiError = ApiError(
            status = HttpStatus.CONFLICT.value(),
            error = "Conflicto de datos",
            message = message
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError)
    }

    @ExceptionHandler(InvalidStateTransitionException::class)
    fun handleInvalidStateTransition(ex: InvalidStateTransitionException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.CONFLICT.value(),
            error = "Transición de estado inválida",
            message = ex.message ?: "La operación no está permitida en el estado actual de la solicitud"
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Recurso no encontrado",
            message = ex.message ?: "El recurso solicitado no existe"
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        log.warn("Argumento invalido: {}", ex.message, ex)
        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Argumento inválido",
            message = ex.message ?: "Valor no permitido"
        )
        return ResponseEntity.badRequest().body(apiError)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiError> {
        log.warn("Violacion de constraints en parametros: {}", ex.message, ex)
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

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(ex: MaxUploadSizeExceededException): ResponseEntity<ApiError> {
        log.warn("Archivo demasiado grande", ex)
        val apiError = ApiError(
            status = HttpStatus.PAYLOAD_TOO_LARGE.value(),
            error = "Archivo demasiado grande",
            message = "El archivo supera el tamaño máximo permitido (10 MB por archivo, 15 MB por solicitud)"
        )
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(apiError)
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

    private fun integrityMessage(rootMessage: String): String =
        if (rootMessage.contains("ruta_carta", ignoreCase = true)) {
            "La base de datos aun exige la columna ruta_carta. Aplique la migracion V4 para permitir solicitudes sin PDF."
        } else {
            "Conflicto de datos al guardar el registro."
        }
}
