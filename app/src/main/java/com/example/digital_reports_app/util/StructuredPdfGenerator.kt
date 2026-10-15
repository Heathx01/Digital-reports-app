package com.example.digital_reports_app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.digital_reports_app.data.local.entity.ReportEntity
import com.example.digital_reports_app.domain.model.FormKeys
import com.example.digital_reports_app.domain.model.FormPage
import com.example.digital_reports_app.domain.model.FormSection
import com.example.digital_reports_app.domain.model.RowKind
import com.example.digital_reports_app.domain.model.StructuredForm
import com.example.digital_reports_app.domain.model.jsonToStrokes
import java.io.File
import java.io.FileOutputStream

/**
 * Genera el PDF final de un formato estructurado, replicando el diseño del formato en papel:
 * encabezado con logotipo oficial, campos, tablas con casillas, observaciones, comentarios y firmas.
 */
object StructuredPdfGenerator {

    private const val PW = 595
    private const val PH = 842
    private const val M = 36f
    private const val CW = PW - 2 * M

    fun generate(
        context: Context,
        report: ReportEntity,
        form: StructuredForm,
        answers: Map<String, String>
    ): File {
        val doc = PdfDocument()
        val renderer = Renderer(doc, form, answers)
        form.pages.forEach { renderer.drawPage(it) }
        renderer.finish()

        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(dir, "Reporte_${report.reportId}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun paint(
        size: Float,
        bold: Boolean = false,
        color: Int = Color.BLACK,
        style: Paint.Style = Paint.Style.FILL,
        align: Paint.Align = Paint.Align.LEFT,
        strokeWidth: Float = 0.7f
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.style = style
        textAlign = align
        this.strokeWidth = strokeWidth
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private class Renderer(
        val doc: PdfDocument,
        val form: StructuredForm,
        val answers: Map<String, String>
    ) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas = Canvas()
        private var y = M
        private var number = 0

        private val border = paint(8f, style = Paint.Style.STROKE)
        private val text8 = paint(8f)

        fun finish() {
            page?.let {
                drawFooter()
                doc.finishPage(it)
            }
            page = null
        }

        private fun startPage() {
            finish()
            number++
            val info = PdfDocument.PageInfo.Builder(PW, PH, number).create()
            page = doc.startPage(info)
            canvas = page!!.canvas
            y = M - 8f
        }

        private fun ensureSpace(h: Float, onNewPage: (() -> Unit)? = null) {
            if (y + h > PH - 52f) {
                startPage()
                onNewPage?.invoke()
            }
        }

        private fun wrap(text: String, p: Paint, maxWidth: Float): List<String> {
            val out = mutableListOf<String>()
            text.split("\n").forEach { paragraph ->
                var line = ""
                paragraph.split(" ").forEach { word ->
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (p.measureText(candidate) <= maxWidth) {
                        line = candidate
                    } else {
                        if (line.isNotEmpty()) out.add(line)
                        line = word
                    }
                }
                out.add(line)
            }
            return out
        }

        fun drawPage(fp: FormPage) {
            startPage()
            drawDocHeader()
            drawTitleBar(fp.title)
            drawHeaderFields(fp)
            fp.sections.forEach { drawSection(it) }
            drawComments(fp)
            drawSignatures(fp)
        }

        // ---------- Encabezado del documento con Logotipo Oficial Pantaleon ----------
        private fun drawDocHeader() {
            val top = y
            val h = 58f
            canvas.drawRect(M, top, M + CW, top + h, border)
            val logoW = 145f
            val codeW = 90f
            canvas.drawLine(M + logoW, top, M + logoW, top + h, border)
            canvas.drawLine(M + CW - codeW, top, M + CW - codeW, top + h, border)
            canvas.drawLine(M + logoW, top + h - 18f, M + CW, top + h - 18f, border)
            canvas.drawLine(M + CW - codeW, top + h - 18f, M + CW - codeW, top + h, border)

            // Dibuja el emblema corporativo de Pantaleon
            val logoCenterY = top + h / 2f
            val greenPaint = paint(12f, bold = true, color = Color.rgb(22, 128, 61))
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(22, 128, 61)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(M + 22f, logoCenterY, 14f, circlePaint)

            val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val leafPath = Path().apply {
                moveTo(M + 22f, logoCenterY - 9f)
                quadTo(M + 29f, logoCenterY, M + 22f, logoCenterY + 9f)
                quadTo(M + 15f, logoCenterY, M + 22f, logoCenterY - 9f)
            }
            canvas.drawPath(leafPath, leafPaint)

            val orgName = if (form.organization.contains("Pantaleon", ignoreCase = true)) "Pantaleon" else form.organization
            canvas.drawText(orgName, M + 42f, logoCenterY + 5f, greenPaint)

            val midCenter = M + logoW + (CW - logoW - codeW) / 2f
            val titleP = paint(8.5f, bold = true, align = Paint.Align.CENTER)
            var ty = top + 14f
            form.documentTitle.split("\n").forEach {
                canvas.drawText(it, midCenter, ty, titleP)
                ty += 11f
            }
            canvas.drawText(form.revision, midCenter, top + h - 6f, paint(7.5f, align = Paint.Align.CENTER))
            canvas.drawText(form.code, M + CW - codeW / 2f, top + h - 6f, paint(7.5f, bold = true, align = Paint.Align.CENTER))
            y = top + h
        }

        private fun drawTitleBar(title: String) {
            canvas.drawRect(M, y, M + CW, y + 18f, border)
            canvas.drawText(title, M + CW / 2f, y + 12.5f, paint(9.5f, bold = true, align = Paint.Align.CENTER))
            y += 18f + 12f
        }

        // ---------- Campos de cabecera ----------
        private fun drawHeaderFields(fp: FormPage) {
            val labelP = paint(7.5f, bold = true)
            val valueP = paint(9f)
            val fields = fp.headerFields
            var i = 0
            while (i < fields.size) {
                val f = fields[i]
                if (f.label.length <= 15) {
                    val group = mutableListOf(f)
                    while (i + 1 < fields.size && fields[i + 1].label.length <= 15 && group.size < 3) {
                        i++
                        group.add(fields[i])
                    }
                    val colW = CW / group.size
                    group.forEachIndexed { idx, gf ->
                        drawField(M + idx * colW, colW - 10f, gf.label, answers[FormKeys.header(fp.id, gf.id)].orEmpty(), labelP, valueP)
                    }
                } else {
                    drawField(M, CW, f.label, answers[FormKeys.header(fp.id, f.id)].orEmpty(), labelP, valueP)
                }
                y += 20f
                i++
            }
            y += 4f
        }

        private fun drawField(x: Float, w: Float, label: String, value: String, labelP: Paint, valueP: Paint) {
            val lw = labelP.measureText("$label: ")
            canvas.drawText("$label:", x, y + 10f, labelP)
            canvas.drawLine(x + lw, y + 12f, x + w, y + 12f, border)
            val maxW = w - lw - 2f
            var shown = value
            while (shown.isNotEmpty() && valueP.measureText(shown) > maxW) shown = shown.dropLast(1)
            canvas.drawText(shown, x + lw + 2f, y + 10f, valueP)
        }

        // ---------- Tablas de evaluación sin desbordamiento ----------
        private fun columns(kind: RowKind): List<Pair<String, Float>> = when (kind) {
            RowKind.YES_NO -> listOf("SI" to 28f, "NO" to 28f)
            RowKind.COMPLY -> listOf("CUMPLE" to 38f, "NO CUMPLE" to 42f)
            RowKind.MEASURE -> listOf("MEDICIÓN" to 46f, "CUMPLE" to 38f, "NO CUMPLE" to 42f)
        }

        private fun drawSection(s: FormSection) {
            val cols = columns(s.kind)
            val labelW = 210f
            val checkW = cols.sumOf { it.second.toDouble() }.toFloat()
            val obsW = CW - labelW - checkW

            val header = {
                val hh = 16f
                var x = M
                canvas.drawRect(x, y, x + labelW, y + hh, border)
                canvas.drawText(s.title, x + labelW / 2f, y + 11f, paint(9f, bold = true, align = Paint.Align.CENTER))
                x += labelW
                cols.forEach { (name, w) ->
                    canvas.drawRect(x, y, x + w, y + hh, border)
                    canvas.drawText(name, x + w / 2f, y + 10.5f, paint(6f, align = Paint.Align.CENTER))
                    x += w
                }
                canvas.drawRect(x, y, x + obsW, y + hh, border)
                canvas.drawText("Observaciones", x + obsW / 2f, y + 11f, paint(9f, bold = true, align = Paint.Align.CENTER))
                y += hh
            }

            ensureSpace(16f + 32f)
            header()

            s.items.forEach { item ->
                val labelLines = wrap(item.label, text8, labelW - 10f)
                val obs = answers[FormKeys.obs(item.id)].orEmpty()
                val obsLines = if (obs.isBlank()) emptyList() else wrap(obs, text8, obsW - 10f)
                
                // Cálculo de altura dinamica con espaciado amplio para evitar textos superpuestos
                val lineCount = maxOf(labelLines.size, obsLines.size)
                val rowH = (lineCount * 13f + 14f).coerceAtLeast(30f)
                
                ensureSpace(rowH) { header() }

                var x = M
                canvas.drawRect(x, y, x + labelW, y + rowH, border)
                labelLines.forEachIndexed { i, l -> 
                    canvas.drawText(l, x + 5f, y + 14f + i * 12f, text8) 
                }
                x += labelW

                val colXs = mutableListOf<Pair<Float, Float>>()
                cols.forEach { (_, w) ->
                    canvas.drawRect(x, y, x + w, y + rowH, border)
                    colXs.add(x to w)
                    x += w
                }

                canvas.drawRect(x, y, x + obsW, y + rowH, border)
                obsLines.forEachIndexed { i, l -> 
                    canvas.drawText(l, x + 5f, y + 14f + i * 12f, text8) 
                }

                val ans = answers[FormKeys.answer(item.id)].orEmpty()
                val markCol = when (s.kind) {
                    RowKind.YES_NO, RowKind.COMPLY -> when (ans) {
                        s.kind.positive -> 0
                        s.kind.negative -> 1
                        else -> -1
                    }
                    RowKind.MEASURE -> when (ans) {
                        s.kind.positive -> 1
                        s.kind.negative -> 2
                        else -> -1
                    }
                }
                if (markCol >= 0) {
                    val (cx, cw) = colXs[markCol]
                    canvas.drawText("X", cx + cw / 2f, y + rowH / 2f + 4f, paint(11f, bold = true, align = Paint.Align.CENTER))
                }
                if (s.kind == RowKind.MEASURE) {
                    val (cx, cw) = colXs[0]
                    var v = answers[FormKeys.value(item.id)].orEmpty()
                    val vp = paint(8f, align = Paint.Align.CENTER)
                    while (v.isNotEmpty() && vp.measureText(v) > cw - 4f) v = v.dropLast(1)
                    canvas.drawText(v, cx + cw / 2f, y + rowH / 2f + 4f, vp)
                }
                y += rowH
            }
            y += 6f
        }

        // ---------- Comentarios ----------
        private fun drawComments(fp: FormPage) {
            val comments = answers[FormKeys.comments(fp.id)].orEmpty()
            val lines = if (comments.isBlank()) emptyList() else wrap(comments, paint(9f), CW - 12f)
            val rows = maxOf(4, lines.size)
            val boxH = rows * 16f
            ensureSpace(14f + boxH + 90f)
            y += 6f
            canvas.drawText(fp.commentsLabel, M, y + 8f, paint(8f))
            y += 14f
            canvas.drawRect(M, y, M + CW, y + boxH, border)
            for (r in 1 until rows) canvas.drawLine(M, y + r * 16f, M + CW, y + r * 16f, border)
            val p = paint(9f)
            lines.forEachIndexed { i, l -> canvas.drawText(l, M + 6f, y + i * 16f + 12f, p) }
            y += boxH + 14f
        }

        // ---------- Firmas ----------
        private fun drawSignatures(fp: FormPage) {
            val n = fp.signatureLabels.size.coerceAtLeast(1)
            val colW = CW / n
            val areaH = 60f
            ensureSpace(areaH + 36f)
            val top = y + 6f
            fp.signatureLabels.forEachIndexed { i, label ->
                val x0 = M + i * colW + 10f
                val x1 = M + (i + 1) * colW - 10f
                val strokes = jsonToStrokes(answers[FormKeys.sigStrokes(fp.id, i)].orEmpty())
                val w = x1 - x0
                val sigPaint = paint(1.6f, style = Paint.Style.STROKE, strokeWidth = 1.6f).apply {
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                strokes.forEach { stroke ->
                    if (stroke.size > 1) {
                        val path = Path()
                        stroke.forEachIndexed { k, (nx, ny) ->
                            val px = x0 + nx * w
                            val py = top + ny * areaH
                            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        canvas.drawPath(path, sigPaint)
                    }
                }
                canvas.drawLine(x0, top + areaH, x1, top + areaH, border)
                val cx = (x0 + x1) / 2f
                val lp = paint(7.5f, bold = true, align = Paint.Align.CENTER)
                wrap(label, lp, w).forEachIndexed { k, l ->
                    canvas.drawText(l, cx, top + areaH + 11f + k * 9f, lp)
                }
                val name = answers[FormKeys.sigName(fp.id, i)].orEmpty()
                if (name.isNotBlank()) {
                    canvas.drawText(name, cx, top + areaH + 31f, paint(8f, align = Paint.Align.CENTER))
                }
            }
            y = top + areaH + 40f
        }

        private fun drawFooter() {
            val p = paint(6.5f, color = Color.DKGRAY, align = Paint.Align.CENTER)
            val lines = wrap(form.footer, p, CW)
            lines.take(2).forEachIndexed { i, l ->
                canvas.drawText(l, PW / 2f, PH - 30f + i * 8f, p)
            }
        }
    }
}
