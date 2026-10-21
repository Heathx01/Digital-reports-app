package com.example.digital_reports_app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.R
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass

data class RoleOption(
    val id: String,
    val title: String,
    val badgeText: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("SUPERVISOR") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val roleOptions = remember {
        listOf(
            RoleOption(
                id = "SUPERVISOR",
                title = "Supervisor",
                badgeText = "SUPERVISOR",
                description = "Captura de expedientes, llenado de formatos guiados y escaneo.",
                icon = Icons.Outlined.Person
            ),
            RoleOption(
                id = "JEFE",
                title = "Jefe de Área",
                badgeText = "JEFE",
                description = "Supervisión de área, revisión de bitácora y aprobación de expedientes.",
                icon = Icons.Outlined.Info
            ),
            RoleOption(
                id = "ADMIN",
                title = "Administrador",
                badgeText = "ADMIN",
                description = "Control total del sistema, gestión de plantillas y usuarios.",
                icon = Icons.Outlined.Settings
            )
        )
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Gestión de Usuarios", fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    newName = ""
                    newCode = ""
                    newPassword = ""
                    newRole = "SUPERVISOR"
                    isPasswordVisible = false
                    viewModel.toggleCreateUserDialog(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Nuevo Usuario", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Usuarios Registrados (${uiState.users.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.users) { user ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(cornerRadius = 16.dp, prominent = true)
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
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${stringResource(R.string.label_user_code)}: ${user.userCode}",
                                            style = TelemetryMonoStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val (badgeBg, badgeText, label) = when (user.role.uppercase()) {
                                    "SUPERADMIN", "ADMIN" -> Triple(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary,
                                        "ADMIN"
                                    )
                                    "JEFE" -> Triple(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        MaterialTheme.colorScheme.tertiary,
                                        "JEFE DE ÁREA"
                                    )
                                    "SUPERVISOR" -> Triple(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.secondary,
                                        "SUPERVISOR"
                                    )
                                    else -> Triple(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        user.role
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(badgeBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = TelemetryMonoStyle.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // REGISTRAR NUEVO USUARIO DIALOG (Professional & Detailed Role Access UI)
    if (uiState.showCreateUserDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleCreateUserDialog(false) },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Registrar Nuevo Usuario",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Datos Personales
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre Completo *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("Código de Trabajador (min. 6 dígitos) *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Contraseña Inicial *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.Check else Icons.Outlined.Lock,
                                    contentDescription = if (isPasswordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Rol y Niveles de Acceso
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Rol y Nivel de Acceso *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Selecciona las responsabilidades del usuario:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        roleOptions.forEach { option ->
                            val isSelected = newRole == option.id

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { newRole = option.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { newRole = option.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = option.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = option.badgeText,
                                                    style = TelemetryMonoStyle.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = option.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createUser(newName, newCode, newPassword, newRole)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Registrar Usuario", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.toggleCreateUserDialog(false) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
