package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.ports.FileStoragePort
import co.edu.udemedellin.validacionacademica.domain.ports.SolicitudEmpresaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.NoSuchFileException

@Service
class DescargarCartaSolicitudUseCase(
    private val solicitudEmpresaRepositoryPort: SolicitudEmpresaRepositoryPort,
    private val fileStoragePort: FileStoragePort
) {
    private val log = LoggerFactory.getLogger(DescargarCartaSolicitudUseCase::class.java)

    /**
     * Carga y devuelve los bytes de la carta adjunta a la solicitud.
     *
     * @throws NoSuchElementException   si no existe solicitud con ese número → HTTP 404.
     * @throws IllegalArgumentException si el archivo físico no está disponible → HTTP 400.
     */
    @Transactional(readOnly = true)
    fun execute(numeroSolicitud: String): ByteArray {
        val solicitud = solicitudEmpresaRepositoryPort.findByNumeroSolicitud(numeroSolicitud)
            ?: throw NoSuchElementException("Solicitud no encontrada: $numeroSolicitud")
        val rutaCarta = solicitud.rutaCarta
            ?: throw IllegalArgumentException(
                "La solicitud $numeroSolicitud no tiene carta de autorizaciÃ³n adjunta."
            )

        return try {
            fileStoragePort.loadAsBytes(rutaCarta)
        } catch (e: NoSuchFileException) {
            log.error("Carta no encontrada en disco para solicitud {}: {}", numeroSolicitud, rutaCarta)
            throw IllegalArgumentException(
                "La carta asociada a la solicitud $numeroSolicitud no está disponible en el " +
                "sistema de archivos. Contacta al administrador."
            )
        }
    }
}
