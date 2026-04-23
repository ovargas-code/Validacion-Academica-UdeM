package co.edu.udemedellin.validacionacademica.infrastructure.email

import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import org.slf4j.LoggerFactory

/**
 * Implementación sin operación de MailPort — ya no se registra como bean.
 * La lógica de no-op fue absorbida por MailServiceAdapter cuando mailSender == null.
 * Se conserva como referencia histórica.
 */
class NoOpMailAdapter : MailPort {

    private val logger = LoggerFactory.getLogger(NoOpMailAdapter::class.java)

    override fun enviarCertificado(emailDestino: String, nombreEstudiante: String, pdfBytes: ByteArray) {
        logger.warn(
            "Correo no configurado (MAIL_HOST no definido). Se omite envío del certificado a '{}'.",
            emailDestino
        )
    }

    override fun enviarCodigoVerificacion(
        emailDestino: String,
        nombreSolicitante: String,
        codigo: String,
        expiresInMinutes: Long
    ) {
        logger.warn(
            "Correo no configurado (MAIL_HOST no definido). Se omite envío del OTP '{}' a '{}'.",
            codigo, emailDestino
        )
    }

    override fun enviarNotificacionEnRevision(emailDestino: String, nombreContacto: String, numeroSolicitud: String) {
        logger.warn("NoOpMailAdapter: se omite notificación EN_REVISION para '{}'.", emailDestino)
    }

    override fun enviarNotificacionAprobada(
        emailDestino: String,
        nombreContacto: String,
        numeroSolicitud: String,
        pdfBytes: ByteArray
    ) {
        logger.warn("NoOpMailAdapter: se omite notificación APROBADA para '{}'.", emailDestino)
    }

    override fun enviarNotificacionRechazada(
        emailDestino: String,
        nombreContacto: String,
        numeroSolicitud: String,
        comentarioAdmin: String
    ) {
        logger.warn("NoOpMailAdapter: se omite notificación RECHAZADA para '{}'.", emailDestino)
    }
}
