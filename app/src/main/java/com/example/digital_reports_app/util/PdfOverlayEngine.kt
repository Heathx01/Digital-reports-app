package com.example.digital_reports_app.util

import android.content.Context
import android.graphics.Bitmap
import com.example.digital_reports_app.domain.model.FormatoF04Config
import com.example.digital_reports_app.domain.model.PageSize
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfOverlayEngine(private val context: Context) {

    var debugGrid: Boolean = false

    suspend fun generateClonedPdf(
        formData: Map<String, String>,
        signatureBitmap: Bitmap?,
        outputFile: File,
        pageSize: PageSize = PageSize.A4
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1. Cargar el PDF escaneado base desde assets (o crear uno de 1 pagina si no existe)
            val document = try {
                val assetStream = context.assets.open(FormatoF04Config.TEMPLATE_FILE)
                PDDocument.load(assetStream)
            } catch (_: Exception) {
                val doc = PDDocument()
                doc.addPage(PDPage())
                doc
            }

            val page = document.getPage(0) // Página 1 del formato

            // 2. Abrir flujo de contenido para dibujar SOBRE la hoja (modo APPEND)
            val contentStream = PDPageContentStream(
                document, 
                page, 
                PDPageContentStream.AppendMode.APPEND, 
                true, 
                true
            )

            // 3. Dibujar cuadrícula de calibración si la bandera DEBUG está activa
            if (debugGrid) {
                drawCalibrationGrid(contentStream, pageSize)
            }

            // 4. Escribir textos en las casillas correspondientes usando las coordenadas
            formData.forEach { (fieldKey, textValue) ->
                val coord = FormatoF04Config.FIELD_POSITIONS[fieldKey]
                if (coord != null && textValue.isNotBlank()) {
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, coord.fontSize)
                    contentStream.newLineAtOffset(coord.x, coord.y)
                    contentStream.showText(textValue)
                    contentStream.endText()
                }
            }

            // 5. Incrustar la firma digital en su recuadro
            signatureBitmap?.let { bitmap ->
                val coordFirma = FormatoF04Config.FIELD_POSITIONS["firma"]
                if (coordFirma != null) {
                    val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.85f)
                    contentStream.drawImage(pdImage, coordFirma.x, coordFirma.y, 140f, 60f)
                }
            }

            // 6. Cerrar flujos y guardar el PDF generado
            contentStream.close()
            outputFile.parentFile?.mkdirs()
            document.save(FileOutputStream(outputFile))
            document.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawCalibrationGrid(contentStream: PDPageContentStream, pageSize: PageSize) {
        try {
            contentStream.setFont(PDType1Font.HELVETICA, 7f)
            contentStream.setStrokingColor(0.8f, 0.8f, 0.8f)

            // Líneas verticales cada 50 puntos PostScript
            var x = 0f
            while (x <= pageSize.widthPt) {
                contentStream.moveTo(x, 0f)
                contentStream.lineTo(x, pageSize.heightPt)
                contentStream.stroke()

                contentStream.beginText()
                contentStream.newLineAtOffset(x + 2f, 10f)
                contentStream.showText("${x.toInt()}")
                contentStream.endText()
                x += 50f
            }

            // Líneas horizontales cada 50 puntos PostScript
            var y = 0f
            while (y <= pageSize.heightPt) {
                contentStream.moveTo(0f, y)
                contentStream.lineTo(pageSize.widthPt, y)
                contentStream.stroke()

                contentStream.beginText()
                contentStream.newLineAtOffset(10f, y + 2f)
                contentStream.showText("${y.toInt()}")
                contentStream.endText()
                y += 50f
            }
        } catch (_: Exception) {}
    }
}
