package co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository

import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.SolicitudEmpresaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SolicitudEmpresaJpaRepository : JpaRepository<SolicitudEmpresaEntity, Long> {

    fun findByNumeroSolicitud(numeroSolicitud: String): SolicitudEmpresaEntity?

    /**
     * Retorna el número de solicitud (`SOL-YYYYMMDD-NNNNNN`) más alto cuyo
     * [SolicitudEmpresaEntity.createdAt] cae dentro del rango [startOfDay, endOfDay),
     * o `null` si no existe ningún registro en ese rango.
     *
     * El MAX léxico es correcto porque la fecha está fija dentro del rango y el sufijo
     * numérico tiene cero-relleno a 6 dígitos, por lo que el orden léxico coincide
     * con el orden numérico.
     *
     * Se usa un rango explícito (en lugar de DATE()) para garantizar compatibilidad
     * con H2 (dev) y PostgreSQL (producción) sin funciones específicas de cada BD.
     */
    @Query(
        "SELECT MAX(s.numeroSolicitud) FROM SolicitudEmpresaEntity s " +
        "WHERE s.createdAt >= :startOfDay AND s.createdAt < :endOfDay"
    )
    fun findMaxNumeroSolicitudByFecha(
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime
    ): String?
}
