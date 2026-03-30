package co.edu.udemedellin.validacionacademica.domain.ports

/**
 * Puerto de salida para el envío de correos electrónicos.
 *
 * Las implementaciones deben ser tolerantes a fallos transitorios de SMTP.
 * Si el envío falla, deben lanzar una excepción no verificada para que la capa
 * de aplicación decida si reintenta o degrada la operación.
 *
 * En entornos de desarrollo sin configuración SMTP se usa [NoOpMailAdapter],
 * que registra el mensaje en el log sin intentar el envío real.
 */
interface MailPort {

    /**
     * Envía el certificado académico en PDF al correo del estudiante.
     *
     * @param emailDestino dirección de correo del estudiante receptor.
     * @param nombreEstudiante nombre completo del estudiante, usado en el cuerpo del mensaje.
     * @param pdfBytes contenido del certificado PDF como arreglo de bytes.
     * @throws Exception si el envío SMTP falla de forma no recuperable.
     */
    fun enviarCertificado(emailDestino: String, nombreEstudiante: String, pdfBytes: ByteArray)

    /**
     * Envía el código OTP de verificación al correo del solicitante.
     *
     * @param emailDestino dirección de correo del solicitante.
     * @param nombreSolicitante nombre del solicitante, usado en el saludo del mensaje.
     * @param codigo código OTP de 6 dígitos generado para esta solicitud.
     * @param expiresInMinutes tiempo de validez del código en minutos.
     * @throws Exception si el envío SMTP falla de forma no recuperable.
     */
    fun enviarCodigoVerificacion(emailDestino: String, nombreSolicitante: String, codigo: String, expiresInMinutes: Long)
}
