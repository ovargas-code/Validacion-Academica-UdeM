package co.edu.udemedellin.validacionacademica.infrastructure.email

import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(JavaMailSender::class)
class MailServiceAdapter(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username:}") private val configuredFrom: String,
    @Value("\${spring.mail.host:}") private val configuredHost: String,
    @Value("\${spring.mail.port:0}") private val configuredPort: Int
) : MailPort {

    private val logger = LoggerFactory.getLogger(MailServiceAdapter::class.java)

    override fun enviarCertificado(emailDestino: String, nombreEstudiante: String, pdfBytes: ByteArray) {
        require(emailDestino.isNotBlank()) { "El correo destino no puede estar vacío" }
        require(pdfBytes.isNotEmpty()) { "El PDF del certificado llegó vacío" }

        val normalizedFrom = configuredFrom.trim()
        if (normalizedFrom.isBlank() || normalizedFrom.contains("su_correo@") || normalizedFrom.contains("tu-correo@")) {
            throw IllegalStateException(
                "El correo no está configurado. Cree el archivo .env con MAIL_HOST, MAIL_PORT, MAIL_USERNAME y MAIL_PASSWORD reales."
            )
        }

        logger.info(
            "Preparando envío SMTP host={} puerto={} from={} destino={} estudiante={}",
            configuredHost,
            configuredPort,
            normalizedFrom,
            emailDestino,
            nombreEstudiante
        )

        val mensaje: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mensaje, true, Charsets.UTF_8.name())

        helper.setTo(emailDestino)
        helper.setFrom(normalizedFrom)
        helper.setReplyTo(normalizedFrom)
        helper.setSubject("Certificado de Graduado - Universidad de Medellín")

        val cuerpoHtml = """
            <div style="font-family: Arial, sans-serif; border-top: 5px solid #C8102E; padding: 20px; color: #333;">
                <h2 style="color: #C8102E;">¡Cordial saludo, $nombreEstudiante!</h2>
                <p>Adjunto encontrará su <b>Certificado de Graduado</b> oficial, emitido por la Universidad de Medellín.</p>
                <p>Este documento cuenta con firma digital y validación institucional mediante código QR.</p>
                <br>
                <p>Atentamente,</p>
                <p><b>Universidad de Medellín</b><br>
                Sistema de Validación Académica</p>
            </div>
        """.trimIndent()

        helper.setText(cuerpoHtml, true)

        val nombreArchivo = "Certificado_${nombreEstudiante.replace(" ", "_")}.pdf"
        helper.addAttachment(nombreArchivo, ByteArrayResource(pdfBytes))

        mailSender.send(mensaje)
        logger.info("Certificado enviado correctamente a {}", emailDestino)
    }
}
