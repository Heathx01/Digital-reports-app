package com.example.digital_reports_app.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.domain.model.FormItem
import com.example.digital_reports_app.domain.model.FormKeys
import com.example.digital_reports_app.domain.model.FormSection
import com.example.digital_reports_app.domain.model.RowKind
import com.example.digital_reports_app.domain.model.jsonToStrokes
import com.example.digital_reports_app.domain.model.strokesToJson
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedFormScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: GuidedFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val form = state.form
    val steps = state.steps
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.savedReportId) {
        state.savedReportId?.let(onSaved)
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = form?.name ?: "Llenado de Formato",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (form != null && steps.isNotEmpty()) {
                val isLast = state.stepIndex == steps.lastIndex
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (state.stepIndex == 0) {
                                    onBack()
                                } else {
                                    viewModel.previous()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (state.stepIndex == 0) "Cambiar formato" else "Atrás") }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isLast) viewModel.save() else viewModel.next()
                            },
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isLast) "Generar reporte" else "Siguiente")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (form == null) {
            FormPicker(
                modifier = Modifier.padding(padding),
                onSelect = { viewModel.selectForm(it) },
                forms = state.forms.map { Triple(it.id, it.name, "${it.code} · ${it.pages.size} página(s) · Predefinido") }
            )
        } else if (steps.isNotEmpty()) {
            val step = steps[state.stepIndex.coerceIn(0, steps.lastIndex)]
            val fraction = (state.stepIndex + 1f) / steps.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                // Real-time Field Completion Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso de Llenado",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Paso ${state.stepIndex + 1} de ${steps.size} (${(fraction * 100).toInt()}%)",
                            style = TelemetryMonoStyle.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    when (step) {
                        is GuidedStep.Header -> {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    step.page.headerFields.forEach { f ->
                                        val key = FormKeys.header(step.page.id, f.id)
                                        OutlinedTextField(
                                            value = state.answers[key].orEmpty(),
                                            onValueChange = { viewModel.setAnswer(key, it) },
                                            label = { Text("${f.label.lowercase().replaceFirstChar { it.uppercase() }} *") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        is GuidedStep.Checklist -> step.section.items.forEach { item ->
                            ChecklistItem(
                                section = step.section,
                                item = item,
                                answers = state.answers,
                                onToggle = { value ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.toggleAnswer(FormKeys.answer(item.id), value)
                                },
                                onValue = { viewModel.setAnswer(FormKeys.value(item.id), it) },
                                onObs = { viewModel.setAnswer(FormKeys.obs(item.id), it) }
                            )
                        }

                        is GuidedStep.Closing -> {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.answers[FormKeys.comments(step.page.id)].orEmpty(),
                                        onValueChange = { viewModel.setAnswer(FormKeys.comments(step.page.id), it) },
                                        label = { Text(step.page.commentsLabel.trimEnd(':')) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            step.page.signatureLabels.forEachIndexed { i, label ->
                                val nameKey = FormKeys.sigName(step.page.id, i)
                                val strokesKey = FormKeys.sigStrokes(step.page.id, i)

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                        OutlinedTextField(
                                            value = state.answers[nameKey].orEmpty(),
                                            onValueChange = { viewModel.setAnswer(nameKey, it) },
                                            label = { Text("Nombre de quien firma *") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        SignaturePad(
                                            strokesJson = state.answers[strokesKey].orEmpty(),
                                            onChange = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setAnswer(strokesKey, it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    state.errorMessage?.let { errorText ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ $errorText",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Extra Spacer at bottom so keyboard never obstructs lower fields
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}

@Composable
private fun FormPicker(
    modifier: Modifier,
    forms: List<Triple<String, String, String>>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Elige el formato que quieres llenar. Ingresarás los datos paso a paso y la app generará el reporte completo en PDF.",
            style = MaterialTheme.typography.bodyMedium
        )
        forms.forEach { (id, name, subtitle) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(id) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    section: FormSection,
    item: FormItem,
    answers: Map<String, String>,
    onToggle: (String) -> Unit,
    onValue: (String) -> Unit,
    onObs: (String) -> Unit
) {
    val current = answers[FormKeys.answer(item.id)].orEmpty()

    // Determine target compliant/positive answer
    val positiveTarget = item.positiveAnswer ?: section.kind.positive
    val isYESNO = section.kind == RowKind.YES_NO

    val opt1 = if (isYESNO) "SI" else "CUMPLE"
    val opt2 = if (isYESNO) "NO" else "NO CUMPLE"

    val isOpt1Favorable = (opt1 == positiveTarget)
    val isOpt2Favorable = (opt2 == positiveTarget)

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Numeric Measurement Input with Auto Unit Suffix
            if (section.kind == RowKind.MEASURE && item.unit != null) {
                val unit = item.unit
                val currentStored = answers[FormKeys.value(item.id)].orEmpty()
                val currentDigits = currentStored
                    .removeSuffix(" $unit")
                    .removeSuffix(unit)
                    .trim()

                OutlinedTextField(
                    value = currentDigits,
                    onValueChange = { input ->
                        val cleanDigits = input.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        if (cleanDigits.isBlank()) {
                            onValue("")
                        } else {
                            val formattedVal = if (unit == "%") "$cleanDigits%" else "$cleanDigits $unit"
                            onValue(formattedVal)
                        }
                    },
                    label = { Text("Medición ($unit) *") },
                    suffix = {
                        Text(
                            text = unit,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            } else if (section.kind == RowKind.MEASURE && item.unit == null) {
                val valueStored = answers[FormKeys.value(item.id)].orEmpty()
                if (valueStored.isNotBlank()) {
                    OutlinedTextField(
                        value = valueStored,
                        onValueChange = onValue,
                        label = { Text("Medición / Valor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // High-Contrast Dual Option Selector (SI / NO, CUMPLE / NO CUMPLE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OPTION 1 (SI / CUMPLE)
                val isOpt1Selected = current == opt1
                val opt1Bg = if (isOpt1Selected) {
                    if (isOpt1Favorable) Color(0xFF2E7D32) else Color(0xFFC62828)
                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                val opt1Border = if (isOpt1Selected) {
                    if (isOpt1Favorable) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                } else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                val opt1Icon = if (isOpt1Favorable) Icons.Outlined.Check else Icons.Outlined.Close

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable { onToggle(opt1) },
                    shape = RoundedCornerShape(12.dp),
                    color = opt1Bg,
                    border = BorderStroke(if (isOpt1Selected) 2.dp else 1.dp, opt1Border)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = opt1Icon,
                            contentDescription = null,
                            tint = if (isOpt1Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = opt1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isOpt1Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // OPTION 2 (NO / NO CUMPLE)
                val isOpt2Selected = current == opt2
                val opt2Bg = if (isOpt2Selected) {
                    if (isOpt2Favorable) Color(0xFF2E7D32) else Color(0xFFC62828)
                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                val opt2Border = if (isOpt2Selected) {
                    if (isOpt2Favorable) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                } else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                val opt2Icon = if (isOpt2Favorable) Icons.Outlined.Check else Icons.Outlined.Close

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable { onToggle(opt2) },
                    shape = RoundedCornerShape(12.dp),
                    color = opt2Bg,
                    border = BorderStroke(if (isOpt2Selected) 2.dp else 1.dp, opt2Border)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = opt2Icon,
                            contentDescription = null,
                            tint = if (isOpt2Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = opt2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isOpt2Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = answers[FormKeys.obs(item.id)].orEmpty(),
                onValueChange = onObs,
                label = { Text("Observaciones (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun SignaturePad(strokesJson: String, onChange: (String) -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val strokes = remember { mutableStateListOf<List<Pair<Float, Float>>>() }
    var current by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    LaunchedEffect(strokesJson) {
        if (strokesJson.isBlank()) {
            strokes.clear()
            current = emptyList()
        } else {
            strokes.clear()
            strokes.addAll(jsonToStrokes(strokesJson))
        }
    }

    fun norm(o: Offset): Pair<Float, Float> {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        return (o.x / w).coerceIn(0f, 1f) to (o.y / h).coerceIn(0f, 1f)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o -> current = listOf(norm(o)) },
                        onDrag = { change, _ ->
                            change.consume()
                            current = current + norm(change.position)
                        },
                        onDragEnd = {
                            if (current.size > 1) {
                                strokes.add(current)
                                onChange(strokesToJson(strokes.toList()))
                            }
                            current = emptyList()
                        },
                        onDragCancel = { current = emptyList() }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                (strokes.toList() + listOf(current)).forEach { stroke ->
                    if (stroke.size > 1) {
                        val path = Path()
                        stroke.forEachIndexed { i, (nx, ny) ->
                            val x = nx * this.size.width
                            val y = ny * this.size.height
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color.Black, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    }
                }
            }
            if (strokes.isEmpty() && current.isEmpty()) {
                Text(
                    "Firma aquí con el dedo",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
        TextButton(onClick = {
            strokes.clear()
            current = emptyList()
            onChange("")
        }) { Text("Borrar firma") }
    }
}
