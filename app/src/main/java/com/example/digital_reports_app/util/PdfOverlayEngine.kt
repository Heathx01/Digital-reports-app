package com.example.digital_reports_app.util

import android.content.Context
import android.graphics.Bitmap
import com.example.digital_reports_app.domain.model.FormatoF04Config
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

    suspend fun generateClonedPdf(
        formData: Map<String, String>,
        signatureBitmap: Bitmap?,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1. Cargar el PDF escaneado base desde assets (o crear uno si no existe)
            val document = try {
                val assetStream = context.assets.open(FormatoF04Config.TEMPLATE_FILE)
                PDDocument.load(assetStream)
            } catch (_: Exception) {
                // Si el asset no existe aún, crea un documento base de 1 página A4
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

            // 3. Escribir textos en las casillas correspondientes
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

            // 4. Incrustar la firma digital en su recuadro
            signatureBitmap?.let { bitmap ->
                val coordFirma = FormatoF04Config.FIELD_POSITIONS["firma"]
                if (coordFirma != null) {
                    val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.85f)
                    // Dibuja la firma escalada (ancho: 140, alto: 60 puntos)
                    contentStream.drawImage(pdImage, coordFirma.x, coordFirma.y, 140f, 60f)
                }
            }

            // 5. Cerrar flujos y guardar el PDF generado
            contentStream.close()
            outputFile.parentFile?.mkdirs()
            document.save(FileOutputStream(outputFile))
            document.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
