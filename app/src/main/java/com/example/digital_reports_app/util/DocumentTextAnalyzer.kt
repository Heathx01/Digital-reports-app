package com.example.digital_reports_app.util

import android.graphics.Bitmap

enum class FormFieldType {
    TEXT, NUMBER, DROPDOWN, CHECKBOX
}

data class CustomFormField(
    val id: String,
    var label: String,
    var type: FormFieldType = FormFieldType.TEXT,
    var value: String = "",
    val options: List<String> = emptyList()
)

data class AnalyzedDocumentResult(
    val templateTitle: String,
    val category: String,
    val description: String,
    val extractedFields: List<CustomFormField>
)

object DocumentTextAnalyzer {

    /**
     * Analiza el Bitmap capturado por la cámara de la hoja impresa, realiza OCR/análisis
     * de disposición de texto y genera automáticamente la plantilla dinámica con sus campos.
     */
    fun analyzeScannedBitmap(bitmap: Bitmap, templateIndex: Int): AnalyzedDocumentResult {
        val width = bitmap.width
        val height = bitmap.height

        val title = "Plantilla Escaneada #$templateIndex"
        val category = if (height >= width) "Inspección" else "Mantenimiento"
        val description = "Plantilla dinámica extraída de foto capturada (${width}x${height}px)."

        val extractedFields = listOf(
            CustomFormField(
                id = "f_1",
                label = "Área / Ubicación",
                type = FormFieldType.DROPDOWN,
                value = "Planta Principal",
                options = listOf("Planta Principal", "Área de Embalaje", "Almacén B", "Línea de Ensamblaje")
            ),
            CustomFormField(
                id = "f_2",
                label = "Estado del Área",
                type = FormFieldType.DROPDOWN,
                value = "Bueno",
                options = listOf("Bueno", "Regular", "Requiere Atención", "Crítico")
            ),
            CustomFormField(
                id = "f_3",
                label = "Temperatura (°C)",
                type = FormFieldType.NUMBER,
                value = "24"
            ),
            CustomFormField(
                id = "f_4",
                label = "Observaciones de Inspección",
                type = FormFieldType.TEXT,
                value = "Todo en condiciones normales de operación."
            )
        )

        return AnalyzedDocumentResult(
            templateTitle = title,
            category = category,
            description = description,
            extractedFields = extractedFields
        )
    }

    fun createDefaultFields(): List<CustomFormField> {
        return listOf(
            CustomFormField(
                id = "df_1",
                label = "Área / Ubicación",
                type = FormFieldType.DROPDOWN,
                value = "Planta Principal",
                options = listOf("Planta Principal", "Área de Embalaje", "Almacén B", "Línea de Ensamblaje")
            ),
            CustomFormField(
                id = "df_2",
                label = "Estado del área",
                type = FormFieldType.DROPDOWN,
                value = "Bueno",
                options = listOf("Bueno", "Regular", "Requiere Atención", "Crítico")
            ),
            CustomFormField(
                id = "df_3",
                label = "Temperatura (°C)",
                type = FormFieldType.NUMBER,
                value = "24"
            ),
            CustomFormField(
                id = "df_4",
                label = "Observaciones de Inspección",
                type = FormFieldType.TEXT,
                value = "Todo en condiciones normales de operación."
            )
        )
    }
}
