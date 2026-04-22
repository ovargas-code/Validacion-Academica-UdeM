package co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository

import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.SolicitudEmpresaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SolicitudEmpresaJpaRepository : JpaRepository<SolicitudEmpresaEntity, Long> {

    fun findByNumeroSolicitud(numeroSolicitud: String): SolicitudEmpresaEntity?

    /**
     * Cuenta las solicitudes cuyo [SolicitudEmpresaEntity.createdAt] cae dentro del rango
     * [startOfDay, endOfDay). Usado para calcular el consecutivo diario del número de solicitud.
     *
     * Se usa un rango explícito (en lugar de DATE()) para garantizar compatibilidad
     * con H2 (dev) y PostgreSQL (producción) sin funciones específicas de cada BD.
     */
    @Query(
        "SELECT COUNT(s) FROM SolicitudEmpresaEntity s " +
        "WHERE s.createdAt >= :startOfDay AND s.createdAt < :endOfDay"
    )
    fun countByFecha(
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime
    ): Long
}
