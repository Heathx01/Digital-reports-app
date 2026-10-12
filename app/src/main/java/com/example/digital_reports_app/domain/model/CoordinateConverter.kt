package com.example.digital_reports_app.domain.model

enum class PageSize(val widthPt: Float, val heightPt: Float) {
    CARTA(612f, 792f),
    A4(595.28f, 841.89f)
}

/**
 * Convierte medidas tomadas con regla física en milímetros desde la esquina superior izquierda
 * a coordenadas estándar PDF (puntos PostScript desde la esquina inferior izquierda).
 */
object CoordinateConverter {
    private const val MM_TO_PT = 72f / 25.4f // 1 mm ≈ 2.83465 pt

    fun fromTopLeftMm(
        xMm: Float, 
        yFromTopMm: Float, 
        pageSize: PageSize = PageSize.CARTA
    ): FieldCoordinate {
        val xPt = xMm * MM_TO_PT
        val yPt = pageSize.heightPt - (yFromTopMm * MM_TO_PT)
        return FieldCoordinate(x = xPt, y = yPt)
    }
}
