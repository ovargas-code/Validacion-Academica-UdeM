package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.AcademicLevel
import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentStatus
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<ImportRowError>
)

data class ImportRowError(
    val row: Int,
    val document: String?,
    val reason: String
)

/**
 * Procesa un CSV con encabezado:
 * document,fullName,program,academicLevel,status,degreeTitle,graduationDate
 *
 * - academicLevel: TECNICO | TECNOLOGICO | PREGRADO | ESPECIALIZACION | MAESTRIA | DOCTORADO
 * - status: ACTIVO | GRADUADO | INACTIVO | SUSPENDIDO
 * - graduationDate: yyyy-MM-dd (opcional)
 */
@Service
class ImportStudentsUseCase(
    private val studentRepositoryPort: StudentRepositoryPort,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(ImportStudentsUseCase::class.java)

    fun execute(inputStream: InputStream): ImportResult {
        val errors = mutableListOf<ImportRowError>()
        var imported = 0
        var skipped = 0

        inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEachIndexed { index, rawLine ->
                val rowNumber = index + 1

                // Saltar encabezado y líneas vacías
                if (index == 0 || rawLine.isBlank()) {
                    skipped++
                    return@forEachIndexed
                }

                val cols = rawLine.split(",").map { it.trim() }

                if (cols.size < 5) {
                    errors.add(ImportRowError(rowNumber, null, "Faltan columnas (se esperan al menos 5, se recibieron ${cols.size})"))
                    return@forEachIndexed
                }

                val document = cols[0]
                val fullName = cols[1]
                val program = cols[2]
                val academicLevelRaw = cols[3]
                val statusRaw = cols[4]
                val degreeTitle = cols.getOrNull(5)?.takeIf { it.isNotBlank() }
                val graduationDateRaw = cols.getOrNull(6)?.takeIf { it.isNotBlank() }

                if (document.isBlank() || fullName.isBlank() || program.isBlank()) {
                    errors.add(ImportRowError(rowNumber, document.ifBlank { null }, "Los campos document, fullName y program son obligatorios"))
                    return@forEachIndexed
                }

                val academicLevel = try {
                    AcademicLevel.valueOf(academicLevelRaw.uppercase())
                } catch (e: IllegalArgumentException) {
                    errors.add(ImportRowError(rowNumber, document, "Nivel académico inválido: '$academicLevelRaw'"))
                    return@forEachIndexed
                }

                val status = try {
                    StudentStatus.valueOf(statusRaw.uppercase())
                } catch (e: IllegalArgumentException) {
                    errors.add(ImportRowError(rowNumber, document, "Estado inválido: '$statusRaw'"))
                    return@forEachIndexed
                }

                val graduationDate = if (graduationDateRaw != null) {
                    try {
                        LocalDate.parse(graduationDateRaw)
                    } catch (e: DateTimeParseException) {
                        errors.add(ImportRowError(rowNumber, document, "Fecha de graduación inválida: '$graduationDateRaw' (use yyyy-MM-dd)"))
                        return@forEachIndexed
                    }
                } else null

                try {
                    studentRepositoryPort.save(
                        Student(
                            document = document,
                            fullName = fullName,
                            program = program,
                            academicLevel = academicLevel,
                            status = status,
                            degreeTitle = degreeTitle,
                            graduationDate = graduationDate
                        )
                    )
                    imported++
                    log.debug("Fila {}: estudiante '{}' importado", rowNumber, document)
                } catch (e: DataIntegrityViolationException) {
                    errors.add(ImportRowError(rowNumber, document, "Documento '$document' ya existe en el sistema"))
                } catch (e: Exception) {
                    log.error("Error importando fila {}: {}", rowNumber, e.message)
                    errors.add(ImportRowError(rowNumber, document, "Error al guardar: ${e.message}"))
                }
            }
        }

        meterRegistry.counter("students.imported").increment(imported.toDouble())
        if (errors.isNotEmpty()) {
            meterRegistry.counter("students.import.errors").increment(errors.size.toDouble())
        }

        log.info("Importación CSV completada — importados: {}, omitidos: {}, errores: {}", imported, skipped, errors.size)
        return ImportResult(imported = imported, skipped = skipped, errors = errors)
    }
}
