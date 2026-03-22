package co.edu.udemedellin.validacionacademica.domain.ports

interface PdfGeneratorPort {
    fun generateCertificate(
        studentName: String,
        studentDocument: String,
        program: String,
        verificationCode: String
    ): ByteArray
}