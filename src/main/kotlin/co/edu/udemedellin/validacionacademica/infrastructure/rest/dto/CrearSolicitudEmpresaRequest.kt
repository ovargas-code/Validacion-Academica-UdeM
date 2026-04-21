package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.TipoDocumento
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Parte JSON del multipart `POST /api/v1/solicitudes-empresa`.
 *
 * El cliente debe enviar este objeto bajo la clave `datos` como un Blob JSON:
 * ```
 * formData.append("datos", new Blob([JSON.stringify(datos)], { type: "application/json" }))
 * formData.append("carta", archivoPdf)
 * ```
 */
data class CrearSolicitudEmpresaRequest(

    // ── Empresa solicitante ───────────────────────────────────────────────────

    @field:NotBlank(message = "El nombre de la empresa es obligatorio")
    @field:Size(max = 200, message = "El nombre de la empresa no puede superar 200 caracteres")
    val nombreEmpresa: String,

    @field:NotBlank(message = "El NIT de la empresa es obligatorio")
    @field:Size(max = 20, message = "El NIT no puede superar 20 caracteres")
    @field:Pattern(
        regexp = "^[0-9]{6,10}(-[0-9])?$",
        message = "El NIT debe tener entre 6 y 10 dígitos, opcionalmente seguidos de un guion y dígito de verificación"
    )
    val nitEmpresa: String,

    @field:NotBlank(message = "El nombre del contacto es obligatorio")
    @field:Size(max = 150, message = "El nombre del contacto no puede superar 150 caracteres")
    val nombreContacto: String,

    @field:NotBlank(message = "El cargo del contacto es obligatorio")
    @field:Size(max = 100, message = "El cargo no puede superar 100 caracteres")
    val cargoContacto: String,

    @field:NotBlank(message = "El correo del contacto es obligatorio")
    @field:Email(message = "El correo del contacto debe ser una dirección válida")
    @field:Size(max = 254, message = "El correo no puede superar 254 caracteres")
    val correoContacto: String,

    @field:Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    val telefonoContacto: String? = null,

    // ── Estudiante a verificar ────────────────────────────────────────────────

    @field:NotNull(message = "El tipo de documento del estudiante es obligatorio (CC, CE, PAS, TI)")
    val tipoDocumentoEstudiante: TipoDocumento,

    @field:NotBlank(message = "El número de documento del estudiante es obligatorio")
    @field:Size(min = 1, max = 20, message = "El documento debe tener entre 1 y 20 caracteres")
    @field:Pattern(
        regexp = "^[A-Za-z0-9\\-]+$",
        message = "El documento solo puede contener letras, números y guiones"
    )
    val documentoEstudiante: String,

    @field:NotBlank(message = "El nombre del estudiante es obligatorio")
    @field:Size(max = 150, message = "El nombre del estudiante no puede superar 150 caracteres")
    val nombreEstudiante: String,

    @field:Size(max = 200, message = "El programa consultado no puede superar 200 caracteres")
    val programaConsultado: String? = null,

    // ── Detalle de la solicitud ───────────────────────────────────────────────

    @field:NotNull(message = "El tipo de validación es obligatorio (DEGREE, ENROLLMENT)")
    val tipoValidacion: ValidationType,

    @field:Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    val observaciones: String? = null,

    @field:AssertTrue(message = "Debe aceptar los términos de uso y la política de tratamiento de datos")
    val aceptaTerminos: Boolean
)
