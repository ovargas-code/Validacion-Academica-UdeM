package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.model.ValidationResult

/**
 * Puerto de salida para la generación del documento textual de respuesta a una solicitud de validación.
 *
 * A diferencia de [PdfGeneratorPort], este puerto genera la carta o resolución en formato
 * de texto/HTML que describe el resultado de la validación académica.
 */
interface DocumentGeneratorPort {

    /**
     * Genera el contenido textual de la carta de validación.
     *
     * @param request solicitud de validación con los datos del solicitante y el tipo de trámite.
     * @param student datos del estudiante al momento de la generación; puede ser `null` si el
     *   estudiante no fue encontrado en el sistema, en cuyo caso la carta debe indicar la ausencia de registro.
     * @param result resultado de la validación que determina el contenido y tono del documento.
     * @return contenido de la carta como cadena de texto (puede ser texto plano o HTML).
     */
    fun generateLetter(request: ValidationRequest, student: Student?, result: ValidationResult): String
}
