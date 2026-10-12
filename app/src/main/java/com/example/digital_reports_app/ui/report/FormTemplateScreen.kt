package com.example.digital_reports_app.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.CustomFormField
import com.example.digital_reports_app.util.DocumentTextAnalyzer
import com.example.digital_reports_app.util.FormFieldType
import com.example.digital_reports_app.util.NotificationHelper

data class ReportTemplateItem(
    val id: String,
    var title: String,
    var category: String,
    var description: String,
    val icon: ImageVector = Icons.Outlined.Edit,
    val fields: MutableList<CustomFormField> = mutableStateListOf()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FormTemplateScreen(
    onBack: () -> Unit,
    viewModel: CreateReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Dynamic Templates Catalog
    val templates = remember {
        mutableStateListOf(
            ReportTemplateItem(
                id = "tpl_1",
                title = "Inspección Diaria de Planta",
                category = "Inspección",
                description = "Verificación general de infraestructura, equipos y condiciones ambientales.",
                icon = Icons.Outlined.Edit,
                fields = mutableStateListOf(*DocumentTextAnalyzer.createDefaultFields().toTypedArray())
            ),
            ReportTemplateItem(
                id = "tpl_2",
                title = "Mantenimiento Preventivo de Maquinaria",
                category = "Mantenimiento",
                description = "Revisión de lubricación, presión y componentes mecánicos de planta.",
                icon = Icons.Outlined.Info,
                fields = mutableStateListOf(
                    CustomFormField("m_1", "Código de Maquinaria", FormFieldType.TEXT, "MQ-104"),
                    CustomFormField("m_2", "Nivel de Aceite", FormFieldType.DROPDOWN, "Normal", listOf("Normal", "Bajo", "Crítico")),
                    CustomFormField("m_3", "Presión (PSI)", FormFieldType.NUMBER, "120"),
                    CustomFormField("m_4", "Observaciones de Mantenimiento", FormFieldType.TEXT, "Piezas en buen estado.")
                )
            ),
            ReportTemplateItem(
                id = "tpl_3",
                title = "Control de Seguridad e Higiene",
                category = "Incidencia",
                description = "Auditoría de equipos EPP, ventilación y protocolos de riesgo laboral.",
                icon = Icons.Outlined.Check,
                fields = mutableStateListOf(
                    CustomFormField("s_1", "Zona de Trabajo", FormFieldType.TEXT, "Socavón / Nivel 2"),
                    CustomFormField("s_2", "Uso de EPP", FormFieldType.DROPDOWN, "Conforme", listOf("Conforme", "Incompleto", "No Conforme")),
                    CustomFormField("s_3", "Medición de Gas (PPM)", FormFieldType.NUMBER, "12"),
                    CustomFormField("s_4", "Detalles de Seguridad", FormFieldType.TEXT, "Sin incidencias graves.")
                )
            ),
            ReportTemplateItem(
                id = "tpl_4",
                title = "Auditoría de Almacén y Embalaje",
                category = "General",
                description = "Revisión de inventarios, cajas y estado de carga de materiales.",
                icon = Icons.Outlined.DateRange,
                fields = mutableStateListOf(
                    CustomFormField("a_1", "Sector de Almacén", FormFieldType.TEXT, "Almacén B"),
                    CustomFormField("a_2", "Estado de Empaque", FormFieldType.DROPDOWN, "Excelente", listOf("Excelente", "Dañado", "Incompleto")),
                    CustomFormField("a_3", "Cantidad de Cajas", FormFieldType.NUMBER, "450"),
                    CustomFormField("a_4", "Notas de Carga", FormFieldType.TEXT, "Carga lista para despacho.")
                )
            )
        )
    }

    var selectedTemplate by remember { mutableStateOf<ReportTemplateItem?>(null) }
    var templateToEdit by remember { mutableStateOf<ReportTemplateItem?>(null) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var showScanDocumentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Multi-Selection State for Long Press
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTemplateIds = remember { mutableStateListOf<String>() }

    // Dialog state for New Template / Scan / PDF Upload
    var newTemplateTitle by remember { mutableStateOf("") }
    var newTemplateCategory by remember { mutableStateOf("Inspección") }
    var newTemplateDescription by remember { mutableStateOf("") }
    val newTemplateFields = remember { mutableStateListOf<CustomFormField>() }

    // Dynamic Form Active Values (Step 2)
    val activeFormFields = remember { mutableStateListOf<CustomFormField>() }

    // Selection Helper functions
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
        isSelectionMode = true
        selectedTemplateIds.clear()
        selectedTemplateIds.add(templateId)
    }

    val clearSelection = {
        isSelectionMode = false
        selectedTemplateIds.clear()
    }

    val moveSelectedUp = {
        val indices = templates.indices.filter { selectedTemplateIds.contains(templates[it].id) }
        for (i in indices) {
            if (i > 0 && !selectedTemplateIds.contains(templates[i - 1].id)) {
                val temp = templates[i]
                templates[i] = templates[i - 1]
                templates[i - 1] = temp
            }
        }
    }

    val moveSelectedDown = {
        val indices = templates.indices.filter { selectedTemplateIds.contains(templates[it].id) }.reversed()
        for (i in indices) {
            if (i < templates.size - 1 && !selectedTemplateIds.contains(templates[i + 1].id)) {
                val temp = templates[i]
                templates[i] = templates[i + 1]
                templates[i + 1] = temp
            }
        }
    }

    // 1. PDF File Upload Launcher: Opens document picker to upload PDF format
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Documento_Formato.pdf"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}

            val pdfTitle = fileName.removeSuffix(".pdf").removeSuffix(".PDF")
                .replace("_", " ")
                .replace("-", " ")
                .trim()
                .ifBlank { "Formato PDF Escaneado" }

            newTemplateTitle = pdfTitle
            newTemplateCategory = "Inspección"
            newTemplateDescription = "Plantilla extraída de archivo PDF cargado ($fileName)."
            newTemplateFields.clear()
            newTemplateFields.addAll(DocumentTextAnalyzer.createDefaultFields())

            showScanDocumentDialog = true
            Toast.makeText(context, "Archivo PDF cargado. Configura el nombre y campos.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Carga de PDF cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Camera TakePicturePreview Launcher: Captures photo & Analyzes photo to create template
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val nextIndex = templates.size + 1
            val result = DocumentTextAnalyzer.analyzeScannedBitmap(bitmap, nextIndex)

            newTemplateTitle = result.templateTitle
            newTemplateCategory = result.category
            newTemplateDescription = result.description
            newTemplateFields.clear()
            newTemplateFields.addAll(result.extractedFields)

            showScanDocumentDialog = true
            Toast.makeText(context, "Foto analizada. Revisa los campos de la plantilla.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Camera Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear el formato impreso.", Toast.LENGTH_LONG).show()
        }
    }

    val triggerCameraOrScan = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo iniciar la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
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
                        if (isSelectionMode) {
                            Text(
                                text = "${selectedTemplateIds.size} seleccionada(s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mover, editar o eliminar plantillas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (selectedTemplate == null) {
                            Text(
                                text = "Gestión de Plantillas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Selecciona, escanea, carga PDF o mantén presionado para gestionar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Llenar Formulario de Reporte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paso 2: Rellena los datos dinámicos del reporte",
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
                            } else if (selectedTemplate != null) {
                                selectedTemplate = null
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
                    if (isSelectionMode && selectedTemplate == null) {
                        // Move Up
                        IconButton(onClick = { moveSelectedUp() }) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = "Mover Arriba",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Move Down
                        IconButton(onClick = { moveSelectedDown() }) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Mover Abajo",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Edit if exactly 1 item selected
                        if (selectedTemplateIds.size == 1) {
                            IconButton(
                                onClick = {
                                    val target = templates.find { it.id == selectedTemplateIds.first() }
                                    if (target != null) {
                                        templateToEdit = target
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar Plantilla",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Delete selected items
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
            // STEP 1: CATALOG OF TEMPLATES
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Scan Document via Native Camera Button
                        OutlinedButton(
                            onClick = { triggerCameraOrScan() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Escanear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Upload PDF Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                } catch (_: Exception) {
                                    pdfPickerLauncher.launch(arrayOf("*/*"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cargar PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Create New Template Button
                        Button(
                            onClick = {
                                newTemplateTitle = ""
                                newTemplateCategory = "Inspección"
                                newTemplateDescription = ""
                                newTemplateFields.clear()
                                newTemplateFields.addAll(DocumentTextAnalyzer.createDefaultFields())
                                showCreateTemplateDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
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
                    val isSelected = selectedTemplateIds.contains(template.id)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                cornerRadius = 18.dp,
                                prominent = true
                            )
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
                                        toggleSelection(template.id)
                                    } else {
                                        selectedTemplate = template
                                        activeFormFields.clear()
                                        activeFormFields.addAll(template.fields.map { it.copy() })
                                        viewModel.onTitleChanged(template.title)
                                        viewModel.onCategoryChanged(template.category)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        startSelection(template.id)
                                    } else {
                                        toggleSelection(template.id)
                                    }
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
                                            onCheckedChange = { toggleSelection(template.id) }
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
                                            text = "Tipo: ${template.category} • ${template.fields.size} campos",
                                            style = TelemetryMonoStyle,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (!isSelectionMode) {
                                    TextButton(
                                        onClick = { templateToEdit = template }
                                    ) {
                                        Text("Editar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!isSelectionMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Toca para llenar reporte • Mantén presionado para seleccionar →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else {
                // STEP 2: FILL DYNAMIC FORM FIELDS BASED ON SELECTED TEMPLATE
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

                // Dynamic Form Container
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

                        // Render each dynamic field from the selected template
                        activeFormFields.forEachIndexed { index, field ->
                            DynamicFormFieldControl(
                                field = field,
                                onValueChanged = { newValue ->
                                    activeFormFields[index] = field.copy(value = newValue)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
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
                            // Formats all filled dynamic fields into description
                            val formattedDescription = activeFormFields.joinToString("\n") {
                                "${it.label}: ${it.value.ifBlank { "N/A" }}"
                            }
                            viewModel.onDescriptionChanged(formattedDescription)
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

    // SCAN PHYSICAL DOCUMENT / UPLOAD PDF CONFIG DIALOG
    if (showScanDocumentDialog) {
        AlertDialog(
            onDismissRequest = { showScanDocumentDialog = false },
            title = { Text("Configurar Plantilla desde PDF / Foto Escaneada", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Documento/PDF procesado. Revisa y asigna el nombre, tipo y campos de la plantilla:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        label = { Text("Tipo / Categoría") },
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

                    Text(
                        text = "Campos Extraídos (${newTemplateFields.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    newTemplateFields.forEachIndexed { idx, fld ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fld.label,
                                onValueChange = { newLabel ->
                                    newTemplateFields[idx] = fld.copy(label = newLabel)
                                },
                                label = { Text("Campo ${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { newTemplateFields.removeAt(idx) }) {
                                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Eliminar Campo", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            newTemplateFields.add(CustomFormField("field_${System.currentTimeMillis()}", "Nuevo Campo"))
                        }
                    ) {
                        Text("+ Agregar Campo Adicional")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTemplateTitle.isNotBlank()) {
                            val scannedTpl = ReportTemplateItem(
                                id = "scanned_pdf_${System.currentTimeMillis()}",
                                title = newTemplateTitle.trim(),
                                category = newTemplateCategory.trim().ifBlank { "Inspección" },
                                description = newTemplateDescription.trim().ifBlank { "Plantilla generada a partir de archivo PDF / documento físico." },
                                icon = Icons.Outlined.Edit,
                                fields = mutableStateListOf(*newTemplateFields.map { it.copy() }.toTypedArray())
                            )
                            templates.add(0, scannedTpl)
                            showScanDocumentDialog = false
                            Toast.makeText(context, "Plantilla '${scannedTpl.title}' configurada con éxito", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar en Plantillas", fontWeight = FontWeight.Bold)
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        label = { Text("Tipo / Categoría") },
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

                    Text(
                        text = "Campos Personalizados:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    newTemplateFields.forEachIndexed { idx, fld ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fld.label,
                                onValueChange = { newLabel ->
                                    newTemplateFields[idx] = fld.copy(label = newLabel)
                                },
                                label = { Text("Campo ${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { newTemplateFields.removeAt(idx) }) {
                                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            newTemplateFields.add(CustomFormField("field_${System.currentTimeMillis()}", "Nuevo Campo"))
                        }
                    ) {
                        Text("+ Agregar Campo")
                    }
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
                                icon = Icons.Outlined.Edit,
                                fields = mutableStateListOf(*newTemplateFields.map { it.copy() }.toTypedArray())
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
        val editFields = remember { mutableStateListOf(*tpl.fields.map { it.copy() }.toTypedArray()) }

        AlertDialog(
            onDismissRequest = { templateToEdit = null },
            title = { Text("Editar Plantilla", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        label = { Text("Tipo / Categoría") },
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

                    Text(
                        text = "Editar Campos de esta Plantilla:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    editFields.forEachIndexed { idx, fld ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fld.label,
                                onValueChange = { newLabel ->
                                    editFields[idx] = fld.copy(label = newLabel)
                                },
                                label = { Text("Campo ${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { editFields.removeAt(idx) }) {
                                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            editFields.add(CustomFormField("ef_${System.currentTimeMillis()}", "Nuevo Campo"))
                        }
                    ) {
                        Text("+ Agregar Campo")
                    }
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
                                description = editDescription.trim(),
                                fields = mutableStateListOf(*editFields.map { it.copy() }.toTypedArray())
                            )
                            templates[index] = updated
                            Toast.makeText(context, "Plantilla actualizada con éxito", Toast.LENGTH_SHORT).show()
                        }
                        templateToEdit = null
                        clearSelection()
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

    // DELETE CONFIRMATION DIALOG FOR MULTI-SELECTION
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar Plantillas", fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Estás seguro de que deseas eliminar ${selectedTemplateIds.size} plantilla(s) seleccionada(s)? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        templates.removeAll { selectedTemplateIds.contains(it.id) }
                        Toast.makeText(context, "${selectedTemplateIds.size} plantilla(s) eliminada(s)", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = false
                        clearSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
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

@Composable
private fun DynamicFormFieldControl(
    field: CustomFormField,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    when (field.type) {
        FormFieldType.DROPDOWN -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = field.value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("${field.label} *") },
                    trailingIcon = { Text("▼") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    field.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChanged(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        FormFieldType.NUMBER -> {
            OutlinedTextField(
                value = field.value,
                onValueChange = onValueChanged,
                label = { Text("${field.label} *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
        FormFieldType.CHECKBOX -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = field.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Checkbox(
                    checked = field.value == "Sí" || field.value == "Conforme",
                    onCheckedChange = { isChecked ->
                        onValueChanged(if (isChecked) "Conforme" else "No Conforme")
                    }
                )
            }
        }
        else -> {
            OutlinedTextField(
                value = field.value,
                onValueChange = onValueChanged,
                label = { Text("${field.label} *") },
                minLines = if (field.label.contains("Observaciones", ignoreCase = true)) 3 else 1,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
