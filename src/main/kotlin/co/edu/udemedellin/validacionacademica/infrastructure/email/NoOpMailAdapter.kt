package co.edu.udemedellin.validacionacademica.infrastructure.email

import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/**
 * Implementación sin operación de MailPort que se activa cuando JavaMailSender
 * no está disponible en el contexto (es decir, cuando el correo no está configurado).
 * Registra una advertencia para que el operador sea consciente de que el envío fue omitido.
 */
@Component
@ConditionalOnMissingBean(JavaMailSender::class)
class NoOpMailAdapter : MailPort {

    private val logger = LoggerFactory.getLogger(NoOpMailAdapter::class.java)

    override fun enviarCertificado(emailDestino: String, nombreEstudiante: String, pdfBytes: ByteArray) {
        logger.warn(
            "El servidor de correo no está configurado (MAIL_HOST no definido). " +
                    "Se omite el envío del certificado a '{}'. " +
                    "Configure las variables MAIL_HOST, MAIL_PORT, MAIL_USERNAME y MAIL_PASSWORD en el archivo .env.",
            emailDestino
        )
    }
}
