package com.example.digital_reports_app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.digital_reports_app.domain.model.DynamicField
import com.example.digital_reports_app.domain.model.DynamicTemplate
import com.example.digital_reports_app.domain.model.FieldType
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class TemplateAnalyzer(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Medidas estándar Carta en puntos PostScript (72 pt/pulgada)
    private val pdfPageWidth = 612f
    private val pdfPageHeight = 792f

    suspend fun analyzeAndCreateTemplate(
        imageUri: Uri,
        templateName: String
    ): Result<DynamicTemplate> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(Exception("No se pudo decodificar la imagen"))

            // 1. Guardar la imagen original en el directorio interno para usarla de fondo
            val templateFile = File(context.filesDir, "templates/${UUID.randomUUID()}_base.jpg")
            templateFile.parentFile?.mkdirs()
            FileOutputStream(templateFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // 2. Procesar OCR sobre la imagen
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText: Text = Tasks.await(recognizer.process(inputImage))

            val detectedFields = mutableListOf<DynamicField>()
            val scaleX = pdfPageWidth / bitmap.width.toFloat()
            val scaleY = pdfPageHeight / bitmap.height.toFloat()

            // 3. Evaluar bloques de texto detectados
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val rawText = line.text.trim()
                    val box = line.boundingBox ?: continue

                    if (isLabelCandidate(rawText)) {
                        val cleanId = rawText.lowercase().replace(Regex("[^a-z0-9]"), "_")
                        
                        // Coordenada X: Justo después de la etiqueta física
                        val xPt = (box.right.toFloat() + 8f) * scaleX
                        
                        // Coordenada Y en PDF: El origen (0,0) está en la esquina inferior izquierda
                        val yPt = pdfPageHeight - (box.bottom.toFloat() * scaleY)

                        val fieldType = when {
                            rawText.contains("FECHA", ignoreCase = true) -> FieldType.DATE
                            rawText.contains("HORA", ignoreCase = true) || 
                            rawText.contains("CANTIDAD", ignoreCase = true) ||
                            rawText.contains("NUMERO", ignoreCase = true) -> FieldType.NUMBER
                            rawText.contains("FIRMA", ignoreCase = true) -> FieldType.SIGNATURE
                            else -> FieldType.TEXT
                        }

                        detectedFields.add(
                            DynamicField(
                                id = cleanId,
                                label = rawText.replace(":", "").trim(),
                                type = fieldType,
                                xPt = xPt.coerceIn(20f, pdfPageWidth - 100f),
                                yPt = yPt.coerceIn(40f, pdfPageHeight - 40f)
                            )
                        )
                    }
                }
            }

            val template = DynamicTemplate(
                templateId = UUID.randomUUID().toString(),
                templateName = templateName,
                baseImagePath = templateFile.absolutePath,
                fields = detectedFields
            )

            Result.success(template)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isLabelCandidate(text: String): Boolean {
        val keywords = listOf(":", "NOMBRE", "FECHA", "HORA", "TURNO", "OPERADOR", "CANTIDAD", "FIRMA", "OBSERVACION", "FOLIO", "ESTADO", "UBICACION")
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
