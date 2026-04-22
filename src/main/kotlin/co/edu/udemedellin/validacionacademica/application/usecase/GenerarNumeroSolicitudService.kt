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
 * El consecutivo se calcula a partir del MAX de `numero_solicitud` del día en la base de datos,
 * parseando los últimos 6 dígitos y sumando 1. Esto lo hace inmune a gaps provocados por borrados:
 * si existían [1, 2, 3] y se borró el 2, MAX=3 → siguiente=4 (sin colisión).
 * El enfoque anterior basado en COUNT generaría 3 → colisión con el registro existente.
 *
 * La unicidad queda garantizada de forma definitiva por la restricción UNIQUE de la columna
 * `numero_solicitud`. Ante colisión por concurrencia extrema, la BD rechazará el segundo
 * insert con [org.springframework.dao.DataIntegrityViolationException].
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
        val maxNumero = solicitudEmpresaRepositoryPort.findMaxNumeroSolicitudByFecha(fecha)
        val siguienteConsecutivo = if (maxNumero == null) {
            1L
        } else {
            maxNumero.takeLast(6).toLong() + 1
        }
        val numero = "SOL-${fecha.format(dateFormatter)}-${siguienteConsecutivo.toString().padStart(6, '0')}"
        log.debug("Número de solicitud generado: {} (basado en MAX={})", numero, maxNumero)
        return numero
    }
}
