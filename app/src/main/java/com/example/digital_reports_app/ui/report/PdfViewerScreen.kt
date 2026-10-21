package com.example.digital_reports_app.ui.report

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.PdfReportGenerator
import com.example.digital_reports_app.util.StructuredPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    onBack: () -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val report = uiState.report

    val pdfPages = remember { mutableStateListOf<Bitmap>() }
    var isLoadingPages by remember { mutableStateOf(true) }

    LaunchedEffect(report) {
        if (report != null) {
            withContext(Dispatchers.IO) {
                try {
                    val pdfFile = if (report.formDataJson.isNotBlank()) {
                        val form = com.example.digital_reports_app.domain.model.FormCatalog.byId(report.formTemplateId)
                        if (form != null) {
                            val answers = com.example.digital_reports_app.domain.model.jsonToAnswers(report.formDataJson)
                            StructuredPdfGenerator.generate(context, report, form, answers)
                        } else {
                            PdfReportGenerator.generatePdfReport(context, report)
                        }
                    } else {
                        PdfReportGenerator.generatePdfReport(context, report)
                    }

                    val input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(input)

                    val renderedBitmaps = mutableListOf<Bitmap>()
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        val pageCanvas = android.graphics.Canvas(bitmap)
                        pageCanvas.drawColor(android.graphics.Color.WHITE) // Relleno previo de papel blanco puro
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderedBitmaps.add(bitmap)
                    }
                    renderer.close()
                    input.close()

                    pdfPages.clear()
                    pdfPages.addAll(renderedBitmaps)
                    isLoadingPages = false
                } catch (_: Exception) {
                    isLoadingPages = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = report?.title ?: "Visor de Reporte PDF",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "Visor Interno ISO/IEC 17020",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (report != null) {
                        IconButton(
                            onClick = {
                                try {
                                    val pdfFile = PdfReportGenerator.generatePdfReport(context, report)
                                    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Reporte ${report.title}")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Reporte via"))
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Compartir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPages) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Cargando páginas del documento PDF...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (pdfPages.isEmpty()) {
                Text(
                    text = "No se pudieron renderizar las páginas del archivo PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(pdfPages) { index, bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(cornerRadius = 12.dp, prominent = true)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Página ${index + 1} de ${pdfPages.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Página ${index + 1}",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
