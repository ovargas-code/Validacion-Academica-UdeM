package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Caso de uso: recupera una solicitud de validación académica por empresa dado su número de radicado.
 */
@Service
class ConsultarSolicitudEmpresaUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort
) {
    private val log = LoggerFactory.getLogger(ConsultarSolicitudEmpresaUseCase::class.java)

    /**
     * Busca la solicitud con el [numeroSolicitud] indicado.
     *
     * @return La [SolicitudEmpresa] encontrada, o `null` si no existe ninguna con ese número.
     */
    @Transactional(readOnly = true)
    fun execute(numeroSolicitud: String): SolicitudEmpresa? {
        log.debug("Consultando solicitud: {}", numeroSolicitud)
        return solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
    }
}
