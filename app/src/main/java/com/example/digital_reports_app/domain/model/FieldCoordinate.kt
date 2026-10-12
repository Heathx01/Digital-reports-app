package com.example.digital_reports_app.domain.model

data class FieldCoordinate(
    val x: Float,
    val y: Float,
    val fontSize: Float = 11f
)

// Mapa de posiciones para el Formato F-04
object FormatoF04Config {
    const val TEMPLATE_FILE = "templates/formato_f04_base.pdf"

    val FIELD_POSITIONS = mapOf(
        "fecha" to FieldCoordinate(x = 110f, y = 695f, fontSize = 10f),
        "hora" to FieldCoordinate(x = 260f, y = 695f, fontSize = 10f),
        "operador" to FieldCoordinate(x = 110f, y = 665f, fontSize = 11f),
        "turno" to FieldCoordinate(x = 420f, y = 665f, fontSize = 10f),
        "observaciones" to FieldCoordinate(x = 65f, y = 520f, fontSize = 9f),
        "firma" to FieldCoordinate(x = 350f, y = 140f)
    )
}
