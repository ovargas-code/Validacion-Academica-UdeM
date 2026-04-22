package co.edu.udemedellin.validacionacademica.infrastructure.persistence.adapter

import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.SolicitudEmpresaEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.SolicitudEmpresaJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SolicitudEmpresaPersistenceAdapter(
    private val jpaRepository: SolicitudEmpresaJpaRepository
) : SolicitudEmpresaRepositoryPort {

    override fun save(solicitud: SolicitudEmpresa): SolicitudEmpresa =
        jpaRepository.save(solicitud.toEntity()).toDomain()

    override fun findByNumeroSolicitud(numero: String): SolicitudEmpresa? =
        jpaRepository.findByNumeroSolicitud(numero)?.toDomain()

    override fun findMaxNumeroSolicitudByFecha(fecha: LocalDate): String? {
        val startOfDay = fecha.atStartOfDay()
        val endOfDay = fecha.plusDays(1).atStartOfDay()
        return jpaRepository.findMaxNumeroSolicitudByFecha(startOfDay, endOfDay)
    }

    // ── Mapeos ────────────────────────────────────────────────────────────────

    private fun SolicitudEmpresa.toEntity(): SolicitudEmpresaEntity = SolicitudEmpresaEntity(
        id = id,
        numeroSolicitud = numeroSolicitud,
        nombreEmpresa = nombreEmpresa,
        nitEmpresa = nitEmpresa,
        nombreContacto = nombreContacto,
        cargoContacto = cargoContacto,
        correoContacto = correoContacto,
        telefonoContacto = telefonoContacto,
        tipoDocumentoEstudiante = tipoDocumentoEstudiante,
        documentoEstudiante = documentoEstudiante,
        nombreEstudiante = nombreEstudiante,
        programaConsultado = programaConsultado,
        tipoValidacion = tipoValidacion,
        observaciones = observaciones,
        aceptaTerminos = aceptaTerminos,
        rutaCarta = rutaCarta,
        estado = estado,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun SolicitudEmpresaEntity.toDomain(): SolicitudEmpresa = SolicitudEmpresa(
        id = id,
        numeroSolicitud = numeroSolicitud,
        nombreEmpresa = nombreEmpresa,
        nitEmpresa = nitEmpresa,
        nombreContacto = nombreContacto,
        cargoContacto = cargoContacto,
        correoContacto = correoContacto,
        telefonoContacto = telefonoContacto,
        tipoDocumentoEstudiante = tipoDocumentoEstudiante,
        documentoEstudiante = documentoEstudiante,
        nombreEstudiante = nombreEstudiante,
        programaConsultado = programaConsultado,
        tipoValidacion = tipoValidacion,
        observaciones = observaciones,
        aceptaTerminos = aceptaTerminos,
        rutaCarta = rutaCarta,
        estado = estado,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
