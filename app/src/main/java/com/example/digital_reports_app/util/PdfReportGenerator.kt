package com.example.digital_reports_app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.digital_reports_app.data.local.entity.ReportEntity
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    fun generatePdfReport(context: Context, report: ReportEntity): File {
        val pdfDocument = PdfDocument()

        // Standard A4 dimensions in points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()

        // 1. Header Banner
        paint.color = Color.rgb(26, 35, 126) // Deep Indigo Header
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DIGITAL REPORTS", 30f, 45f, paint)

        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Sistema de Reportes y Control Digital", 30f, 70f, paint)

        // Date right-aligned in header
        paint.textSize = 11f
        canvas.drawText("Fecha: ${report.dateFormatted}", 380f, 45f, paint)

        // 2. Report ID & Status Sub-header
        var currentY = 130f

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("REPORTE OFICIAL #${report.reportId}", 30f, currentY, paint)

        currentY += 30f

        // Status Badge Box
        val statusBgColor = when (report.status) {
            "COMPLETADO" -> Color.rgb(200, 230, 201)
            "EN_PROCESO" -> Color.rgb(187, 222, 251)
            else -> Color.rgb(255, 236, 179)
        }
        paint.color = statusBgColor
        canvas.drawRoundRect(30f, currentY - 18f, 180f, currentY + 8f, 10f, 10f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ESTADO: ${report.status}", 40f, currentY, paint)

        currentY += 40f

        // Divider Line
        paint.color = Color.rgb(220, 220, 220)
        paint.strokeWidth = 1.5f
        canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)

        currentY += 35f

        // 3. Worker / Metadata Section
        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INFORMACIÓN DEL TRABAJADOR Y CATEGORÍA", 30f, currentY, paint)

        currentY += 25f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Código de Trabajador: ${report.authorCode}", 30f, currentY, paint)
        canvas.drawText("Categoría: ${report.category}", 320f, currentY, paint)

        currentY += 20f
        canvas.drawText("Nombre de Emisor: ${report.authorName}", 30f, currentY, paint)

        currentY += 35f

        // Divider Line
        paint.color = Color.rgb(220, 220, 220)
        canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)

        currentY += 35f

        // 4. Report Content
        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TÍTULO: ${report.title}", 30f, currentY, paint)

        currentY += 30f
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DESCRIPCIÓN Y OBSERVACIONES:", 30f, currentY, paint)

        currentY += 15f

        // Bordered Description Box
        val boxTop = currentY
        val boxBottom = currentY + 220f
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRoundRect(30f, boxTop, (pageWidth - 30).toFloat(), boxBottom, 12f, 12f, paint)

        paint.color = Color.rgb(180, 180, 180)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, boxTop, (pageWidth - 30).toFloat(), boxBottom, 12f, 12f, paint)

        // Multiline Description Text
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        var textY = boxTop + 25f
        val lineMargin = 45f
        val maxLineWidth = pageWidth - 90

        val words = report.description.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) < maxLineWidth) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, lineMargin, textY, paint)
                textY += 18f
                currentLine = word
                if (textY > boxBottom - 20f) break
            }
        }
        if (currentLine.isNotEmpty() && textY <= boxBottom - 20f) {
            canvas.drawText(currentLine, lineMargin, textY, paint)
        }

        // 5. Footer
        val footerY = pageHeight - 50f
        paint.color = Color.rgb(150, 150, 150)
        paint.strokeWidth = 1f
        canvas.drawLine(30f, footerY - 20f, (pageWidth - 30).toFloat(), footerY - 20f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(100, 100, 100)
        canvas.drawText("Documento generado oficialmente por Digital Reports App", 30f, footerY, paint)
        canvas.drawText("Página 1 de 1", (pageWidth - 100).toFloat(), footerY, paint)

        pdfDocument.finishPage(page)

        // Save file to external files directory
        val reportsDir = File(context.getExternalFilesDir(null), "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val pdfFile = File(reportsDir, "Reporte_${report.reportId}.pdf")
        FileOutputStream(pdfFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()
        return pdfFile
    }

    fun openPdfFile(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val pdfUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(intent, "Abrir o compartir PDF con...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
    }
}
