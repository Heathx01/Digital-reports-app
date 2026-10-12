package com.example.digital_reports_app.domain.model

enum class FieldType {
    TEXT, NUMBER, DATE, SIGNATURE
}

data class DynamicField(
    val id: String,
    val label: String,
    val type: FieldType = FieldType.TEXT,
    val xPt: Float,         // Coordenada X calculada en puntos PostScript (0,0 esquina inferior izquierda)
    val yPt: Float,         // Coordenada Y calculada en puntos PostScript
    val widthPt: Float = 140f
)

data class DynamicTemplate(
    val templateId: String,
    val templateName: String,
    val baseImagePath: String, // Ruta local a la imagen/PDF base escaneada
    val fields: List<DynamicField>
)
