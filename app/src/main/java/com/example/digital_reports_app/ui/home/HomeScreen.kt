package com.example.digital_reports_app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.digital_reports_app.data.local.entity.ReportEntity
import com.example.digital_reports_app.ui.navigation.Screen
import com.example.digital_reports_app.ui.navigation.WorkProBottomNavBar
import com.example.digital_reports_app.ui.report.ReportListViewModel
import com.example.digital_reports_app.ui.theme.StatusSuccessGreen
import com.example.digital_reports_app.ui.theme.StatusWarningAmber
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.worker.SyncReportsWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCreateReportClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onBitacoraClick: () -> Unit,
    onPendingReportsClick: () -> Unit = onBitacoraClick,
    homeViewModel: HomeViewModel = hiltViewModel(),
    reportListViewModel: ReportListViewModel = hiltViewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val reportUiState by reportListViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val user = homeUiState.currentUser

    LaunchedEffect(homeUiState.isLoggedOut) {
        if (homeUiState.isLoggedOut) {
            onLogout()
        }
    }

    val totalCount = reportUiState.reports.size
    val completedCount = reportUiState.reports.count { it.status == "COMPLETADO" }
    val pendingCount = reportUiState.reports.count { it.status != "COMPLETADO" }

    Scaffold(
        bottomBar = {
            WorkProBottomNavBar(
                currentRoute = Screen.HOME,
                onTabSelected = { tabTarget ->
                    when (tabTarget) {
                        "PENDIENTES" -> onPendingReportsClick()
                        Screen.CREATE_REPORT -> onCreateReportClick()
                        Screen.BITACORA, Screen.REPORTS_LIST -> onBitacoraClick()
                        Screen.PROFILE -> onProfileClick()
                        else -> {}
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hola, ${user?.name?.split(" ")?.firstOrNull() ?: "Usuario"} 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "DocuField • Telemetría de Turno",
                        style = TelemetryMonoStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = {
                            homeViewModel.toggleNotificationsDialog(true)
                            val syncRequest = OneTimeWorkRequestBuilder<SyncReportsWorker>().build()
                            WorkManager.getInstance(context).enqueue(syncRequest)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlass(cornerRadius = 22.dp, prominent = true)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Indicator Badge for unsynced pending reports
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Summary Card (Resumen General Liquid Glass)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 20.dp, prominent = true)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dashboard Operativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Point 2: Dynamic Connectivity Status Badge
                        val networkStatusText = if (homeUiState.isOnline) "EN LÍNEA" else "OFFLINE (BÚFER SEGURO)"
                        val networkBadgeBg = if (homeUiState.isOnline) Color(0x3322C55E) else Color(0x33F59E0B)

                        Box(
                            modifier = Modifier
                                .background(networkBadgeBg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = networkStatusText,
                                style = TelemetryMonoStyle.copy(fontSize = 10.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryMetricItem("Reportes", totalCount.toString())
                        SummaryMetricItem("Completados", completedCount.toString())
                        SummaryMetricItem("Pendientes", pendingCount.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Actividad Reciente Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Actividad reciente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onBitacoraClick) {
                    Text("Ver todos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recent Activity Items
            if (reportUiState.reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 16.dp)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Aún no hay actividad reciente.\nCrea un nuevo reporte desde las opciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                reportUiState.reports.take(3).forEach { report ->
                    RecentActivityItemCard(report = report, onClick = { onReportClick(report.reportId) })
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Módulos Grid
            Text(
                text = "Módulos de Inspección",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nuevo Reporte en blanco
                ModuleCard(
                    title = "Nuevo Reporte",
                    subtitle = "Crear reporte técnico",
                    icon = Icons.Outlined.Add,
                    onClick = onCreateReportClick,
                    modifier = Modifier.weight(1f)
                )

                // Point 3: Plantillas ISO con selector
                ModuleCard(
                    title = "Plantillas ISO",
                    subtitle = "Formatos homologados",
                    icon = Icons.Outlined.DateRange,
                    onClick = { homeViewModel.toggleTemplatesDialog(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Point 1: Notifications Modal Dialog
    if (homeUiState.showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { homeViewModel.toggleNotificationsDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Telemetría y Notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    homeUiState.notifications.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(cornerRadius = 12.dp)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(item.timeFormatted, style = TelemetryMonoStyle.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { homeViewModel.toggleNotificationsDialog(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Point 3: ISO Templates Selector Modal Dialog
    if (homeUiState.showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { homeViewModel.toggleTemplatesDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Plantillas Homologadas ISO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TemplateOptionItem("ISO 9001 - Inspección Diurna Planta") {
                        homeViewModel.toggleTemplatesDialog(false)
                        onCreateReportClick()
                    }
                    TemplateOptionItem("ISO 14001 - Control Ambiental y Gas") {
                        homeViewModel.toggleTemplatesDialog(false)
                        onCreateReportClick()
                    }
                    TemplateOptionItem("ISO 45001 - Mantenimiento Mecánico Preventivo") {
                        homeViewModel.toggleTemplatesDialog(false)
                        onCreateReportClick()
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { homeViewModel.toggleTemplatesDialog(false) }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun TemplateOptionItem(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 12.dp)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryMetricItem(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun RecentActivityItemCard(
    report: ReportEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 16.dp)
            .clickable { onClick() }
            .padding(14.dp)
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
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
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
                    Text(
                        text = report.dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Tag
            val isCompleted = report.status == "COMPLETADO"
            val badgeBg = if (isCompleted) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
            val badgeText = if (isCompleted) StatusSuccessGreen else StatusWarningAmber

            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = report.status,
                    color = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .liquidGlass(cornerRadius = 18.dp, prominent = true)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
