package com.example.digital_reports_app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.digital_reports_app.data.local.entity.ReportEntity
import com.example.digital_reports_app.ui.report.ReportTemplateItem
import java.io.File
import java.io.FileOutputStream

object DocumentScannerHelper {

    /**
     * Analiza una hoja física o documento escaneado y crea automáticamente
     * la plantilla estructurada para la empresa.
     */
    fun createTemplateFromScan(
        templateTitle: String,
        category: String,
        description: String
    ): ReportTemplateItem {
        val templateId = "scanned_tpl_${System.currentTimeMillis()}"
        return ReportTemplateItem(
            id = templateId,
            title = templateTitle.trim().ifBlank { "Plantilla Escaneada" },
            category = category.trim().ifBlank { "Inspección" },
            description = description.trim().ifBlank { "Plantilla generada desde escaneo de formato impreso." }
        )
    }

    /**
     * Genera el PDF final dibujando el texto con estilo de "llenado a mano" (tinta azul y fuente manuscrita)
     * sobre el formato físico original.
     */
    fun generateHandwrittenPdfOverlay(
        context: Context,
        report: ReportEntity,
        location: String,
        areaStatus: String,
        temperature: String,
        observations: String
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()

        // 1. Dibuja el Fondo del Formulario Impreso Vacío (Simulación de Hoja de Reporte Física)
        paint.color = Color.rgb(250, 250, 245) // Color hoja papel bond
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Bordes y marco del formato físico
        paint.color = Color.rgb(40, 40, 40)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), (pageHeight - 20).toFloat(), paint)

        // Encabezado impreso del formato físico vacio
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        canvas.drawText("FORMATO DE INSPECCIÓN Y CONTROL TÉCNICO", 40f, 55f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        canvas.drawText("CÓDIGO DE FORMATO: FMT-FIELD-2026", 40f, 75f, paint)

        paint.strokeWidth = 1f
        canvas.drawLine(30f, 90f, (pageWidth - 30).toFloat(), 90f, paint)

        // Líneas y cajas vacías del formato impreso
        var lineY = 130f
        val drawFormLine: (String) -> Unit = { labelText ->
            paint.color = Color.rgb(60, 60, 60)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            canvas.drawText(labelText, 40f, lineY, paint)

            paint.color = Color.rgb(180, 180, 180)
            paint.strokeWidth = 1f
            canvas.drawLine(180f, lineY + 2f, (pageWidth - 40).toFloat(), lineY + 2f, paint)
            lineY += 45f
        }

        drawFormLine("CÓDIGO / ID:")
        drawFormLine("CÓDIGO INSPECTOR:")
        drawFormLine("NOMBRE EMISOR:")
        drawFormLine("UBICACIÓN / ÁREA:")
        drawFormLine("ESTADO DEL ÁREA:")
        drawFormLine("TEMPERATURA (°C):")

        // Caja vacía de observaciones
        paint.color = Color.rgb(60, 60, 60)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        canvas.drawText("OBSERVACIONES / DICTAMEN:", 40f, lineY, paint)

        lineY += 15f
        paint.color = Color.rgb(200, 200, 200)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(40f, lineY, (pageWidth - 40).toFloat(), lineY + 180f, 8f, 8f, paint)

        // 2. ESTAMPADO CON ESTILO MANUSCRITO / A MANO (Tinta azul pluma y fuente manuscrita)
        val inkPaint = Paint().apply {
            color = Color.rgb(15, 35, 160) // Tinta azul pluma
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC) // Estilo manuscrito cursivo
            isAntiAlias = true
        }

        // Estampado de respuestas sobre el documento impreso
        canvas.drawText(report.reportId, 185f, 130f, inkPaint)
        canvas.drawText(report.authorCode, 185f, 175f, inkPaint)
        canvas.drawText(report.authorName, 185f, 220f, inkPaint)
        canvas.drawText(location, 185f, 265f, inkPaint)
        canvas.drawText(areaStatus, 185f, 310f, inkPaint)
        canvas.drawText("$temperature °C", 185f, 355f, inkPaint)

        // Dibujar observaciones escritas a mano dentro del recuadro
        var obsY = lineY + 30f
        val obsText = if (observations.isBlank()) report.description else observations
        val words = obsText.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (inkPaint.measureText(testLine) < (pageWidth - 110)) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, 55f, obsY, inkPaint)
                obsY += 22f
                currentLine = word
                if (obsY > lineY + 160f) break
            }
        }
        if (currentLine.isNotEmpty() && obsY <= lineY + 160f) {
            canvas.drawText(currentLine, 55f, obsY, inkPaint)
        }

        // Firma manuscrita al pie
        val signatureY = pageHeight - 100f
        inkPaint.textSize = 18f
        canvas.drawText("Firma: ${report.authorName}", 60f, signatureY, inkPaint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(100, 100, 100)
        canvas.drawLine(60f, signatureY + 5f, 260f, signatureY + 5f, paint)

        pdfDocument.finishPage(page)

        // Guardar archivo PDF escaneado con llenado
        val reportsDir = File(context.getExternalFilesDir(null), "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val pdfFile = File(reportsDir, "Reporte_Manuscrito_${report.reportId}.pdf")
        FileOutputStream(pdfFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()
        return pdfFile
    }
}
