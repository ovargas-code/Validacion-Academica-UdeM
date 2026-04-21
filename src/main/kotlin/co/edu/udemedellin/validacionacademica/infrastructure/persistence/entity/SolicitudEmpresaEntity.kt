package co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity

import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.TipoDocumento
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "solicitudes_empresa",
    indexes = [
        Index(name = "idx_solicitud_numero", columnList = "numero_solicitud", unique = true),
        Index(name = "idx_solicitud_created_at", columnList = "created_at"),
        Index(name = "idx_solicitud_documento_estudiante", columnList = "documento_estudiante")
    ]
)
class SolicitudEmpresaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "numero_solicitud", nullable = false, unique = true, length = 25)
    var numeroSolicitud: String = "",

    // ── Empresa solicitante ───────────────────────────────────────────────────

    @Column(name = "nombre_empresa", nullable = false, length = 200)
    var nombreEmpresa: String = "",

    @Column(name = "nit_empresa", nullable = false, length = 20)
    var nitEmpresa: String = "",

    @Column(name = "nombre_contacto", nullable = false, length = 150)
    var nombreContacto: String = "",

    @Column(name = "cargo_contacto", nullable = false, length = 100)
    var cargoContacto: String = "",

    @Column(name = "correo_contacto", nullable = false, length = 254)
    var correoContacto: String = "",

    @Column(name = "telefono_contacto", length = 20)
    var telefonoContacto: String? = null,

    // ── Estudiante a verificar ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento_estudiante", nullable = false, length = 10)
    var tipoDocumentoEstudiante: TipoDocumento = TipoDocumento.CC,

    @Column(name = "documento_estudiante", nullable = false, length = 20)
    var documentoEstudiante: String = "",

    @Column(name = "nombre_estudiante", nullable = false, length = 150)
    var nombreEstudiante: String = "",

    @Column(name = "programa_consultado", length = 200)
    var programaConsultado: String? = null,

    // ── Detalle de la solicitud ───────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_validacion", nullable = false, length = 20)
    var tipoValidacion: ValidationType = ValidationType.DEGREE,

    @Column(name = "observaciones", length = 500)
    var observaciones: String? = null,

    @Column(name = "acepta_terminos", nullable = false)
    var aceptaTerminos: Boolean = false,

    // ── Archivo adjunto ───────────────────────────────────────────────────────

    @Column(name = "ruta_carta", nullable = false, length = 500)
    var rutaCarta: String = "",

    // ── Trazabilidad ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    var estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
