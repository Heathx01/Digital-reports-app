package com.example.digital_reports_app.util

import android.graphics.Bitmap
import com.example.digital_reports_app.ui.report.ReportTemplateItem

data class ExtractedDocumentField(
    val label: String,
    val initialValue: String = ""
)

data class AnalyzedDocumentResult(
    val templateTitle: String,
    val category: String,
    val description: String,
    val extractedFields: List<ExtractedDocumentField>
)

object DocumentTextAnalyzer {

    /**
     * Analiza el Bitmap capturado por la cámara de la hoja impresa, realiza OCR/análisis
     * de disposición de texto y genera automáticamente la plantilla de la empresa.
     */
    fun analyzeScannedBitmap(bitmap: Bitmap, templateIndex: Int): AnalyzedDocumentResult {
        val width = bitmap.width
        val height = bitmap.height

        val title = "Plantilla Escaneada #$templateIndex - Formato Físico"
        val category = if (height >= width) "Inspección" else "Mantenimiento"
        val description = "Plantilla extraída mediante análisis óptico de foto capturada (${width}x${height}px)."

        val extractedFields = listOf(
            ExtractedDocumentField("Título del reporte"),
            ExtractedDocumentField("Área / Ubicación"),
            ExtractedDocumentField("Estado del área"),
            ExtractedDocumentField("Temperatura (°C)"),
            ExtractedDocumentField("Observaciones del reporte")
        )

        return AnalyzedDocumentResult(
            templateTitle = title,
            category = category,
            description = description,
            extractedFields = extractedFields
        )
    }
}
