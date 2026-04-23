package co.edu.udemedellin.validacionacademica.domain.ports

/**
 * Puerto de salida para la generación de certificados académicos en formato PDF.
 *
 * La implementación ([PdfDocumentGeneratorAdapter]) usa OpenPDF y ZXing para
 * incrustar un código QR que apunta al endpoint público de verificación.
 */
interface PdfGeneratorPort {

    /**
     * Genera el PDF del certificado académico y lo retorna como arreglo de bytes.
     *
     * @param studentName nombre completo del estudiante que aparecerá en el certificado.
     * @param studentDocument número de documento del estudiante.
     * @param program nombre del programa académico.
     * @param verificationUrl URL completa que se codifica en el QR del certificado.
     *   El puerto no construye la URL; la recibe lista del caller.
     * @return contenido del PDF generado listo para enviar o descargar.
     * @throws Exception si la generación del PDF falla (error de plantilla, recursos faltantes, etc.).
     */
    fun generateCertificate(
        studentName: String,
        studentDocument: String,
        program: String,
        verificationUrl: String
    ): ByteArray
}
