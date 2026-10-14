package com.example.digital_reports_app.ui.report

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.domain.model.FormCatalog
import com.example.digital_reports_app.ui.navigation.Screen
import com.example.digital_reports_app.ui.navigation.WorkProBottomNavBar
import com.example.digital_reports_app.ui.navigation.horizontalTabSwipeGesture
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass

data class ReportTemplateItem(
    val id: String,
    var title: String,
    var category: String,
    var description: String,
    val icon: ImageVector = Icons.Outlined.Edit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FormTemplateScreen(
    onBack: () -> Unit,
    onOpenGuidedForms: (String?) -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    viewModel: CreateReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isAdmin = uiState.isAdmin

    val formsList = FormCatalog.all

    // Multi-Selection State for Long Press
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTemplateIds = remember { mutableStateListOf<String>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val toggleSelection = { templateId: String ->
        if (selectedTemplateIds.contains(templateId)) {
            selectedTemplateIds.remove(templateId)
            if (selectedTemplateIds.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedTemplateIds.add(templateId)
        }
    }

    val startSelection = { templateId: String ->
        if (isAdmin) {
            isSelectionMode = true
            selectedTemplateIds.clear()
            selectedTemplateIds.add(templateId)
        } else {
            Toast.makeText(context, "Solo los usuarios administradores pueden gestionar formatos.", Toast.LENGTH_SHORT).show()
        }
    }

    val clearSelection = {
        isSelectionMode = false
        selectedTemplateIds.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (isSelectionMode) {
                            Text(
                                text = "${selectedTemplateIds.size} seleccionada(s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Formatos Guiados Oficiales",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Selecciona el formato oficial que deseas llenar paso a paso",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                clearSelection()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver / Cancelar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isSelectionMode && isAdmin) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Eliminar Seleccionadas",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            WorkProBottomNavBar(
                currentRoute = Screen.CREATE_REPORT,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .horizontalTabSwipeGesture(Screen.CREATE_REPORT, onTabSelected)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text(
                text = "Selecciona una plantilla",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            formsList.forEach { form ->
                val isSelected = selectedTemplateIds.contains(form.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 18.dp, prominent = true)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            } else Modifier
                        )
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(form.id)
                                } else {
                                    onOpenGuidedForms(form.id)
                                }
                            },
                            onLongClick = {
                                startSelection(form.id)
                            }
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggleSelection(form.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = form.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Código: ${form.code} • ${form.pages.size} página(s) • Oficial",
                                        style = TelemetryMonoStyle,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Organización: ${form.organization} · ${form.revision}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Toca para iniciar llenado asistido paso a paso →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar Formatos", fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Estás seguro de que deseas eliminar ${selectedTemplateIds.size} formato(s) seleccionada(s)? Esta acción requiere permisos de administrador.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Los formatos predeterminados están protegidos por el sistema", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = false
                        clearSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
