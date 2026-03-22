package co.edu.udemedellin.validacionacademica.infrastructure.documents

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.model.ValidationResult
import co.edu.udemedellin.validacionacademica.domain.ports.DocumentGeneratorPort
import org.springframework.stereotype.Component

@Component
class SimpleTextDocumentGeneratorAdapter : DocumentGeneratorPort {
    override fun generateLetter(
        request: ValidationRequest,
        student: Student?,
        result: ValidationResult
    ): String {
        return buildString {
            appendLine("UNIVERSIDAD - VALIDACIÓN ACADÉMICA")
            appendLine("Código de control: ${result.controlCode}")
            appendLine("Solicitante: ${request.requesterName} - ${request.requesterEmail}")
            appendLine("Documento consultado: ${request.studentDocument}")
            appendLine("Tipo de validación: ${request.validationType}")
            appendLine("Fecha: ${result.generatedAt}")
            appendLine()
            if (student != null) {
                appendLine("Nombre: ${student.fullName}")
                appendLine("Programa: ${student.program}")
                appendLine("Estado: ${student.status}")
                appendLine("Nivel académico: ${student.academicLevel}")
                student.degreeTitle?.let { appendLine("Título: $it") }
                student.graduationDate?.let { appendLine("Fecha de grado: $it") }
                appendLine()
            }
            appendLine(result.message)
        }
    }
}
