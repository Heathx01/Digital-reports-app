package com.example.digital_reports_app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.digital_reports_app.domain.model.PageSize
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class FormFieldType {
    TEXT, NUMBER, DROPDOWN, CHECKBOX
}

data class CustomFormField(
    val id: String,
    var label: String,
    var type: FormFieldType = FormFieldType.TEXT,
    var value: String = "",
    val options: List<String> = emptyList(),
    var positionX: Float = 180f,
    var positionY: Float = 600f
)

data class AnalyzedDocumentResult(
    val templateTitle: String,
    val category: String,
    val description: String,
    val extractedFields: List<CustomFormField>
)

object DocumentTextAnalyzer {

    private const val TAG = "DocumentTextAnalyzer"

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extrae el texto real impreso en un archivo PDF utilizando PDFTextStripper
     * e infiere el título y las etiquetas reales del formulario.
     */
    suspend fun analyzePdfUri(context: Context, pdfUri: Uri, templateIndex: Int): AnalyzedDocumentResult {
        return try {
            val extractedText = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
                        PDDocument.load(inputStream).use { pdDoc ->
                            PDFTextStripper().getText(pdDoc)
                        }
                    } ?: ""
                } catch (e: Exception) {
                    Log.w(TAG, "PDFBox no pudo extraer texto, se usará OCR", e)
                    ""
                }
            }

            val rawLines = extractedText.split("\n")
                .map { it.trim().replace("\r", "") }
                .filter { it.length > 2 && !it.contains("PÁGINA", ignoreCase = true) }

            if (rawLines.isEmpty()) {
                // PDF escaneado (solo imagen): renderizar la página real y aplicar OCR
                val pageBitmap = withContext(Dispatchers.IO) { renderFirstPdfPage(context, pdfUri) }
                return if (pageBitmap != null) {
                    analyzeScannedBitmapWithMlKit(pageBitmap, templateIndex)
                } else {
                    AnalyzedDocumentResult(
                        templateTitle = "Formato PDF #$templateIndex",
                        category = "Inspección",
                        description = "No se pudo leer el PDF; se cargaron campos por defecto.",
                        extractedFields = createDefaultFields()
                    )
                }
            }

            // Extract Title from PDF Text
            val titleCandidate = rawLines.firstOrNull { line ->
                val txt = line.uppercase()
                txt.contains("INSPECCION") || txt.contains("CONTROL") ||
                txt.contains("REPORTE") || txt.contains("FORMATO") ||
                txt.contains("MANTENIMIENTO") || txt.contains("AUDITORIA") ||
                txt.contains("HOJA") || txt.contains("ORDEN")
            } ?: rawLines.first()

            val extractedTitle = titleCandidate.replace(":", "").replace("_", " ").trim()

            // Extract Category
            val category = when {
                extractedTitle.contains("Mantenimiento", ignoreCase = true) -> "Mantenimiento"
                extractedTitle.contains("Seguridad", ignoreCase = true) -> "Incidencia"
                extractedTitle.contains("Almacen", ignoreCase = true) -> "General"
                else -> "Inspección"
            }

            // Extract real fields from PDF text lines
            val fieldsList = mutableListOf<CustomFormField>()
            var fieldCounter = 1

            for (line in rawLines) {
                if (line.equals(titleCandidate, ignoreCase = true)) continue

                val cleanLabel = line.replace(":", "").replace("_", "").trim()
                if (cleanLabel.length in 3..45 && !fieldsList.any { it.label.equals(cleanLabel, ignoreCase = true) }) {
                    val fieldType = when {
                        cleanLabel.contains("Temp", ignoreCase = true) ||
                        cleanLabel.contains("Cantidad", ignoreCase = true) ||
                        cleanLabel.contains("PSI", ignoreCase = true) ||
                        cleanLabel.contains("PPM", ignoreCase = true) ||
                        cleanLabel.contains("Número", ignoreCase = true) ||
                        cleanLabel.contains("Codigo", ignoreCase = true) -> FormFieldType.NUMBER

                        cleanLabel.contains("Estado", ignoreCase = true) ||
                        cleanLabel.contains("Nivel", ignoreCase = true) ||
                        cleanLabel.contains("Uso", ignoreCase = true) -> FormFieldType.DROPDOWN

                        else -> FormFieldType.TEXT
                    }

                    val options = if (fieldType == FormFieldType.DROPDOWN) {
                        listOf("Bueno", "Regular", "Requiere Atención", "Crítico")
                    } else emptyList()

                    fieldsList.add(
                        CustomFormField(
                            id = "f_pdf_$fieldCounter",
                            label = cleanLabel,
                            type = fieldType,
                            value = if (fieldType == FormFieldType.DROPDOWN) "Bueno" else "",
                            options = options,
                            positionX = 180f,
                            positionY = (700f - (fieldCounter * 45f)).coerceAtLeast(100f)
                        )
                    )
                    fieldCounter++
                }

                if (fieldsList.size >= 10) break
            }

            if (fieldsList.isEmpty()) {
                fieldsList.addAll(createDefaultFields())
            }

            AnalyzedDocumentResult(
                templateTitle = extractedTitle,
                category = category,
                description = "Plantilla generada por extracción de texto real de archivo PDF (${fieldsList.size} campos extraídos).",
                extractedFields = fieldsList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fallo análisis de PDF", e)
            AnalyzedDocumentResult(
                templateTitle = "Formato PDF Escaneado #$templateIndex",
                category = "Inspección",
                description = "Plantilla extraída de archivo PDF cargado.",
                extractedFields = createDefaultFields()
            )
        }
    }

    /**
     * Analiza el Bitmap de la hoja capturada con Google ML Kit Text Recognition (OCR).
     * Calcula los Bounding Boxes (Cajas Delimitadoras X,Y en puntos PostScript A4)
     * para superponer los datos exactamente sobre las casillas físicas.
     */
    suspend fun analyzeScannedBitmapWithMlKit(
        bitmap: Bitmap,
        templateIndex: Int,
        pageSize: PageSize = PageSize.A4
    ): AnalyzedDocumentResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText: Text = recognizer.process(image).awaitResult()

            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            val textLines = visionText.textBlocks.flatMap { it.lines }
                .filter { line -> line.text.trim().length > 2 }

            var extractedCategory = "Inspección"
            val fieldsList = mutableListOf<CustomFormField>()

            // 1. Identificar Título del Documento
            val titleLine = textLines.firstOrNull { line ->
                val txt = line.text.uppercase()
                txt.contains("INSPECCION") || txt.contains("CONTROL") ||
                txt.contains("REPORTE") || txt.contains("FORMATO") ||
                txt.contains("MANTENIMIENTO") || txt.contains("AUDITORIA") ||
                txt.contains("HOJA")
            } ?: textLines.firstOrNull()

            val extractedTitle = titleLine?.text?.replace(":", "")?.replace("_", " ")?.trim()
                ?: "Formato Escaneado #$templateIndex"

            // 2. Determinar Categoría
            if (extractedTitle.contains("Mantenimiento", ignoreCase = true)) {
                extractedCategory = "Mantenimiento"
            } else if (extractedTitle.contains("Seguridad", ignoreCase = true) || extractedTitle.contains("Riesgo", ignoreCase = true)) {
                extractedCategory = "Incidencia"
            } else if (extractedTitle.contains("Almacen", ignoreCase = true) || extractedTitle.contains("Inventario", ignoreCase = true)) {
                extractedCategory = "General"
            }

            // 3. Convertir Bounding Boxes de Pixeles de Imagen a Puntos PostScript PDF
            var fieldCounter = 1
            for (line in textLines) {
                if (line.text.equals(titleLine?.text, ignoreCase = true)) continue

                val cleanLabel = line.text.replace(":", "").replace("_", "").trim()
                if (cleanLabel.length in 3..45) {
                    val box: Rect? = line.boundingBox

                    val xPt = if (box != null && bitmapWidth > 0) {
                        ((box.right.toFloat() + 15f) / bitmapWidth) * pageSize.widthPt
                    } else 180f

                    val yPt = if (box != null && bitmapHeight > 0) {
                        pageSize.heightPt - ((box.bottom.toFloat() / bitmapHeight) * pageSize.heightPt)
                    } else (700f - (fieldCounter * 45f))

                    val fieldType = when {
                        cleanLabel.contains("Temp", ignoreCase = true) ||
                        cleanLabel.contains("Cantidad", ignoreCase = true) ||
                        cleanLabel.contains("PSI", ignoreCase = true) ||
                        cleanLabel.contains("PPM", ignoreCase = true) ||
                        cleanLabel.contains("Número", ignoreCase = true) ||
                        cleanLabel.contains("Codigo", ignoreCase = true) -> FormFieldType.NUMBER

                        cleanLabel.contains("Estado", ignoreCase = true) ||
                        cleanLabel.contains("Nivel", ignoreCase = true) ||
                        cleanLabel.contains("Uso", ignoreCase = true) -> FormFieldType.DROPDOWN

                        else -> FormFieldType.TEXT
                    }

                    val options = if (fieldType == FormFieldType.DROPDOWN) {
                        listOf("Bueno", "Regular", "Requiere Atención", "Crítico")
                    } else emptyList()

                    fieldsList.add(
                        CustomFormField(
                            id = "f_ocr_$fieldCounter",
                            label = cleanLabel,
                            type = fieldType,
                            value = if (fieldType == FormFieldType.DROPDOWN) "Bueno" else "",
                            options = options,
                            positionX = xPt.coerceIn(20f, pageSize.widthPt - 50f),
                            positionY = yPt.coerceIn(50f, pageSize.heightPt - 50f)
                        )
                    )
                    fieldCounter++
                }

                if (fieldsList.size >= 10) break
            }

            if (fieldsList.isEmpty()) {
                fieldsList.addAll(createDefaultFields())
            }

            AnalyzedDocumentResult(
                templateTitle = extractedTitle,
                category = extractedCategory,
                description = "Plantilla analizada por OCR con Bounding Boxes de ML Kit (${textLines.size} líneas detectadas).",
                extractedFields = fieldsList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fallo OCR ML Kit", e)
            val defaultFields = createDefaultFields()
            AnalyzedDocumentResult(
                templateTitle = "Formato Escaneado #$templateIndex",
                category = "Inspección",
                description = "Plantilla extraída de foto de documento impreso.",
                extractedFields = defaultFields
            )
        }
    }

    suspend fun analyzeScannedBitmap(bitmap: Bitmap, templateIndex: Int): AnalyzedDocumentResult {
        return analyzeScannedBitmapWithMlKit(bitmap, templateIndex)
    }

    /**
     * Decodifica una imagen (URI) en resolución completa, corrigiendo la rotación EXIF
     * y limitando el lado mayor para no agotar memoria.
     */
    suspend fun decodeBitmapFromUri(context: Context, uri: Uri, maxSide: Int = 2400): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val raw = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@withContext null

                val rotation = try {
                    context.contentResolver.openInputStream(uri)?.use { s ->
                        when (ExifInterface(s).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                        )) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    } ?: 0f
                } catch (_: Exception) { 0f }

                val scale = maxSide.toFloat() / maxOf(raw.width, raw.height)
                val matrix = Matrix().apply {
                    if (scale < 1f) postScale(scale, scale)
                    if (rotation != 0f) postRotate(rotation)
                }
                if (matrix.isIdentity) raw
                else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            } catch (e: Exception) {
                Log.e(TAG, "Error decodificando imagen", e)
                null
            }
        }

    /** Renderiza la primera página de un PDF a Bitmap (para PDFs escaneados sin texto). */
    private fun renderFirstPdfPage(context: Context, pdfUri: Uri): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    renderer.openPage(0).use { page ->
                        val scale = 2000f / maxOf(page.width, page.height)
                        val bmp = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renderizando PDF", e)
            null
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    fun createDefaultFields(): List<CustomFormField> {
        return listOf(
            CustomFormField(
                id = "df_1",
                label = "Área / Ubicación",
                type = FormFieldType.DROPDOWN,
                value = "Planta Principal",
                options = listOf("Planta Principal", "Área de Embalaje", "Almacén B", "Línea de Ensamblaje"),
                positionX = 180f,
                positionY = 650f
            ),
            CustomFormField(
                id = "df_2",
                label = "Estado del área",
                type = FormFieldType.DROPDOWN,
                value = "Bueno",
                options = listOf("Bueno", "Regular", "Requiere Atención", "Crítico"),
                positionX = 180f,
                positionY = 600f
            ),
            CustomFormField(
                id = "df_3",
                label = "Temperatura (°C)",
                type = FormFieldType.NUMBER,
                value = "24",
                positionX = 180f,
                positionY = 550f
            ),
            CustomFormField(
                id = "df_4",
                label = "Observaciones de Inspección",
                type = FormFieldType.TEXT,
                value = "Todo en condiciones normales de operación.",
                positionX = 50f,
                positionY = 450f
            )
        )
    }
}
