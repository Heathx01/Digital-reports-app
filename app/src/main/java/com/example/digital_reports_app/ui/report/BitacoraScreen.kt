package com.example.digital_reports_app.ui.report

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.data.local.entity.ReportEntity
import com.example.digital_reports_app.ui.navigation.Screen
import com.example.digital_reports_app.ui.navigation.WorkProBottomNavBar
import com.example.digital_reports_app.ui.navigation.horizontalTabSwipeGesture
import com.example.digital_reports_app.ui.theme.SkeletonReportCardItem
import com.example.digital_reports_app.ui.theme.StatusSuccessGreen
import com.example.digital_reports_app.ui.theme.StatusWarningAmber
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.PdfReportGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitacoraScreen(
    onReportClick: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    viewModel: ReportListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filterOptions = listOf("TODOS", "COMPLETADOS", "PENDIENTES")
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var reportToDelete by remember { mutableStateOf<ReportEntity?>(null) }

    val totalCount = uiState.reports.size
    val completedCount = uiState.reports.count { it.status == "COMPLETADO" }
    val pendingCount = uiState.reports.count { it.status != "COMPLETADO" }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bitácora de Expedientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            WorkProBottomNavBar(
                currentRoute = Screen.BITACORA,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .horizontalTabSwipeGesture(Screen.BITACORA, onTabSelected)
        ) {
            // Metrics Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .liquidGlass(cornerRadius = 14.dp, prominent = true)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Totales: $totalCount", style = TelemetryMonoStyle, fontWeight = FontWeight.Bold)
                    Text("Completados: $completedCount", style = TelemetryMonoStyle, color = StatusSuccessGreen, fontWeight = FontWeight.Bold)
                    Text("Pendientes: $pendingCount", style = TelemetryMonoStyle, color = StatusWarningAmber, fontWeight = FontWeight.Bold)
                }
            }

            // Search Bar & Clear Button & Sort Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Buscar expediente...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Limpiar Búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Button
                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ordenar ▼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Más recientes primero") },
                            onClick = {
                                viewModel.onSortOptionSelected(ReportSortOption.NEWEST)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Más antiguos primero") },
                            onClick = {
                                viewModel.onSortOptionSelected(ReportSortOption.OLDEST)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Título (A - Z)") },
                            onClick = {
                                viewModel.onSortOptionSelected(ReportSortOption.TITLE_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Por Estado") },
                            onClick = {
                                viewModel.onSortOptionSelected(ReportSortOption.STATUS)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val filterKey = when (filter) {
                        "COMPLETADOS" -> "COMPLETADO"
                        "PENDIENTES" -> "PENDIENTE"
                        else -> "TODOS"
                    }
                    FilterChip(
                        selected = uiState.selectedStatusFilter == filterKey,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onStatusFilterSelected(filterKey)
                        },
                        label = { Text(filter) }
                    )
                }
            }

            // Result Counter Indicator
            if (uiState.filteredReports.isNotEmpty()) {
                Text(
                    text = "Mostrando ${uiState.filteredReports.size} expediente(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Reports List or Skeleton Loading
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        SkeletonReportCardItem()
                    }
                }
            } else if (uiState.filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron expedientes con los criterios seleccionados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.filteredReports,
                        key = { it.reportId }
                    ) { report ->
                        BitacoraCardItem(
                            report = report,
                            onClick = { onReportClick(report.reportId) },
                            onOpenPdf = {
                                val file = PdfReportGenerator.generatePdfReport(context, report)
                                PdfReportGenerator.openPdfFile(context, file)
                            },
                            onPrintPdf = {
                                val file = PdfReportGenerator.generatePdfReport(context, report)
                                PdfReportGenerator.printPdfFile(context, file)
                            },
                            onDelete = {
                                reportToDelete = report
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Report Confirmation Dialog with UNDO Snackbar
    reportToDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("Eliminar Expediente", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar el reporte '${report.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedTarget = report
                        viewModel.deleteReport(deletedTarget.reportId)
                        reportToDelete = null

                        // Show Snackbar with UNDO Action
                        scope.launch {
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "Expediente '${deletedTarget.title}' eliminado",
                                actionLabel = "DESHACER",
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                viewModel.restoreReport(deletedTarget)
                                Toast.makeText(context, "Expediente restaurado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reportToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BitacoraCardItem(
    report: ReportEntity,
    onClick: () -> Unit,
    onOpenPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 16.dp)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val iconVector = when (report.category.uppercase()) {
                    "INSPECCIÓN" -> Icons.Outlined.Edit
                    "MANTENIMIENTO" -> Icons.Outlined.Info
                    else -> Icons.Outlined.Check
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${report.reportId} • ${report.dateFormatted}",
                        style = TelemetryMonoStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Context Menu
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("👁️ Ver Detalle Dictamen") },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📄 Abrir en Lector PDF") },
                            onClick = {
                                menuExpanded = false
                                onOpenPdf()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🖨️ Previsualizar / Imprimir") },
                            onClick = {
                                menuExpanded = false
                                onPrintPdf()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ Eliminar Reporte", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }

                val isCompleted = report.status == "COMPLETADO"
                val badgeText = if (isCompleted) StatusSuccessGreen else StatusWarningAmber
                Text(
                    text = if (isCompleted) "Completado" else "Pendiente",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
        }
    }
}
