package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import java.time.LocalDate

/**
 * Puerto de salida para la persistencia de [SolicitudEmpresa].
 */
interface SolicitudEmpresaRepositoryPort {

    /**
     * Persiste una solicitud y retorna la instancia guardada con su ID asignado.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException si ya existe una solicitud
     * con el mismo [SolicitudEmpresa.numeroSolicitud].
     */
    fun save(solicitud: SolicitudEmpresa): SolicitudEmpresa

    /**
     * Busca una solicitud por su número de radicado.
     *
     * @return la solicitud encontrada, o `null` si no existe ningún registro con ese número.
     */
    fun findByNumeroSolicitud(numero: String): SolicitudEmpresa?

    /**
     * Retorna el número de solicitud más alto (`SOL-YYYYMMDD-NNNNNN`) creado en la fecha
     * calendario indicada, o `null` si no existe ningún registro para ese día.
     * Usado por el generador de número de solicitud para calcular el consecutivo diario
     * de forma segura ante borrados (MAX es inmune a gaps, COUNT no lo es).
     */
    fun findMaxNumeroSolicitudByFecha(fecha: LocalDate): String?

    /**
     * Actualiza una solicitud existente (debe tener [SolicitudEmpresa.id] no nulo).
     * Usado para cambios de estado administrativos.
     */
    fun update(solicitud: SolicitudEmpresa): SolicitudEmpresa
}
