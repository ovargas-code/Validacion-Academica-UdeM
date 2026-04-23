package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListarSolicitudesEmpresaUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort
) {
    /**
     * Retorna una página de solicitudes ordenada por `createdAt` DESC.
     *
     * @param page   número de página (base 0).
     * @param size   tamaño de página (1–100).
     * @param estado filtro opcional por estado; null devuelve todas.
     */
    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int, estado: EstadoSolicitud?): Page<SolicitudEmpresa> {
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )
        return solicitudEmpresaRepositoryPort.findAll(pageable, estado)
    }
}
