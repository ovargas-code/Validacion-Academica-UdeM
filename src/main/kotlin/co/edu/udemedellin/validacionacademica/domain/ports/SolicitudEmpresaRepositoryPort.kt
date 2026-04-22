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
     * Cuenta cuántas solicitudes fueron creadas en la fecha calendario indicada.
     * Usado por el generador de número de solicitud para calcular el consecutivo diario.
     */
    fun countByFecha(fecha: LocalDate): Long
}
