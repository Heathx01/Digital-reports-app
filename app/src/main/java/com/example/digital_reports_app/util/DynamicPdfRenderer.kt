package com.example.digital_reports_app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.digital_reports_app.domain.model.DynamicTemplate
import com.example.digital_reports_app.domain.model.FieldType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DynamicPdfRenderer {

    suspend fun renderFinalClonedPdf(
        template: DynamicTemplate,
        formValues: Map<String, String>,
        signatureBitmap: Bitmap?,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument()
            val page = PDPage(PDRectangle.LETTER) // 612 x 792 pt
            document.addPage(page)

            val contentStream = PDPageContentStream(document, page)

            // 1. Dibujar la hoja física escaneada como fondo completo
            val bgBitmap = BitmapFactory.decodeFile(template.baseImagePath)
            if (bgBitmap != null) {
                val pdBgImage = JPEGFactory.createFromImage(document, bgBitmap, 0.90f)
                contentStream.drawImage(pdBgImage, 0f, 0f, 612f, 792f)
            }

            // 2. Dibujar los textos rellenados sobre las casillas
            template.fields.forEach { field ->
                val value = formValues[field.id]
                if (!value.isNullOrBlank()) {
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                    contentStream.newLineAtOffset(field.xPt, field.yPt)
                    contentStream.showText(value)
                    contentStream.endText()
                }
            }

            // 3. Estampar la firma si fue capturada
            signatureBitmap?.let { sig ->
                val sigField = template.fields.firstOrNull { it.type == FieldType.SIGNATURE }
                val targetX = sigField?.xPt ?: 350f
                val targetY = sigField?.yPt ?: 100f
                val pdSigImage = JPEGFactory.createFromImage(document, sig, 0.85f)
                contentStream.drawImage(pdSigImage, targetX, targetY, 130f, 50f)
            }

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
