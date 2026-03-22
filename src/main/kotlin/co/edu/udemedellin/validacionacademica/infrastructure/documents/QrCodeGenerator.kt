package co.edu.udemedellin.validacionacademica.infrastructure.documents

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream

object QrCodeGenerator {

    fun generate(text: String, width: Int = 200, height: Int = 200): ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)

        val pngOutputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream)

        return pngOutputStream.toByteArray()
    }
}