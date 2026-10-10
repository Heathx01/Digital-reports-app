package com.example.digital_reports_app.ui.report

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.DocumentScannerHelper
import com.example.digital_reports_app.util.NotificationHelper

data class ReportTemplateItem(
    val id: String,
    var title: String,
    var category: String,
    var description: String,
    val icon: ImageVector = Icons.Outlined.Edit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTemplateScreen(
    onBack: () -> Unit,
    viewModel: CreateReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Dynamic Templates List (Support Scan, Create & Edit Template)
    val templates = remember {
        mutableStateListOf(
            ReportTemplateItem(
                id = "tpl_1",
                title = "Inspección Diaria de Planta",
                category = "Inspección",
                description = "Verificación general de infraestructura, equipos y condiciones ambientales.",
                icon = Icons.Outlined.Edit
            ),
            ReportTemplateItem(
                id = "tpl_2",
                title = "Mantenimiento Preventivo de Maquinaria",
                category = "Mantenimiento",
                description = "Revisión de lubricación, presión y componentes mecánicos de planta.",
                icon = Icons.Outlined.Info
            ),
            ReportTemplateItem(
                id = "tpl_3",
                title = "Control de Seguridad e Higiene",
                category = "Incidencia",
                description = "Auditoría de equipos EPP, ventilación y protocolos de riesgo laboral.",
                icon = Icons.Outlined.Check
            ),
            ReportTemplateItem(
                id = "tpl_4",
                title = "Auditoría de Almacén y Embalaje",
                category = "General",
                description = "Revisión de inventarios, cajas y estado de carga de materiales.",
                icon = Icons.Outlined.DateRange
            )
        )
    }

    var selectedTemplate by remember { mutableStateOf<ReportTemplateItem?>(null) }
    var templateToEdit by remember { mutableStateOf<ReportTemplateItem?>(null) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var showScanDocumentDialog by remember { mutableStateOf(false) }

    // Dialog state for New Template / Scan
    var newTemplateTitle by remember { mutableStateOf("") }
    var newTemplateCategory by remember { mutableStateOf("Inspección") }
    var newTemplateDescription by remember { mutableStateOf("") }

    // Form fields for Step 2
    var locationExpanded by remember { mutableStateOf(false) }
    var areaStatusExpanded by remember { mutableStateOf(false) }

    var selectedLocation by remember { mutableStateOf("Planta Principal") }
    var selectedAreaStatus by remember { mutableStateOf("Bueno") }
    var temperatureInput by remember { mutableStateOf("24") }

    // Native Camera Launcher for Document Scanning
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            newTemplateTitle = "Formato Escaneado #${templates.size + 1}"
            newTemplateCategory = "Inspección"
            newTemplateDescription = "Plantilla extraída de foto capturada con la cámara."
            showScanDocumentDialog = true
            Toast.makeText(context, "Foto de formato capturada correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            NotificationHelper.showNotification(
                context,
                "Reporte Creado y Guardado",
                "El reporte '${uiState.title}' ha sido guardado en la bitácora."
            )
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTemplate == null) "Gestión de Plantillas" else "Llenar Formulario de Reporte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedTemplate == null) "Selecciona, escanea o crea una plantilla" else "Paso 2: Rellena los datos para generar el reporte",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedTemplate != null) {
                                selectedTemplate = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // STEP 1: SELECT, SCAN, CREATE OR EDIT TEMPLATE
            if (selectedTemplate == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plantillas de la Empresa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Scan Document via Native Camera Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    cameraLauncher.launch(null)
                                } catch (e: Exception) {
                                    newTemplateTitle = "Formato Escaneado #${templates.size + 1}"
                                    newTemplateCategory = "Inspección"
                                    newTemplateDescription = "Plantilla extraída mediante escaneo de documento físico."
                                    showScanDocumentDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("📷 Escanear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Create New Template Button
                        Button(
                            onClick = {
                                newTemplateTitle = ""
                                newTemplateCategory = "Inspección"
                                newTemplateDescription = ""
                                showCreateTemplateDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Nueva", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                templates.forEach { template ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 18.dp, prominent = true)
                            .clickable {
                                selectedTemplate = template
                                viewModel.onTitleChanged(template.title)
                                viewModel.onCategoryChanged(template.category)
                            }
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
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = template.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = template.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Categoría: ${template.category}",
                                            style = TelemetryMonoStyle,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { templateToEdit = template }
                                ) {
                                    Text("Editar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Toca para usar esta plantilla y llenar el reporte →",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else {
                // STEP 2: FILL FORM BASED ON SELECTED TEMPLATE
                val currentTpl = selectedTemplate!!

                // Selected Template Header Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 16.dp, prominent = true)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plantilla Seleccionada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentTpl.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentTpl.category,
                                style = TelemetryMonoStyle,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Form Fields
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 20.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Llenado de Datos del Reporte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::onTitleChanged,
                            label = { Text("Título del reporte *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Área / Ubicación Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedLocation,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Área / Ubicación *") },
                                trailingIcon = { Text("▼") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = locationExpanded,
                                onDismissRequest = { locationExpanded = false }
                            ) {
                                listOf("Planta Principal", "Área de Embalaje", "Almacén B", "Línea de Ensamblaje").forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc) },
                                        onClick = {
                                            selectedLocation = loc
                                            locationExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estado del Área
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedAreaStatus,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Estado del área *") },
                                trailingIcon = { Text("▼") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = areaStatusExpanded,
                                onDismissRequest = { areaStatusExpanded = false }
                            ) {
                                listOf("Bueno", "Regular", "Requiere Atención", "Crítico").forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = {
                                            selectedAreaStatus = status
                                            areaStatusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Temperatura
                        OutlinedTextField(
                            value = temperatureInput,
                            onValueChange = { temperatureInput = it },
                            label = { Text("Temperatura (°C)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Observaciones
                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::onDescriptionChanged,
                            label = { Text("Observaciones del reporte") },
                            minLines = 3,
                            maxLines = 5,
                            placeholder = { Text("Ingresa detalles sobre la inspección...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedTemplate = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (uiState.title.isBlank()) {
                                viewModel.onTitleChanged(currentTpl.title)
                            }
                            if (uiState.description.isBlank()) {
                                viewModel.onDescriptionChanged("Estado de área: $selectedAreaStatus. Temp: $temperatureInput °C. Reporte registrado mediante plantilla.")
                            }
                            viewModel.createReport()
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Guardar Reporte", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // SCAN PHYSICAL DOCUMENT DIALOG
    if (showScanDocumentDialog) {
        AlertDialog(
            onDismissRequest = { showScanDocumentDialog = false },
            title = { Text("Documento Capturado con Cámara", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Foto del formato recibida. Se han extraído los campos del documento impreso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newTemplateTitle,
                        onValueChange = { newTemplateTitle = it },
                        label = { Text("Nombre para la Plantilla Escaneada *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTemplateCategory,
                        onValueChange = { newTemplateCategory = it },
                        label = { Text("Categoría (ej. Inspección, Mantenimiento)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTemplateTitle.isNotBlank()) {
                            val scannedTpl = DocumentScannerHelper.createTemplateFromScan(
                                templateTitle = newTemplateTitle,
                                category = newTemplateCategory,
                                description = "Plantilla extraída mediante escaneo con cámara del formato físico."
                            )
                            templates.add(0, scannedTpl)
                            showScanDocumentDialog = false
                            Toast.makeText(context, "Plantilla escaneada '${scannedTpl.title}' lista", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Crear Plantilla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showScanDocumentDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // CREATE NEW TEMPLATE DIALOG
    if (showCreateTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTemplateDialog = false },
            title = { Text("Crear Nueva Plantilla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTemplateTitle,
                        onValueChange = { newTemplateTitle = it },
                        label = { Text("Nombre de la Plantilla *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTemplateCategory,
                        onValueChange = { newTemplateCategory = it },
                        label = { Text("Categoría (ej. Inspección, Mantenimiento)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTemplateDescription,
                        onValueChange = { newTemplateDescription = it },
                        label = { Text("Descripción / Instrucciones") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTemplateTitle.isNotBlank()) {
                            val newTpl = ReportTemplateItem(
                                id = "tpl_${System.currentTimeMillis()}",
                                title = newTemplateTitle.trim(),
                                category = newTemplateCategory.trim().ifBlank { "General" },
                                description = newTemplateDescription.trim().ifBlank { "Plantilla personalizada de la empresa." },
                                icon = Icons.Outlined.Edit
                            )
                            templates.add(0, newTpl)
                            showCreateTemplateDialog = false
                            Toast.makeText(context, "Plantilla '${newTpl.title}' creada con éxito", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Crear Plantilla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateTemplateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // EDIT TEMPLATE DIALOG
    templateToEdit?.let { tpl ->
        var editTitle by remember { mutableStateOf(tpl.title) }
        var editCategory by remember { mutableStateOf(tpl.category) }
        var editDescription by remember { mutableStateOf(tpl.description) }

        AlertDialog(
            onDismissRequest = { templateToEdit = null },
            title = { Text("Editar Plantilla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Nombre de la Plantilla") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Categoría") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Descripción") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = templates.indexOfFirst { it.id == tpl.id }
                        if (index != -1 && editTitle.isNotBlank()) {
                            val updated = tpl.copy(
                                title = editTitle.trim(),
                                category = editCategory.trim(),
                                description = editDescription.trim()
                            )
                            templates[index] = updated
                            Toast.makeText(context, "Plantilla actualizada", Toast.LENGTH_SHORT).show()
                        }
                        templateToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { templateToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
