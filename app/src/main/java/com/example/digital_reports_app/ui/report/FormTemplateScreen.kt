package com.example.digital_reports_app.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.ui.navigation.Screen
import com.example.digital_reports_app.ui.navigation.WorkProBottomNavBar
import com.example.digital_reports_app.ui.navigation.horizontalTabSwipeGesture
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.CustomFormField
import com.example.digital_reports_app.util.DocumentTextAnalyzer
import com.example.digital_reports_app.util.FormFieldType
import com.example.digital_reports_app.util.NotificationHelper
import java.io.File

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
    onOpenGuidedForms: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    viewModel: CreateReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isAdmin = uiState.isAdmin

    // Observe templates directly from Room DB
    val templates = uiState.savedTemplates

    var selectedTemplate by remember { mutableStateOf<ReportTemplateItem?>(null) }
    var templateToEdit by remember { mutableStateOf<ReportTemplateItem?>(null) }
    var showCreateOptionsDialog by remember { mutableStateOf(false) }
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

    // Full-resolution Camera Capture Uri
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

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
        if (isAdmin) {
            isSelectionMode = true
            selectedTemplateIds.clear()
            selectedTemplateIds.add(templateId)
        } else {
            Toast.makeText(context, "Solo los usuarios administradores pueden editar o eliminar plantillas.", Toast.LENGTH_SHORT).show()
        }
    }

    val clearSelection = {
        isSelectionMode = false
        selectedTemplateIds.clear()
    }

    // 1. PDF File Upload Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeAndProcessPdfUri(context, uri) { title, cat, desc, fields ->
                newTemplateTitle = title
                newTemplateCategory = cat
                newTemplateDescription = desc
                newTemplateFields.clear()
                newTemplateFields.addAll(fields)
                showScanDocumentDialog = true
                Toast.makeText(context, "¡PDF analizado! Se extrajeron ${fields.size} campos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 2. Full-resolution TakePicture Launcher (FileProvider in cacheDir/scans)
    val fullResPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess && currentPhotoFile != null && currentPhotoFile!!.exists()) {
            val bitmap: Bitmap? = BitmapFactory.decodeFile(currentPhotoFile!!.absolutePath)
            if (bitmap != null) {
                viewModel.analyzeAndProcessScannedPhoto(bitmap) { title, cat, desc, fields ->
                    newTemplateTitle = title
                    newTemplateCategory = cat
                    newTemplateDescription = desc
                    newTemplateFields.clear()
                    newTemplateFields.addAll(fields)
                    showScanDocumentDialog = true
                    Toast.makeText(context, "¡Foto analizada! Se extrajeron ${fields.size} campos.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 3. Camera Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val scansDir = File(context.cacheDir, "scans")
                scansDir.mkdirs()
                val photoFile = File(scansDir, "scan_photo_${System.currentTimeMillis()}.jpg")
                val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                currentPhotoFile = photoFile
                currentPhotoUri = photoUri
                fullResPhotoLauncher.launch(photoUri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al iniciar cámara: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear formatos.", Toast.LENGTH_LONG).show()
        }
    }

    val triggerCameraOrScan = {
        if (!isAdmin) {
            Toast.makeText(context, "Solo los usuarios administradores pueden escanear nuevas plantillas.", Toast.LENGTH_SHORT).show()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    val scansDir = File(context.cacheDir, "scans")
                    scansDir.mkdirs()
                    val photoFile = File(scansDir, "scan_photo_${System.currentTimeMillis()}.jpg")
                    val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                    currentPhotoFile = photoFile
                    currentPhotoUri = photoUri
                    fullResPhotoLauncher.launch(photoUri)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al iniciar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            try {
                File(context.cacheDir, "scans").deleteRecursively()
            } catch (_: Exception) {}

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
                                text = "Editar o eliminar plantillas seleccionadas",
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
                                text = if (isAdmin) "Selecciona, crea o gestiona las plantillas de la empresa" else "Selecciona una plantilla para crear tu reporte",
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
                    if (isSelectionMode && selectedTemplate == null && isAdmin) {
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
            if (selectedTemplate == null) {
                WorkProBottomNavBar(
                    currentRoute = Screen.CREATE_REPORT,
                    onTabSelected = onTabSelected
                )
            }
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
            // STEP 1: CATALOG OF TEMPLATES
            if (selectedTemplate == null) {
                // Unified Single Main Button: Crear Plantilla
                if (isAdmin) {
                    Button(
                        onClick = { showCreateOptionsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear Plantilla", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Plantillas de la Empresa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (templates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 16.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAdmin)
                                "No hay plantillas registradas aún. Presiona el botón 'Crear Plantilla' para escanear o cargar los formatos de tu empresa."
                            else
                                "No hay plantillas registradas por el administrador aún.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
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
                                        if (isAdmin) {
                                            if (!isSelectionMode) {
                                                startSelection(template.id)
                                            } else {
                                                toggleSelection(template.id)
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Solo los usuarios administradores pueden editar o eliminar plantillas.",
                                                Toast.LENGTH_SHORT
                                            ).show()
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

                                    if (!isSelectionMode && isAdmin) {
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
                                        text = if (isAdmin) "Toca para llenar reporte • Mantén presionado para gestionar →" else "Toca para llenar reporte →",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
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

    // UNIFIED CREATE TEMPLATE OPTIONS DIALOG (Cargar PDF vs Escanear)
    if (showCreateOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showCreateOptionsDialog = false },
            title = { Text("Crear Nueva Plantilla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Selecciona el método de creación de la plantilla para tu empresa:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Cargar PDF
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 14.dp, prominent = true)
                            .clickable {
                                showCreateOptionsDialog = false
                                try {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                } catch (_: Exception) {
                                    pdfPickerLauncher.launch(arrayOf("*/*"))
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📄", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Cargar Documento PDF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Sube un archivo PDF membretado de la empresa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 2: Escanear
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 14.dp, prominent = true)
                            .clickable {
                                showCreateOptionsDialog = false
                                triggerCameraOrScan()
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📷", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Escanear Hoja Física", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Captura una fotografía del formato impreso", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showCreateOptionsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ANALYZING DOCUMENT LOADING DIALOG
    if (uiState.isAnalyzingDocument) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Analizando Documento...", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Ejecutando reconocimiento óptico OCR y extrayendo campos fidedignos...")
                }
            },
            confirmButton = {}
        )
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
                        text = "Documento procesado. Revisa y asigna el nombre, tipo y campos de la plantilla:",
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
                            viewModel.saveTemplateToDb(
                                title = newTemplateTitle,
                                category = newTemplateCategory,
                                description = newTemplateDescription,
                                fields = newTemplateFields
                            )
                            showScanDocumentDialog = false
                            Toast.makeText(context, "Plantilla '$newTemplateTitle' guardada como predeterminada", Toast.LENGTH_SHORT).show()
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
                            viewModel.saveTemplateToDb(
                                title = newTemplateTitle,
                                category = newTemplateCategory,
                                description = newTemplateDescription,
                                fields = newTemplateFields
                            )
                            showCreateTemplateDialog = false
                            Toast.makeText(context, "Plantilla '$newTemplateTitle' creada con éxito", Toast.LENGTH_SHORT).show()
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
                        if (editTitle.isNotBlank()) {
                            viewModel.saveTemplateToDb(
                                title = editTitle,
                                category = editCategory,
                                description = editDescription,
                                fields = editFields,
                                customId = tpl.id
                            )
                            Toast.makeText(context, "Plantilla actualizada en la base de datos", Toast.LENGTH_SHORT).show()
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
                        viewModel.deleteTemplatesByIdsFromDb(selectedTemplateIds.toList())
                        Toast.makeText(context, "${selectedTemplateIds.size} plantilla(s) eliminada(s) de la base de datos", Toast.LENGTH_SHORT).show()
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
