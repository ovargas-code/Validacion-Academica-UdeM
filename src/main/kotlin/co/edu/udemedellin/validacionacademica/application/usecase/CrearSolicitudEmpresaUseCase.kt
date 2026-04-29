package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.EstadoSolicitud
import co.edu.udemedellin.validacionacademica.domain.model.SolicitudEmpresa
import co.edu.udemedellin.validacionacademica.domain.model.TipoDocumento
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import co.edu.udemedellin.validacionacademica.domain.ports.FileStoragePort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Caso de uso: recibe los datos del formulario de solicitud de validacion por empresa y guarda
 * el registro en base de datos.
 *
 * La carta de autorizacion PDF era un documento de entrada del usuario. Ya no es obligatoria;
 * si llega desde clientes antiguos se almacena como adjunto legado, pero no participa en la
 * creacion ni en la generacion del certificado final.
 *
 * Flujo:
 * 1. Valida que [CrearSolicitudEmpresaCommand.aceptaTerminos] sea `true`.
 * 2. Si se adjuntó carta, almacena el PDF en `uploads/solicitudes-empresa/{yyyy}/{MM}/{uuid}.pdf`.
 * 3. Genera el número de radicado `SOL-YYYYMMDD-NNNNNN`.
 * 4. Persiste la [SolicitudEmpresa] con estado [EstadoSolicitud.PENDIENTE].
 * 5. Retorna la entidad guardada.
 */
@Service
class CrearSolicitudEmpresaUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val generarNumeroSolicitudService: GenerarNumeroSolicitudService
) {
    private val log = LoggerFactory.getLogger(CrearSolicitudEmpresaUseCase::class.java)
    private val yearMonthFormatter = DateTimeFormatter.ofPattern("yyyy/MM")

    /**
     * Crea una nueva solicitud de validación académica por empresa.
     *
     * @param command   Datos del formulario (empresa, estudiante, tipo de validación).
     * @param autorizacionPdfStream Stream opcional legado del PDF de autorizacion de entrada.
     * @return La [SolicitudEmpresa] persistida con su [SolicitudEmpresa.numeroSolicitud] asignado.
     * @throws IllegalArgumentException si [CrearSolicitudEmpresaCommand.aceptaTerminos] es `false`.
     */
    @Transactional
    fun execute(command: CrearSolicitudEmpresaCommand, autorizacionPdfStream: InputStream?): SolicitudEmpresa {
        require(command.aceptaTerminos) {
            "La solicitud requiere aceptación de los términos de uso y política de tratamiento de datos."
        }

        // Documento de entrada legado: no es obligatorio y no es el certificado final.
        val today = LocalDate.now()
        val rutaCarta = autorizacionPdfStream?.let {
            val subPath = "solicitudes-empresa/${today.format(yearMonthFormatter)}"
            val filename = "${UUID.randomUUID()}.pdf"
            fileStoragePort.store(it, subPath, filename).also { ruta ->
                log.info("Carta almacenada en: {}", ruta)
            }
        }

        val numeroSolicitud = generarNumeroSolicitudService.generar(today)

        val solicitud = SolicitudEmpresa(
            numeroSolicitud = numeroSolicitud,
            nombreEmpresa = command.nombreEmpresa,
            nitEmpresa = command.nitEmpresa,
            nombreContacto = command.nombreContacto,
            cargoContacto = command.cargoContacto,
            correoContacto = command.correoContacto,
            telefonoContacto = command.telefonoContacto,
            tipoDocumentoEstudiante = command.tipoDocumentoEstudiante,
            documentoEstudiante = command.documentoEstudiante,
            nombreEstudiante = command.nombreEstudiante,
            programaConsultado = command.programaConsultado,
            tipoValidacion = command.tipoValidacion,
            observaciones = command.observaciones,
            aceptaTerminos = command.aceptaTerminos,
            rutaCarta = rutaCarta,
            estado = EstadoSolicitud.PENDIENTE
        )

        val saved = solicitudEmpresaRepositoryPort.save(solicitud)
        log.info(
            "Solicitud creada: numero={}, empresa={}, doc={}",
            saved.numeroSolicitud, saved.nombreEmpresa, saved.documentoEstudiante
        )
        return saved
    }
}

/**
 * Datos de entrada para [CrearSolicitudEmpresaUseCase].
 * Agrupa todos los campos del formulario excepto los generados por el sistema
 * (numeroSolicitud, rutaCarta, estado, createdAt).
 */
data class CrearSolicitudEmpresaCommand(
    // ── Empresa solicitante ───────────────────────────────────────────────────
    val nombreEmpresa: String,
    val nitEmpresa: String,
    val nombreContacto: String,
    val cargoContacto: String,
    val correoContacto: String,
    val telefonoContacto: String? = null,

    // ── Estudiante a verificar ────────────────────────────────────────────────
    val tipoDocumentoEstudiante: TipoDocumento,
    val documentoEstudiante: String,
    val nombreEstudiante: String,
    val programaConsultado: String? = null,

    // ── Detalle de la solicitud ───────────────────────────────────────────────
    val tipoValidacion: ValidationType,
    val observaciones: String? = null,
    val aceptaTerminos: Boolean
)
