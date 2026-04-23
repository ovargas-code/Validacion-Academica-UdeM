package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Caso de uso: consulta pública del estado de una solicitud de empresa por número de radicado.
 *
 * Devuelve el modelo de dominio completo. La decisión de qué campos exponer
 * públicamente es responsabilidad del DTO ([ConsultaPublicaSolicitudResponse])
 * y su función de extensión [toPublicResponse].
 */
@Service
class ConsultarEstadoPublicoSolicitudUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort
) {
    private val log = LoggerFactory.getLogger(ConsultarEstadoPublicoSolicitudUseCase::class.java)

    /**
     * @return la solicitud encontrada, o `null` si no existe ninguna con ese número.
     */
    @Transactional(readOnly = true)
    fun execute(numeroSolicitud: String): SolicitudEmpresa? {
        log.debug("Consulta pública de estado: {}", numeroSolicitud)
        return solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
    }
}
