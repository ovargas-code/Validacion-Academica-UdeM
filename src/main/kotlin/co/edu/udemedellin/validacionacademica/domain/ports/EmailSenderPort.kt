package co.edu.udemedellin.validacionacademica.domain.ports

interface EmailSenderPort {
    fun sendCertificateWithAttachment(
        toEmail: String,
        studentName: String,
        pdfBytes: ByteArray,
        verificationCode: String
    )
}