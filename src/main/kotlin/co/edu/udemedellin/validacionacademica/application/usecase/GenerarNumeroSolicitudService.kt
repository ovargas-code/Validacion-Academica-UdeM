package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Genera el número de radicado único de una [co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa]
 * en el formato `SOL-YYYYMMDD-NNNNNN`, donde NNNNNN es el consecutivo diario con cero-relleno.
 *
 * El consecutivo se calcula contando los registros del día en la base de datos dentro de la
 * misma transacción que persiste la solicitud. La unicidad queda garantizada de forma definitiva
 * por la restricción UNIQUE de la columna `numero_solicitud` en la base de datos.
 *
 * Nota: ante una colisión por concurrencia extrema, la BD rechazará el segundo insert con
 * [org.springframework.dao.DataIntegrityViolationException]. Para el volumen de este sistema
 * (institución académica) esto es suficiente; si el volumen crece, migrar a una secuencia
 * dedicada por día (tabla `solicitud_consecutivo`).
 */
@Service
class GenerarNumeroSolicitudService(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort
) {
    private val log = LoggerFactory.getLogger(GenerarNumeroSolicitudService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Genera el siguiente número de solicitud para la [fecha] indicada.
     *
     * @param fecha Día calendario para el que se genera el número (normalmente [LocalDate.now]).
     * @return Cadena en formato `SOL-YYYYMMDD-NNNNNN`, ej: `SOL-20260421-000001`.
     */
    @Transactional
    fun generar(fecha: LocalDate): String {
        val count = solicitudEmpresaRepositoryPort.countByFecha(fecha)
        val consecutivo = (count + 1).toString().padStart(6, '0')
        val numero = "SOL-${fecha.format(dateFormatter)}-$consecutivo"
        log.debug("Número de solicitud generado: {}", numero)
        return numero
    }
}
