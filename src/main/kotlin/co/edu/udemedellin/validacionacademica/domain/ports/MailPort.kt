package co.edu.udemedellin.validacionacademica.domain.ports

interface MailPort {
    fun enviarCertificado(emailDestino: String, nombreEstudiante: String, pdfBytes: ByteArray)
    fun enviarCodigoVerificacion(emailDestino: String, nombreSolicitante: String, codigo: String, expiresInMinutes: Long)
}