package com.example.digital_reports_app.util

import android.content.Context
import android.net.Uri
import com.example.digital_reports_app.domain.model.FormItem
import com.example.digital_reports_app.domain.model.FormPage
import com.example.digital_reports_app.domain.model.FormSection
import com.example.digital_reports_app.domain.model.HeaderField
import com.example.digital_reports_app.domain.model.RowKind
import com.example.digital_reports_app.domain.model.StructuredForm
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

object ExcelTemplateParser {

    /**
     * Parsea un archivo de Microsoft Excel (.xlsx) y construye una estructura de
     * Formato Guiado Paso a Paso (StructuredForm) lista para agregarse al catálogo oficial.
     */
    fun parseExcelToStructuredForm(
        context: Context,
        fileUri: Uri,
        customName: String? = null
    ): Result<StructuredForm> {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                parseExcelStream(inputStream, customName)
            } ?: Result.failure(Exception("No se pudo abrir el archivo de Excel."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseExcelStream(inputStream: InputStream, customName: String?): Result<StructuredForm> {
        return try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) ?: return Result.failure(Exception("El archivo de Excel está vacío."))

            val formPages = mutableListOf<FormPage>()
            val headerFields = mutableListOf<HeaderField>()
            val sections = mutableListOf<FormSection>()

            var docTitle = customName ?: "FORMATO OFICIAL DESDE EXCEL"
            var docCode = "FMT-XLSX-2026"
            var organizationName = "Empresa Oficial"

            val currentSectionItems = mutableListOf<FormItem>()
            var currentSectionTitle = "Parámetros del Formato"

            for (rowIndex in 0..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val firstCellText = getCellStringValue(row.getCell(0)).trim()

                if (firstCellText.isNotBlank()) {
                    val upper = firstCellText.uppercase()

                    // Identificación de Encabezado Corporativo
                    when {
                        upper.contains("FORMATO") || upper.contains("INSPECCION") || upper.contains("REPORTE") -> {
                            docTitle = firstCellText
                        }
                        upper.contains("CODIGO") || upper.contains("CÓDIGO") -> {
                            docCode = firstCellText
                        }
                        upper.contains("ORGANIZACION") || upper.contains("EMPRESA") -> {
                            organizationName = firstCellText
                        }
                        upper.endsWith(":") || upper.contains("FECHA") || upper.contains("RESPONSABLE") || upper.contains("FINCA") -> {
                            val fieldId = "hdr_${headerFields.size + 1}"
                            headerFields.add(HeaderField(fieldId, firstCellText.replace(":", "").trim(), upper.contains("FECHA")))
                        }
                        else -> {
                            // Celdas de preguntas o puntos de la plantilla
                            val itemId = "item_${currentSectionItems.size + 1}"
                            currentSectionItems.add(FormItem(itemId, firstCellText))
                        }
                    }
                }
            }

            if (currentSectionItems.isNotEmpty()) {
                sections.add(
                    FormSection(
                        id = "sec_1",
                        title = currentSectionTitle,
                        kind = RowKind.COMPLY,
                        items = currentSectionItems.toList()
                    )
                )
            }

            val defaultPage = FormPage(
                id = "p_1",
                title = "DATOS Y EVALUACIÓN DEL FORMATO",
                headerFields = if (headerFields.isNotEmpty()) headerFields else listOf(
                    HeaderField("fecha", "FECHA", isDate = true),
                    HeaderField("area", "ÁREA / UBICACIÓN"),
                    HeaderField("responsable", "RESPONSABLE")
                ),
                sections = if (sections.isNotEmpty()) sections else listOf(
                    FormSection(
                        "sec_gen", "Puntos de Control", RowKind.COMPLY,
                        listOf(FormItem("item_1", "Estado del área de trabajo"))
                    )
                ),
                commentsLabel = "Observaciones y Comentarios Adicionales:",
                signatureLabels = listOf("FIRMA DEL INSPECTOR", "FIRMA DE CONFORMIDAD")
            )

            formPages.add(defaultPage)

            val structuredForm = StructuredForm(
                id = "FMT_XLSX_${System.currentTimeMillis()}",
                name = customName ?: docTitle,
                category = "Oficial",
                organization = organizationName,
                documentTitle = docTitle,
                revision = "Versión Excel On-Premise",
                code = docCode,
                footer = "Documento oficial procesado desde plantilla Excel. Retención: 6 meses.",
                pages = formPages
            )

            workbook.close()
            Result.success(structuredForm)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCellStringValue(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (_: Exception) {
                    cell.numericCellValue.toString()
                }
            }
            else -> ""
        }
    }
}
