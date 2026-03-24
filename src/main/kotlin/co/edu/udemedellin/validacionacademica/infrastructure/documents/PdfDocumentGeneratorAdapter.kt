package co.edu.udemedellin.validacionacademica.infrastructure.documents

import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import com.lowagie.text.*
import com.lowagie.text.pdf.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.awt.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter

@Component
class PdfDocumentGeneratorAdapter : PdfGeneratorPort {

    private val logger = LoggerFactory.getLogger(PdfDocumentGeneratorAdapter::class.java)

    override fun generateCertificate(
        studentName: String,
        studentDocument: String,
        program: String,
        verificationCode: String
    ): ByteArray {
        val out      = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 0f, 0f, 0f, 0f)
        val writer   = PdfWriter.getInstance(document, out)
        document.open()

        val page   = PageSize.A4
        val canvas = writer.directContent
        val cx     = page.width / 2f

        val red   = Color(200, 16, 46)
        val black = Color(20, 20, 20)
        val gray  = Color(248, 248, 248)

        val fTitle      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    26f,  Font.NORMAL, red)
        val fSubtitle   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    15f,  Font.NORMAL, black)
        val fBody       = FontFactory.getFont(FontFactory.HELVETICA,         13f,  Font.NORMAL, black)
        val fMono       = FontFactory.getFont(FontFactory.COURIER,           12f,  Font.NORMAL, black)
        val fMonoB      = FontFactory.getFont(FontFactory.COURIER_BOLD,      12f,  Font.NORMAL, black)
        val fName       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    18f,  Font.NORMAL, black)
        val fProgram    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    15f,  Font.NORMAL, black)
        val fSigner     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    12f,  Font.NORMAL, black)
        val fSignerRole = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    11f,  Font.NORMAL, black)
        val fFooter     = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  9f,  Font.NORMAL, black)
        val fVerif      = FontFactory.getFont(FontFactory.HELVETICA,          8f,  Font.NORMAL, black)

        // 1. FONDO
        canvas.saveState()
        canvas.setColorFill(gray)
        canvas.rectangle(0f, 0f, page.width, page.height)
        canvas.fill()
        canvas.restoreState()

        // Helper: escala manteniendo proporción y retorna (w, h)
        fun scaleFit(imgW: Float, imgH: Float, maxW: Float, maxH: Float): Pair<Float, Float> {
            val ratio = minOf(maxW / imgW, maxH / imgH)
            return Pair(imgW * ratio, imgH * ratio)
        }

        // 2. CABEZOTE
        loadImage("cabezote_rojo.png")?.let { bytes ->
            val img = Image.getInstance(bytes)
            val (w, h) = scaleFit(img.width, img.height, page.width * 0.44f, page.height * 0.14f)
            canvas.addImage(img, w, 0f, 0f, h, 0f, page.height - h)
        }

        // 3. LOGO
        loadImage("logo_udem.png")?.let { bytes ->
            val img = Image.getInstance(bytes)
            val (w, h) = scaleFit(img.width, img.height, 200f, 76f)
            canvas.addImage(img, w, 0f, 0f, h, (page.width - w) / 2f, 648f)
        }

        // 4. TEXTOS
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("CERTIFICADO DE GRADUADO", fTitle), cx, 594f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("LA COORDINACIÓN DE ADMISIONES Y REGISTRO DE LA", fSubtitle), cx, 553f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("UNIVERSIDAD DE MEDELLÍN,", fSubtitle), cx, 532f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("HACE CONSTAR QUE:", fBody), cx, 484f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase(studentName.uppercase(), fName), cx, 442f, 0f)

        val body = Phrase().apply {
            add(Chunk("Identificado(a) con documento de identidad No. ", fMono))
            add(Chunk(studentDocument, fMonoB))
            add(Chunk(", cumplió satisfactoriamente los requisitos exigidos para optar al título de:", fMono))
        }
        val ct = ColumnText(canvas)
        ct.setSimpleColumn(80f, 350f, page.width - 80f, 418f, 22f, Element.ALIGN_JUSTIFIED)
        ct.addText(body)
        ct.go()

        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase(program.uppercase(), fProgram), cx, 306f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase(buildDateText(), fBody), cx, 224f, 0f)

        // 5. FIRMA
        val sigCx = 210f
        loadImage("Firma_Sandra.png")?.let { bytes ->
            val img = Image.getInstance(bytes)
            val (w, h) = scaleFit(img.width, img.height, 175f, 62f)
            canvas.addImage(img, w, 0f, 0f, h, sigCx - w / 2f, 130f)
        }

        // Línea de firma
        canvas.saveState()
        canvas.setLineWidth(0.85f)
        canvas.moveTo(96f, 127f)
        canvas.lineTo(324f, 127f)
        canvas.stroke()
        canvas.restoreState()

        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("SANDRA PATRICIA GIRALDO MONTOYA", fSigner), sigCx, 107f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("Coordinadora", fSignerRole), sigCx, 88f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("Coordinación De Admisiones Y Registro", fSignerRole), sigCx, 70f, 0f)

        // 6. QR
        try {
            val bits = QRCodeWriter().encode(
                "https://validador.udem.edu.co/verify/$verificationCode",
                BarcodeFormat.QR_CODE, 240, 240
            )
            val qrStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bits, "PNG", qrStream)
            val qr = Image.getInstance(qrStream.toByteArray())
            val (w, h) = scaleFit(qr.width, qr.height, 94f, 94f)
            canvas.addImage(qr, w, 0f, 0f, h, 454f, 90f)
        } catch (e: Exception) { logger.warn("Error generando código QR para el certificado: {}", e.message) }

        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
            Phrase("Verificación: $verificationCode", fVerif), 440f, 72f, 0f)

        // 7. PIE
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("Este documento es una copia auténtica. Verifique en validador.udem.edu.co", fFooter), cx, 38f, 0f)
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
            Phrase("Universidad de Medellín - Vigilada Mineducación.", fFooter), cx, 24f, 0f)

        document.close()
        return out.toByteArray()
    }

    private fun loadImage(filename: String): ByteArray? {
        val paths = listOf(
            "/static/images/$filename",
            "/images/$filename",
            "/$filename",
            "static/images/$filename"
        )
        for (path in paths) {
            val stream = javaClass.getResourceAsStream(path)
                ?: this::class.java.classLoader.getResourceAsStream(path)
            if (stream != null) {
                logger.debug("Imagen '{}' cargada desde '{}'", filename, path)
                return stream.readBytes()
            }
        }
        logger.warn("Imagen '{}' no encontrada en ninguna ruta de recursos. El certificado se generará sin ella.", filename)
        return null
    }

    private fun buildDateText(): String {
        val locale = Locale("es", "CO")
        val cal    = Calendar.getInstance(locale).apply { time = Date() }
        val day    = cal.get(Calendar.DAY_OF_MONTH)
        val month  = SimpleDateFormat("MMMM", locale).format(cal.time)
        val year   = cal.get(Calendar.YEAR)
        return "Se expide en Medellín, a los $day días del mes de $month de $year."
    }
}